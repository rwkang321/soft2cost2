/** 2026.08.13 LoginRequest.java by rwkang.
 * 1. 기존 UserId 대신 LoginId 사용.
 * 2. Nexacro 에서 보내온 JSON 은 아래 형식으로 통일.
 * {
 *     "loginId": "admin",
 *     "password": "Admin1234!"
 * }
 */
/** 2026.08.17 Updated by cdx.
 * 변경 이유:
 * null request 값에 의한 NullPointerException과 과도한 입력 크기를 컨트롤러 진입 전에 차단한다.
 * 로그인에서는 구체적인 비밀번호 복잡성 오류를 반환하지 않는다. 복잡성 검사는 변경 DTO에만 적용한다.
 * 빌드 전제:
 * jakarta.validation API가 현재 의존성에 포함되지 않는 경우 spring-boot-starter-validation 추가가 필요하다.
 * 본 작업에서는 build.gradle을 수정하지 않았다.
 *
 */

package com.soft2cost2.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank
        @Size(max = 50)
        @Pattern(
                regexp = "^[A-Za-z0-9._-]+$",
                message = "loginId 형식이 올바르지 않습니다!!!"
        )
        String loginId,

        @NotBlank
        @Size(max = 100)
        String password

) {
}