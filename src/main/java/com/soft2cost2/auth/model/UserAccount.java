/** 2026.08.13 UserAccount.java by rwkang.
 * 1. S2C_USER 테이블 구조에 맞는 record.
 */

package com.soft2cost2.auth.model;

import java.time.LocalDateTime;

public record UserAccount(

        Long userId,
        String loginId,
        String userName,

        Long companyId,
        Long siteId,
        Long deptId,

        String passwordHash,

        // cdx added
        String authType,

        String userStatus,

        Integer failedLoginCount,

        LocalDateTime lockedUntil,

        // cdx added
        LocalDateTime passwordChangedAt,
        String mustChangePasswordYn,

        String useYn

) {
}
