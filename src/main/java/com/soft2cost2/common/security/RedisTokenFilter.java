/** 2026.08.14 RedisTokenFilter.java by rwkang.
 * 1. 여기 RedisTokenFilter 에서, todo: Login 다음 요청 부터는 DB Oracle 을 조회하지 않게 한다.
 * 2. todo: Spring Security 는 인증된 사용자 정보를 "SecurityContextHolder"에 보관하는 구조를 사용한다.
 *
 */

package com.soft2cost2.common.security;

import com.soft2cost2.auth.model.AuthSessionContext;
import com.soft2cost2.auth.service.RedisAuthSessionService;
import com.soft2cost2.common.error.ApiErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Component
public class RedisTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenFilter.class);

    private static final Set<String> PASSWORD_CHANGE_ALLOWED = Set.of(
            "/api/v1/auth/change-password",
            "/api/v1/auth/logout"
    );

    private final RedisAuthSessionService redisAuthSessionService;
    private final ApiErrorWriter apiErrorWriter;

    public RedisTokenFilter(RedisAuthSessionService redisAuthSessionService, ApiErrorWriter apiErrorWriter) {
        this.redisAuthSessionService = redisAuthSessionService;
        this.apiErrorWriter = apiErrorWriter;
    }

    // todo: RedisTokenFilter를 Spring 기본 UsernamePasswordAuthenticationFilter보다 먼저 실행한다.
    //      다만 로그인 URL은 토큰이 아직 없으므로 RedisTokenFilter.shouldNotFilter가 건너뛴다.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        // CORS Preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();

        // todo: 아래 url 는 token 검사 제외
        return uri.equals("/api/v1/auth/login")
                || uri.startsWith("/swagger-ui/")
                || uri.equals("/swagger-ui.html")
                || uri.startsWith("/v3/api-docs/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    )
        throws ServletException, IOException {

        // todo: 메뉴 URL은 permitAll이 아니므로 RedisTokenFilter가 실행된다.
        String accessToken = request.getHeader("X-Auth-Token");

        try {
            Optional<AuthSessionContext> session = redisAuthSessionService.find(accessToken);
            // todo: find의 내부 과정:
            //          헤더가 비어 있지 않은지 확인
            //          token을 SHA-256 hash
            //          Redis session Key 계산
            //          Redis에서 AuthSessionContext JSON 조회
            //          JSON을 Java 객체로 복원
            //          expiresAt이 현재 시각보다 뒤인지 확인

            if (session.isPresent()) {
                AuthSessionContext context = session.get();
                if (context.passwordChangeRequired() && !PASSWORD_CHANGE_ALLOWED.contains(request.getRequestURI())) {
                    apiErrorWriter.write(
                            response,
                            HttpStatus.FORBIDDEN.value(),
                            "E305",
                            request.getRequestURI(),
                            "초기 비밀번호 변경이 필요합니다!!!"
                    );
                    return;
                }

                // 유효한 세션이면 역할 코드를 Spring Security 권한으로 변환한다.
                // SYSTEM_ADMIN은 Spring Security 내부에서 ROLE_SYSTEM_ADMIN 형태가 된다.
                var authorities = context.roleCodes().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();
                // 그다음 인증 객체를 현재 요청의 SecurityContext에 저장한다.
                var authentication = new UsernamePasswordAuthenticationToken(context, null, authorities);

                // 이 순간부터 Spring Security는 이 메뉴 요청을 인증된 요청으로 판단한다.
                SecurityContextHolder.getContext().setAuthentication(authentication);

            }

            filterChain.doFilter(request, response);

        } catch (RedisAuthSessionService.SessionStoreException exception) {
            SecurityContextHolder.clearContext();;
            apiErrorWriter.write(
                    response,
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    "E503",
                    request.getRequestURI(),
                    "인증 세션 저장소를 사용할 수 없습니다!!!"
            );
        }

    }

}
