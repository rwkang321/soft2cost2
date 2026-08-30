/** 2026.08.15 UserService.java by rwkang.
 * 1. 최초 1회만,
 * todo: S2C_USER 테이블에, "ADMIN", "시스템 관리자" 등록을 위해.
 * todo: S2C_USER_ROLE 테이블에, "ADMIN", "SYSTEM_ADMIN"
 * todo: ***** 제일 중요한 점 : [ADMIN] 이미 존재 하더라도, "비밀번호" 절대 재설정 안 한다는 것이다. *****
 * todo: 서버를 재시자할 때마다 Bootstrap 비밀번호로 되돌아가 버리면, 심각한 운영 문제가 생긴다.
 *
 */
/** 2026.08.19 Updated by cdx.
 * todo: 이미 최초 등록했으므로...
 * 사용자 확인 경계: 기동 로그는 기존 ADMIN 계정 확인과 bootstrap 완료를 기록했다.
 * ADMIN 또는 SYSTEM_ADMIN이 없는 fresh schema의 생성 branch는 확인하지 않았다.
 */
//* 변경 이유:
//* 주석 처리된 ConditionalOnProperty를 실제로 활성화하고 matchIfMissing을 사용하지 않는다.
//* 비밀번호 값과 hash를 로그에 남기지 않는다.
//* 역할 ID, 사용자, 역할 연결, 메뉴 권한이 모두 보장돼야 bootstrap 완료로 기록한다.
//
//* 운영 주의:
//* 여러 서비스 메소드의 트랜잭션이 나뉘므로 중간 실패 시 부분 상태가 남을 수 있다.
//* MERGE의 반복 실행으로 복구 가능하게 만들되, 가능하면 별도 BootstrapFacade의 단일 트랜잭션으로 묶는다.
//* bootstrap 성공 후 환경 secret을 제거하고 enabled=false로 재배포한다.

//* AdminBootstrap.java: 개발 전용·단일 진입점
//* @Profile("dev")를 추가해 운영 profile에서 실수로 관리자 생성이 실행되는 것을 이중으로 막는다.

package com.soft2cost2;

import com.soft2cost2.admin.service.RoleService;
import com.soft2cost2.admin.service.UserService;
import com.soft2cost2.bootstrap.InitialAdminBootstrapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

//? 최초 등록 이후
@Profile("dev")
@Component
@ConditionalOnProperty(
        name = "soft2cost2.bootstrap.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class AdminBootstrap implements ApplicationRunner {

    private final InitialAdminBootstrapService bootstrapService;

    public AdminBootstrap(InitialAdminBootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstrapService.initialize();
        // 회사/예약 메뉴 존재와 활성 상태 검증
        // ADMIN insert-if-absent; 기존 hash/status/company는 덮어쓰지 않음
        // SYSTEM_ADMIN 활성/type 검증
        // user-role 보장
        // SYS_ADMIN_GRP=R, SYS_MENU_MGMT=W만 targeted upsert
        // blocking user override가 있으면 자동 삭제하지 않고 실패
    }

    /**
    Logger logger = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserService userService;
    private final RoleService roleService;

    public AdminBootstrap(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    //@Value("${soft2cost1.bootstrap.enabled:true}")
    //private boolean enabled;

    @Value("${soft2cost1.bootstrap.admin-login-id:ADMIN}")
    private String adminLoginId;

    @Value("${soft2cost1.bootstrap.admin-name:시스템관리자}")
    private String adminName;

    @Value("${soft2cost1.bootstrap.admin-password:}")
    private String adminPassword;

    @Value("${soft2cost1.bootstrap.company-code:S2C}")
    private String companyCode;

    @Override
    public void run(ApplicationArguments args) {
        Long userId = userService.findUserIdByLoginId(adminLoginId);
        if (userId == null) {
            if (adminPassword == null || adminPassword.isBlank()) {
                throw new IllegalStateException("신규 ADMIN 용 bootstrap secret 이 없습니다!!!");
            }
            userId = userService.createInitialAdmin(
                    adminLoginId,
                    adminName,
                    adminPassword.toCharArray(),
                    companyCode
            );

            logger.info("Initial ADMIN created. loginId={}", adminLoginId);

        } else {

            logger.info("ADMIN already exists. loginId={}", adminLoginId);

        }

        Long roleId = roleService.ensureSystemAdminRole();
        roleService.ensureAssignmentAndPermissions(userId, roleId);

        logger.info("Soft2Cost ADMIN bootstrap completed!!!");

    }
    */

}


//? 최초 등록 시.
/*
@Component
//@ConditionalOnProperty(
//        name = "soft2cost1.bootstrap.enabled",
//        havingValue = "true",
//        matchIfMissing = true
//)
public class AdminBootstrap implements ApplicationRunner {

    public static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserService userService;
    private final RoleService roleService;

    @Value("${soft2cost1.bootstrap.enabled:true}")
    private boolean enabled;

    @Value("${soft2cost1.bootstrap.admin-login-id:ADMIN}")
    private String adminLoginId;

    @Value("${soft2cost1.bootstrap.admin-name:시스템관리자}")
    private String adminName;

    @Value("${soft2cost1.bootstrap.admin-password:}")
    private String adminPassword;

    @Value("${soft2cost1.bootstrap.company-code:S2C}")
    private String companyCode;

    public AdminBootstrap(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    @Override
    public void run(ApplicationArguments args) {

        if (!enabled) {
            log.info("Soft2Cost Admin Bootstrap disabled!!!");
            return;
        }

        // 1. [SYSTEM_ADMIN] 역할 보장
        //Long roleId = roleService.createSystemAdminRoleIfNotExists();

        // 2. [ADMIN] 존재 체크
        Long userId = userService.findUserIdByLoginId(adminLoginId);

        //? 3. [ADMIN] 없을 때만 생성
        if (userId == null) {

            if (adminPassword == null || adminPassword.isBlank()) {
                throw new IllegalStateException(
                        "최초 [ADMIN] 계정이 존재하지 않지만, [SOFT2COST1_ADMIN_PASSWORD]가 설정되지 않았습니다!!!"
                );
            }

            userId = userService.createInitialAdmin(
                    adminLoginId,
                    adminName,
                    adminPassword.toCharArray(),
                    companyCode
            );

            log.info("Initial [ADMIN] created. loginId={}", adminLoginId);

        } else {

            log.info("[ADMIN] already exists. loginId={}", adminLoginId);

        }

        // 4. [ADMIN] <-> [SYSTEM_ADMIN] 보장
        roleService.assignRoleIfNotExists(userId, roleId);

        log.info("Soft2Cost [ADMIN] Bootstrap completed!!!");

    }

}

 */