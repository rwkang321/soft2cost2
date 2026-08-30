/** 2026.08.13 AuthService.java by rwkang.
 * 1. AuthService : LOGIN -> 권한 계산 -> Redis 저장 -> accessToken 발급 -> Nexacro 응답.
 *
 */
/** 2026.08.17 Updated by cdx.
 * 변경 이유:
 * 자동 해제를 먼저 처리하고, 해시 비교는 존재 여부·상태와 무관하게 한 번 수행한다.
 * 실패 상태는 REQUIRES_NEW로 보존한다.
 * 잠긴 계정의 존재 여부는 올바른 비밀번호를 확인한 사용자에게만 E306으로 노출한다.
 * 변경 필수 상태를 세션과 응답에 포함한다.
 * 비밀번호 변경 후 모든 Redis 세션을 폐기하는 이벤트를 커밋 이후 처리한다.
 *
 * 보안 영향:
 * 수정 목표는 계정 열거와 timing 차이를 줄이고 실패 잠금을 보존하는 것이다. 성공 로그인 확인만으로 이 효과가 검증된 것은 아니다.
 * SSO/LDAP 계정의 null PASSWORD_HASH를 BCrypt에 전달해 500이 발생하는 문제를 차단한다.
 * 비밀번호 변경 DB 트랜잭션이 롤백됐는데 Redis 세션만 먼저 삭제되는 순서 문제를 피한다.
 */

package com.soft2cost2.auth.service;

//import com.fasterxml.jackson.core.JsonProcessingException;
import com.soft2cost2.auth.dto.ChangePasswordRequest;
import com.soft2cost2.auth.dto.LoginRequest;
import com.soft2cost2.auth.dto.LoginResponse;
import com.soft2cost2.auth.mapper.AuthMapper;
import com.soft2cost2.auth.model.*;
import com.soft2cost2.common.error.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthMapper authMapper;
    private final PermissionService permissionService;
    private final RedisAuthSessionService redisAuthSessionService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final String dummyPasswordHash;
    private final int lockThreshold;
    private final int lockMinutes;

    public AuthService(
            AuthMapper authMapper,
            PermissionService permissionService,
            RedisAuthSessionService redisAuthSessionService,
            PasswordEncoder passwordEncoder,
            LoginAttemptService loginAttemptService,
            ApplicationEventPublisher applicationEventPublisher,
            @Value("${soft2cost2.auth.login.failure-threshold:5}") int lockThreshold,
            @Value("${soft2cost2.auth.login.lock-minutes:15}") int lockMinutes
    ) {
        if (lockThreshold < 3 || lockThreshold > 15) {
            throw new IllegalStateException("failure-threshold must be between 3 and 15");
        }
        if (lockMinutes < 1 || lockMinutes > 1440) {
            throw new IllegalStateException("lock-minutes must be between 1 and 1440");
        }
        this.authMapper = authMapper;
        this.permissionService = permissionService;
        this.redisAuthSessionService = redisAuthSessionService;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.lockThreshold = lockThreshold;
        this.lockMinutes = lockMinutes;
        this.dummyPasswordHash = passwordEncoder.encode("Soft2Cost1-Dummy-Password");
    }

    // login 자체에는 @Transactional 을 붙이지 않는다.
    public LoginResponse login(LoginRequest request) {

        String loginId = request.loginId().trim().toUpperCase(Locale.ROOT);
        loginAttemptService.unlockExpired(loginId);

        log.info(">>> AuthService.login() 진입 loginId={}", loginId);

        UserAccount user = authMapper.selectUserByLoginId(loginId);

        // 사용자가 존재하지 않더라도 dummy BCrypt hash와 비교한다.
        // 존재하지 않는 ID만 매우 빨리 실패하도록 만들지 않아 계정 존재 여부를 시간 차이로 추측하기 어렵게 한다.
        String passwordHash = user == null || user.passwordHash() == null ? dummyPasswordHash : user.passwordHash();
        boolean passwordMatches = passwordEncoder.matches(request.password(), passwordHash);

        if (user == null || !"Y".equals(user.useYn()) || !"LOCAL".equals(user.authType())) {
            throw loginFailed();
        }

        if (!passwordMatches) {

            log.info("!passwordMatches logId={}", loginId);

            // todo: DB에는 평문 비밀번호가 아니라 BCrypt hash가 저장되어야 한다.
            //      PasswordEncoder.matches가 입력 평문을 같은 방식으로 계산해 DB hash와 비교한다.
            if ("ACTIVE".equals(user.userStatus())) {
                loginAttemptService.recordFailure(user.userId(), lockThreshold, lockMinutes);
            }
            throw loginFailed();
        }

        if ("LOCKED".equals(user.userStatus()) && user.lockedUntil() != null && LocalDateTime.now().isBefore(user.lockedUntil())) {
            log.info("!LOCKED logId={}", loginId);
            //올바른 비밀번호를 확인한 뒤에만 잠금 상태를 알린다.
            throw new ApiException("E306", HttpStatus.UNAUTHORIZED, "계정이 일시 잠겼습니다!!!");
        }

        if (!"ACTIVE".equals(user.userStatus())) {
            log.info("!ACTIVE logId={}", loginId);
            throw loginFailed();
            //throw new ApiException("E301", HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다!!!");
        }

        // todo: 주의할 점은 SYSTEM_ADMIN이라는 역할 코드가 존재하는 것과 메뉴 권한 데이터가 존재하는 것은 별개라는 것이다.
        //  S2C_ROLE_MENU_AUTH에 연결된 활성 권한이 없다면 역할은 표시되지만 메뉴 결과는 빈 배열일 수 있다.
        List<String> roleCodes = authMapper.selectRoleCodes(user.userId());

        // 최종 결과는 menuLevel, sortOrder, menuId 순으로 정렬한다.
        // menuId, parentMenuId, menuCode, menuName, menuLevel, menuType, screenId, formPath,
        // iconName, sortOrder, authLevel, canExcel, canApprove, canClose, canCostView, dataScope
        List<MenuPermission> permissions = permissionService.calculate(
                authMapper.selectAllMenus(),
                authMapper.selectRoleMenuAuth(user.userId()),
                authMapper.selectUserMenuAuth(user.userId())
        );
        // todo: 현재 메인 화면의 메뉴 Grid가 비어 있다면 다음을 구분해야 한다.
        //      1. GET /api/v1/navigation/menus가 401 또는 403인가, 인증 토큰 또는 세션 문제이다.
        //      2. GET /api/v1/navigation/menus가 200이고 응답이 빈 배열인가,
        //          인증은 성공했지만 최종 visibleIds에 포함된 권한 메뉴가 없는 것이다.

        // 정책 선택: 역할이 없는  계정의 로그인 자체를 막고 싶으면 여기서 "E304".
        loginAttemptService.recordSuccess(user.userId());

        boolean passwordChangeRequired = "Y".equals(user.mustChangePasswordYn());

        // todo: Redis용 AuthSessionContext를 만든다. AuthService는 로그인 과정에서 확보한 정보를 하나의 세션 객체로 묶는다.
        //      이 객체에는 다음이 들어간다.
        //          사용자 식별 정보
        //          회사, 사업장, 부서 범위
        //          역할 코드
        //          계산이 끝난 메뉴 권한
        //          초기 비밀번호 변경 필요 여부
        //          세션 발급 시각과 만료 시각
        //      메뉴 권한을 로그인할 때 계산해 Redis에 넣기 때문에, 다음 메뉴 API에서 Oracle을 다시 조회하지 않아도 된다.
        AuthSessionContext context = new AuthSessionContext(
                user.userId(),
                user.loginId(),
                user.userName(),
                user.companyId(),
                user.siteId(),
                user.deptId(),
                roleCodes,
                permissions,
                passwordChangeRequired,
                null,
                null
        );

        // 내부 순서는 다음과 같다.
        //  SecureRandom으로 원본 accessToken 생성
        //  accessToken을 SHA-256으로 hash
        //  hash를 이용해 Redis 세션 Key 생성
        //  AuthSessionContext에 issuedAt과 expiresAt 설정
        //  Context를 JSON으로 변환
        //  Redis에 TTL과 함께 저장
        //  사용자별 token hash Set에도 등록
        //  원본 accessToken은 LoginResponse로 반환
        RedisAuthSessionService.IssuedSession issued = redisAuthSessionService.issue(context);
        // todo: 보안상 중요한 차이는 다음과 같다.
        //          Nexacro는 원본 accessToken을 보관한다.
        //          Redis Key에는 원본 대신 hash를 사용한다.
        //          이후 요청에서는 Nexacro가 X-Auth-Token 헤더로 원본 token을 보낸다.
        //          서버는 다시 hash한 뒤 Redis 세션을 찾는다.

//        try {
//            log.info(">>> AuthService loginId={},  permissions JSON={}",
//                    loginId,
//                    new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(permissions)
//            );
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }

        // 위 import와 전체 try/catch 블록을 제거한다.
        log.debug("Login permission summary. loginId={}, menuCount={}",
                loginId, permissions.size());

        return new LoginResponse(
                issued.accessToken(),
                user.userId(),
                user.loginId(),
                user.userName(),
                roleCodes,
                passwordChangeRequired,
                issued.context().expiresAt()
        );
        // todo: 응답 구조는 다음과 같다.
        //  {
        //      "accessToken": "발급된 토큰",
        //      "userId": 3,
        //      "loginId": "ADMIN",
        //      "userName": "사용자명",
        //      "roleCodes": ["SYSTEM_ADMIN"],
        //      "passwordChangeRequired": false,
        //      "expiresAt": "세션 만료 시각"
        //  }

    }

    @Transactional
    public void changePassword(
            AuthSessionContext session,
            ChangePasswordRequest request
    ) {

        if (session == null) {
            throw new ApiException(
                    "E303",
                    HttpStatus.UNAUTHORIZED,
                    "인증 세션이 유효하지 않습니다!!!"
                    );
        }

        UserAccount user = authMapper.selectUserByLoginId(session.loginId());
        if (user == null || user.passwordHash() == null || !user.userId().equals(session.userId())
                || !passwordEncoder.matches(request.currentPassword(), user.passwordHash())
        ) {
            throw new ApiException(
                    "E301",
                    HttpStatus.UNAUTHORIZED,
                    "현재 비밀번호가 올바르지 않습니다!!!"
            );
        }

        if (passwordEncoder.matches(request.newPassword(), user.passwordHash())) {
                    throw new ApiException(
                            "E307",
                            HttpStatus.UNAUTHORIZED,
                            "새 비밀번호는 현재 비밀번호와 달라야 합니다!!!"
                    );
        }

        String newPasswordHash = passwordEncoder.encode(request.newPassword());
        int updated = authMapper.updatePassword(user.userId(), newPasswordHash, user.loginId());
        if (updated != 1) {
            throw new IllegalStateException("비밀 번호 갱신 실패!!!");
        }

        applicationEventPublisher.publishEvent(((new RedisAuthSessionService.PasswordChangedEvent(user.userId()))));

        // 트랜잭션 거밋 후 실행되도록 이벤트로 발행한다.
        // applicationEventPublisher.publishEvent(new PasswordChangedEvent(user.userId()));

    }

    private ApiException loginFailed() {
        return new ApiException(
                "E301",
                HttpStatus.UNAUTHORIZED,
                "아이디 또는 비밀번호가 올바르지 않습니다!!!"
        );
    }

}
