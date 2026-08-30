/** 2026.08.13 AuthSessionContext.java by rwkang.
 * 1. Resis 세션 객체 : Redis 에 저장 되는 핵심 객체.
 *
 */
/** 2026.08.17 Updated by cdx.
 * 오타 userNaem을 userName으로 바로잡는다.
 * Redis 역직렬화와 권한 검사에서 사용하는 세션 값을 불변 복사하고, 초기 비밀번호 상태를 필터가 집행할 수 있게 한다.
 * 호환성 주의:
 * Redis에 기존 JSON이 남아 있다면 userNaem에서 userName으로의 필드 변경으로 역직렬화 호환성이 깨진다. 배포 시 세션 key namespace를 v2로 바꾸거나 기존 세션 전체를 폐기해야 한다.
 */

package com.soft2cost2.auth.model;

import java.time.Instant;
import java.util.List;

public record AuthSessionContext(

        Long userId,
        String loginId,
        String userName,

        Long companyId,
        Long siteId,
        Long deptId,

        List<String> roleCodes,

        List<MenuPermission> menus,

        // cdx added
        boolean passwordChangeRequired,

        Instant issuedAt,
        Instant expiresAt

) {
    public AuthSessionContext {
        roleCodes = List.copyOf(roleCodes);
        menus = List.copyOf(menus);
    }
}
