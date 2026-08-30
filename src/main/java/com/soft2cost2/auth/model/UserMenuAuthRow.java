/** 2026.08.13 UserMenuAuthRow.java by rwkang.
 * 1. 사용자 예외 권한 Row
 * 2. todo: 여기서 중요한 것은, S2C_USER_MENU_AUTH 갑이 NULL 이면, "Role 권한 그대로 상속" 한다는 것이다.
 *
 */

package com.soft2cost2.auth.model;

public record UserMenuAuthRow(

        Long menuId,

        String authLevel,

        String canExcel,
        String canApprove,
        String canClose,
        String canCostView,

        String dataScope

) {
}
