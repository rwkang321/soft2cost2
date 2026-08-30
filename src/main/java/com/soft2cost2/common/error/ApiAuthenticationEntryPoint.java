/** 2026.08.17 ApiAuthenticationEntryPoint.java by cdx.
 * 인증 필터 단계 오류도 ControllerAdvice와 같은 JSON 형태로 고정한다.
 *
 */

package com.soft2cost2.common.error;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.naming.AuthenticationException;
import java.io.IOException;

@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ApiErrorWriter writer;

    public ApiAuthenticationEntryPoint(ApiErrorWriter writer) {

        this.writer = writer;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            //AuthenticationException exception
            org.springframework.security.core.AuthenticationException authException
    ) throws IOException, ServletException {
        writer.write(
                response,
                HttpStatus.UNAUTHORIZED.value(),
                "E303",
                request.getRequestURI(), "인증 세션이 유효하지 않습니다!!!");
    }

}
