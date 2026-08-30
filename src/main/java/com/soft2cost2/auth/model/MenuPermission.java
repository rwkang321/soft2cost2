/** 2026.08.13 MenuPermission.java by rwkang.
 * 1. 최종 계산 후 권한
 * 2. todo: 예를 들어,
 *    todo: 원가 조회
 *    todo: AUTH_LEVEL = R
 *    todo: CAN_EXCEL = Y
 *    todo: CAN_APPROVE = N
 *    todo: CAN_CLOSE = N
 *    todo: CAN_COST_VIES = Y
 *    todo: DATA_SCOPE = DEPT
 *    todo: 위와 같이 계산된 결과 권한이 들어 간다.
 */

package com.soft2cost2.auth.model;

public record MenuPermission(

        Long menuId,
        Long parentMenuId,

        String menuCode,
        String menuName,

        Integer menuLevel,
        String menuType,

        String screenId,
        String formPath,
        String iconName,

        Integer sortOrder,

        String authLevel,

        String canExcel,
        String canApprove,
        String canClose,
        String canCostView,

        String dataScope

) {
}
