/** 2026.08.17 ApiError.java by cdx.
 * 신규 이유:
 * Nexacro가 HTTP 상태와 무관하게 안정적인 E 코드로 분기할 수 있게 한다.
 * 내부 예외 메시지와 stack trace를 응답에 노출하지 않는다.
 * 권장 코드:
 * E301: 로그인 자격 증명 실패
 * E302: 잘못된 요청/검증 실패
 * E303: 토큰 없음·만료·폐기
 * E304: 메뉴/기능/데이터 범위 권한 없음
 * E305: 초기 비밀번호 변경 필요
 * E306: 올바른 자격 증명을 확인한 잠긴 계정
 * E307: 비밀번호 정책 위반
 */

package com.soft2cost2.common.error;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public final class ApiException extends RuntimeException{

    private final String code;
    private final HttpStatus status;

    public ApiException (String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String code() { return code; }
    public HttpStatus status() { return status; }

}



