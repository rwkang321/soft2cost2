/** 2026.08.15 UserService.java by rwkang.
 * 1. 최초 1회만, "ADMIN", "시스템 관리자" 등록을 위해.
 * 2. todo: "비밀번호"는 절대 평문을 넣지 않고, "PasswordEncoder"로 단방향 해시 값으로 저장.
 * *. todo: Spring Security PasswordEncoder.encode() and matches
 */
/** 2026.08.19 Updated by cdx.
 * 변경 이유:
 * INSERT를 하는 메소드에서 readOnly를 제거한다.
 * rowPassword 오타를 rawPassword로 고치고 최소 길이를 검증한다.
 * 기존 사용자의 비밀번호를 서버 재시작 때 재설정하지 않는 현재의 좋은 정책은 유지한다.
 *
 * 보안 영향과 한계:
 * Java String 기반 @Value로 받은 비밀은 즉시 메모리 삭제할 수 없다. 가능하면 파일/secret reference를 통해 일회 읽고 bootstrap 종료 뒤 참조를 버린다.
 * S2C 회사 행은 현재 마이그레이션에서 seed되지 않는다. bootstrap 전에 company-code에 대응하는 회사가 있어야 한다는 운영 전제 또는 별도 승인된 seed 절차가 필요하다.
 */

package com.soft2cost2.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.soft2cost2.admin.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Locale;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private static final int MIN_BOOTSTRAP_PASSWORD_LENGTH = 8;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    // LOGIN_ID -> 내부 USER_ID 조회
    @Transactional(readOnly = true)
    public Long findUserIdByLoginId(String loginId) {

        if (loginId == null || loginId.isBlank()) { return null; }

        return userMapper.selectUserIdByLoginId(loginId.trim()); // "ADMIN"

    }

    // COMPANY_CODE -> COMPANY_ID 조회.
    @Transactional(readOnly = true)
    public Long findCompanyIdByCode(String companyCode) {

        if (companyCode == null || companyCode.isBlank()) { return null; }

        return userMapper.selectCompanyIdByCode(companyCode.trim());

    }

    // 최초 "ADMIN" 생성, 이미 존재하면,
    //? 이미 존재하면 비밀 번호를 절대 다시 초기화 하지 않는다.
    //@Transactional(readOnly = true) //* DB 드라이버나 트랜잭션 매니저 설정에 따라 SQLException: Connection is read-only 또는 TransactionSystemException 같은 런타임 에러
    @Transactional //* default : readOnly = false
    public Long createInitialAdmin(String loginId, String userName, char[] rawPassword, String companyCode) {

        try {

            String normalizedLoginId = requireTrimedText(loginId, "loginId").toUpperCase(Locale.ROOT);
            String normalizedUserName = requireTrimedText(userName, "userName");
            String normalizedCompanyCode = requireTrimedText(companyCode, "companyCode");
            //String normalizedPassword = requireTrimedText(rowPassword, "rowPassword");

            if (rawPassword == null || rawPassword.length < 8) {
                throw new IllegalStateException("Bootstrap 비밀번호는 8자 이상이여야 합니다!!!");
            }

            //? 이미 "ADMIN" 이 있으면, 절대 생성 하지 않는다.
            //? 서버를 재 시작 했다고, "ADMIN 비밀번호"를 다시 초기화 하지 않는다.
            //Long existingUserId = userMapper.selectUserIdByLoginId(normalizedLoginId);
            //if (existingUserId != null) { return existingUserId; }
            Long existingUserId = userMapper.selectUserIdByLoginId(normalizedLoginId);
            if (existingUserId != null) {
                return existingUserId;
            }

            //? "S2C_USER.COMPANY_ID"가 NOT NULL 이므로 회사가 먼저 존재 해야 한다.
            log.info(
                    "Bootstrap companyCode=[{}], length={}", companyCode, companyCode == null ? null : companyCode.length()
            );

            Long companyId = userMapper.selectCompanyIdByCode(normalizedCompanyCode);
            if (companyId == null) {
                throw new IllegalStateException("Bootstrap 회사가 존재하지 않습니다. COMPANY_CODE=" + normalizedCompanyCode);
            }

            //* 평문 비밀번호 -> 반드시 BCrypt
            String hash = passwordEncoder.encode(CharBuffer.wrap(rawPassword));
            Arrays.fill(rawPassword, '\0');

            userMapper.mergeInitialAdmin(normalizedLoginId, normalizedUserName, companyId, hash, "BOOTSTRAP");

            Long createdUserId = userMapper.selectUserIdByLoginId(normalizedLoginId);
            if (createdUserId == null) {
                throw new IllegalStateException("ADMIN 조회 실패!!!");
            }

            return createdUserId;

        } finally {
            if (rawPassword != null) { Arrays.fill(rawPassword, '\0'); }
        }

        /*
        //* 평문 비밀번호 -> 반드시 BCrypt
        String passwordHash = passwordEncoder.encode(rowPassword);
        int inserted = userMapper.insertInitialAdmin(
                normalizedLoginId,
                normalizedUserName,
                companyId,
                passwordHash,
                "BOOTSTRAP"
        );
        if (inserted != 1) {
            throw new IllegalStateException("최초 [ADMIN] 사용자 생성 실패!!!");
        }
        Long createdUserId = userMapper.selectUserIdByLoginId(normalizedLoginId);
        if (createdUserId == null) {
            throw new IllegalStateException("[ADMIN] 생성 후 USER_ID 조회 실패!!!");
        }
        return createdUserId;
         */

    }

    private String requireTrimedText (String value, String fieldName) {

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(fieldName + " 값이 없습니다!!!");
        }

        return value.trim();
    }


    public void requireActiveCompany(String companyCode) {
        String code = requireTrimedText(companyCode, "companyCode");
        if (userMapper.countCompanyByCode(code) != 1) {
            throw new IllegalStateException("Bootstrap 회사가 존재하지 않습니다.");
        }
        if (userMapper.countActiveCompanyByCode(code) != 1) {
            throw new IllegalStateException("Bootstrap 회사가 비활성 상태입니다.");
        }
    }

    public void requireExistingAdminConforms(
            Long userId, String loginId, String companyCode
    ) {
        if (userMapper.countConformingInitialAdmin(
                userId, loginId.trim(), companyCode.trim()
        ) != 1) {
            throw new IllegalStateException(
                    "기존 ADMIN의 인증 유형·상태·회사에 drift가 있습니다. 자동 덮어쓰지 않습니다."
            );
        }
    }


}
