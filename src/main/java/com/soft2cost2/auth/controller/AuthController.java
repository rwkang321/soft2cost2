/** 2026.08.14 AuthController.java by rwkang
 * 1. "/api/vi/auth/login" Controller
 *
 */
/** 2026.08.17 Updated by cdx.
 * 변경 이유:
 * DTO 검증을 활성화하고 logout과 change-password의 실제 HTTP 계약을 구현한다.
 * logout은 멱등적으로 설계해 이미 만료·삭제된 토큰에도 204를 반환한다.
 * 보안 영향:
 * 현재 RedisAuthSessionService.logout만 존재하고 외부에서 호출할 수 없는 공백을 제거한다.
 * 비밀번호 변경 완료 뒤 기존 토큰으로 업무 API가 호출되지 않도록 전 세션 폐기와 재로그인을 요구하는 정책이 가장 단순하고 안전하다.
 */

package com.soft2cost2.auth.controller;

import com.soft2cost2.auth.dto.ChangePasswordRequest;
import com.soft2cost2.auth.dto.LoginRequest;
import com.soft2cost2.auth.dto.LoginResponse;
import com.soft2cost2.auth.model.AuthSessionContext;
import com.soft2cost2.auth.service.AuthService;
import com.soft2cost2.auth.service.RedisAuthSessionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final RedisAuthSessionService sessions;

    public AuthController(AuthService authService, RedisAuthSessionService sessions) {
        this.authService = authService;
        this.sessions = sessions;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        // @RequestBody: 요청 JSON 을 LoginRequest 로 변환.
        // @Valid: LoginRequest 의 검증 규칙 실행 :
        //      LoginRequest는 loginId가 비어 있지 않은지, 최대 50자인지, 허용된 문자만 사용하는지 검사한다.
        //      password도 비어 있지 않은지와 최대 길이를 검사한다.

        log.info(">>> AuthController.login() 진입 loginId={}", request.loginId());

        return authService.login(request);

    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @AuthenticationPrincipal AuthSessionContext session,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(session, request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @RequestHeader(
                    value = "X-Auth-Token",
                    required = false
            ) String accessToken
    ) {
        sessions.logout(accessToken);
    }

}
