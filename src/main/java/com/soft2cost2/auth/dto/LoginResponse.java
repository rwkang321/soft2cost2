/** 2026.08.13 LoginResponse.java by rwkang.
 * 1. 로그인 응답에는 "메뉴 불 포함"
 * 2. 로그인은 "인증", "권한 계산", "Redis 저장" 까지만 진행.
 * 3. 그 다음 "Nexacro 메인 화면"에서 "GET /api/v1/navigation/menus"를 호출.
 * todo: Nexacro가 메인 화면 전환 전에 초기 비밀번호 변경 화면을 선택할 수 있도록 서버 판정을 전달한다.
 */

package com.soft2cost2.auth.dto;

import java.time.Instant;
import java.util.List;

public record LoginResponse(

        String accessToken,
        Long userId,
        String loginId,
        String userName,
        List<String> roleCodes,

        // cdx added
        boolean passwordChangeRequired,

        Instant expiresAt

) {
}
