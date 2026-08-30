/** 2026.08.17 LoginAttemptService by cdx.
 * 신규 추가 이유: AuthService.login 의 동일 트랜잭션에서 실패 횟수 증가 후 예외 발생.
 * 인증 실패 예외와 무관하게 실패 횟수·잠금 상태를 독립 커밋한다.
 * 같은 클래스 내부 메소드 호출은 Spring 프록시를 거치지 않으므로 반드시 별도 Bean이어야 한다.
 * 보안 영향:
 * 수정 후 실패 상태가 예외와 분리돼 commit되도록 한다. 실제 DB commit 여부는 섹션 9.2에서 검증해야 한다.
 * 계정 잠금만으로는 IP 기반 공격과 의도적 계정 잠금 공격을 막지 못하므로 Redis rate limiter를 별도 추가해야 한다.
 */

package com.soft2cost2.auth.service;

import com.soft2cost2.auth.mapper.AuthMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptService {

    private final AuthMapper authMapper;

    public LoginAttemptService(AuthMapper authMapper) {

        this.authMapper = authMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void unlockExpired(String loginId) {
        authMapper.unlockExpiredUser(loginId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long userId, int threshold, int lockMinutes) {
        if (authMapper.recordLoginFailure(userId, threshold, lockMinutes) != 1) {
            throw new IllegalStateException("로그인 실패 상태 갱신 실패!!!");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(Long userId) {
        if (authMapper.recordLoginSuccess(userId) != 1) {
            throw new IllegalStateException("로그인 성공 상태 갱신 실패!!!");
        }
    }

}
