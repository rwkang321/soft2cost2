/** 2026.08.14 SecurityConfig.java by rwkang.
 * 1. Password Encoder 설정.
 * todo: 여기서 중요한 것은,
 * todo: DB Oracle 에 "S2C_USER.PASSWORD_HASH" 컬럼에는, 절대로 "password1234..." 이런식으로 저장되면 안 되는 것이다.
 * 2. Nexacro 포트는 9091, Spring API 포트는 8081로 잡았고,  /auth/login, /navigation/menus 구조로 사용한다.
 */

package com.soft2cost2.common.security;

import com.soft2cost2.common.error.ApiAccessDeniedHandler;
import com.soft2cost2.common.error.ApiAuthenticationEntryPoint;
import com.soft2cost2.common.error.ApiErrorWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.DispatcherType;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final RedisTokenFilter redisTokenFilter;
    private final ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;
    private final ApiAccessDeniedHandler apiAccessDeniedHandler;
    private final String allowedOrigin;
    private final boolean swaggerEnabled;
    private final int bcryptStrength;

    public SecurityConfig(
            RedisTokenFilter redisTokenFilter,
            ApiAuthenticationEntryPoint apiAuthenticationEntryPoint,
            ApiErrorWriter apiErrorWriter,
            @Value("${soft2cost2.cors.allowed-origins[0]}") String allowedOrigin,
            @Value("${soft2cost2.security.swagger-enabled:false}") boolean swaggerEnabled,
            @Value("${soft2cost2.auth.login.bcrypt-strength:8}") int bcryptStrength
    ) {

        if (allowedOrigin == null || allowedOrigin.isBlank()) {
            throw new IllegalStateException("At least one CORS origin is required");
        };

        if (bcryptStrength < 8 || bcryptStrength > 14) {
            throw new IllegalStateException("bcrypt-strength must be between 8 and 14");
        }

        this.redisTokenFilter = redisTokenFilter;
        this.apiAuthenticationEntryPoint = apiAuthenticationEntryPoint;
        this.apiAccessDeniedHandler = new ApiAccessDeniedHandler(apiErrorWriter);
        this.allowedOrigin = allowedOrigin;
        this.swaggerEnabled = swaggerEnabled;
        this.bcryptStrength = bcryptStrength;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(bcryptStrength);
        // return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF(Cross-Site-Request Forgery) 비활성화
                // (개발 환경이나 REST API 서버의 경우 SWAGGER 테스트를 위해 주로 비활성화)
                // todo: REST API 에서는 브라우저 쿠키/세션 기반 인증 대신 헫 토큰(Bearer Token 등)을 주로 사용하므로,
                //       일반적으로 CSRF 공격 위험이 낮아 비활성화
                .csrf(csrf -> csrf.disable())
                // Frontend vs Backend 도메인/포트 상이할 때, 교차 출처 요청 정책을 허용 하기 위해, cors 설정 적용 한다.
                .cors(Customizer.withDefaults())

                // todo: 스프링 시큐리티가 세션을 생성하거나 세션 저장소에 인증 상태를 저장하지 않도록 무상태(Stateless) 모드로 설정
                //       모든 요청은 독립적으로 토큰 검증을 거치게 된다.
                .sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 인증/인가 과정에서 발생하는 예외를 커스텀 핸들러로 처리하도록 등록
                .exceptionHandling(errors -> errors
                        // 사용자(로그인 안 됨/유효하지 않은 토큰)가 보호된 리소스에 접근할 때(401 Unauthorized) 실행할 핸들러를 지정
                        .authenticationEntryPoint(apiAuthenticationEntryPoint)
                        // todo: 인증은 되었으나 필요한 권한/역할이 없을 때(403 Forbidden) 실행할 핸들러를 지정
                        .accessDeniedHandler(apiAccessDeniedHandler))

                // HTTP 요청 경로별 인가(접근 권한) 규칙을 정의
                .authorizeHttpRequests(auth -> {
                    auth
                            // .requestMatchers(HttpMethod.OPTIONS, "/**")

                            // Spring Boot 오류 처리 dispatch 허용
                            // 내부 에러 발생 시 스프링 부트의 기본 에러 처리 디스패치(/error 경로 등)를 차단하지 않고 허용
                            .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    auth
                            // 로그인
                            // todo: 로그인 API 경로는 인증 없이 누구나 접근할 수 있도록 허용
                            .requestMatchers("/api/v1/auth/login").permitAll();

                    // swaggerEnabled 플래그가 true일 때만 Swagger UI 및
                    // API 문서 관련 경로(/swagger-ui/**, /v3/api-docs/** 등)의 비인증 접근을 허용
                    if (swaggerEnabled) {
                        auth
                                .requestMatchers(  // Swagger UI
                                    "/swagger-ui/**",
                                    "/swagger-ui.html",
                                    "/v3/api-docs/**").permitAll();
                    }

                    // todo: 위에서 명시적으로 허용한 경로를 제외한 그 외 모든 요청은 반드시 인증(토큰 검증 등)을 거쳐야 함을 강제
                    auth
                            .anyRequest().authenticated();
                            // .anyRequest().permitAll(); // 모든 경로 허용.

                })

                // todo: 요청 헤더의 토큰을 꺼내 Redis나 DB 등을 통해 검증하고
                //       인증 객체(SecurityContextHolder)를 채우는 커스텀 필터(redisTokenFilter)를
                //       스프링 시큐리티의 기본 폼 로그인 필터 앞단에 배치
                .addFilterBefore(redisTokenFilter, UsernamePasswordAuthenticationFilter.class);

        // 위에서 체이닝으로 구성한 HttpSecurity 설정을 바탕으로 최종 SecurityFilterChain 객체를 빌드하여 반환
        return http.build();

    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration corsConfiguration = new CorsConfiguration();

        // Nexacro Port 9091 허용. (Cross Origin Resource Sharing)
        corsConfiguration.setAllowedOrigins(
                List.of(
                        allowedOrigin
                )
        );

        corsConfiguration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
        );

        corsConfiguration.setAllowedHeaders(
                List.of(
                        "Content-Type",
                        "X-Auth-Token",
                        "Cache-Control",
                        "Pragma",
                        "Expires",
                        "If-Modified-Since"
                )
        );

        corsConfiguration.setAllowCredentials(false);
        corsConfiguration.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);

        return urlBasedCorsConfigurationSource;

    }
}
