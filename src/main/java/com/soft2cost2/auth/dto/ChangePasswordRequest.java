/** 2026.08.17 ChangePasswordRequest.java by cdx.
 * 신규 추가 이유:
 * UML의 12~100자 및 영문·숫자·특수문자 계약을 서버에서 최종 검증한다.
 * 확인 비밀번호는 클라이언트 UX용이며 서버에는 전송할 필요가 없다.
 * 보안 영향:
 * 비밀번호 정책을 Nexacro 우회 호출에도 강제한다.
 * 장기적으로는 금지 비밀번호 목록, 사용자 ID 포함 금지, 최근 비밀번호 재사용 금지 정책이 필요하다.
 */

package com.soft2cost2.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank
        @Size(max = 100)
        String currentPassword,

        @NotBlank
        @Size(min = 8, max = 100)
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$",
                message = "영문, 숫자, 특수 문자를 포함해야 합니다!!!"
        )
        String newPassword
) {
}
