/** 2026.08.17 ApiExceptionHandler.java by cdx.
 * 신규 추가 이유:
 * 컨트롤러/서비스 오류 형식을 통일한다.
 * Spring Security 필터에서 발생하는 401/403은 이 Advice를 거치지 않으므로 SecurityConfig의 entry point/denied handler도 동일 JSON 포맷을 써야 한다.
 */

package com.soft2cost2.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {
    public static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

//    private final MessageService messages;
}


/**
@RestControllerAdvice
public class ApiExceptionHandler {

    private static Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handle(
            ApiException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(exception.status()).body(new ApiError(
                exception.code(),
                exception.getMessage(),
                request.getRequestURI(),
                MDC.get("traceId"),
                Instant.now()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("요청 값이 올바르지 않습니다!!!");
        return ResponseEntity.badRequest().body(new ApiError(
                "E302", message, request.getRequestURI(), MDC.get("traceId"), Instant.now()
        ));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled API error. path={}", request.getRequestURI(), ex);
        // 서버 로그에는 ex 전체를 기록하고, 응답에는 일반 메시지만 반환
        return ResponseEntity.internalServerError().body(new ApiError(
                "E500", "서버 오류가 발새했습니다!!!",
                request.getRequestURI(), MDC.get("traceId"), Instant.now()
        ));
    }
}
 */

