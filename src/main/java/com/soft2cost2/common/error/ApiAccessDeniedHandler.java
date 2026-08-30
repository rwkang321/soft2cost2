/** 2026.08.17 ApiAccessDeniedHandler.java by cdx.
 * 인증 필터 단계 오류도 ControllerAdvice와 같은 JSON 형태로 고정한다.
 *
 */

package com.soft2cost2.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ApiErrorWriter writer;

    public ApiAccessDeniedHandler(ApiErrorWriter writer) {

        this.writer = writer;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {
        writer.write(
                response,
                HttpStatus.FORBIDDEN.value(),
                "E304",
                request.getRequestURI(),"요청한 기능에 대한 권한이 없습니다!!!"
        );
    }

}
