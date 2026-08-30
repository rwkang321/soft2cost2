/** 2026.08.13 RoleMenuAuthRow.java by rwkang.
 * 1. 역할 권한 Row
 */

package com.soft2cost2.auth.model;

public record RoleMenuAuthRow(

        Long menuId,

        String authLevel,

        String canExcel,
        String canApprove,
        String canClose,
        String canCostView,

        String dataScope

) {
}
