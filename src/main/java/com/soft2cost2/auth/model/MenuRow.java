/** 2026.08.13 MenuRow.java by rwkang.
 * 1.
 */

package com.soft2cost2.auth.model;

public record MenuRow(

        Long menuId,
        Long parentMenuId,

        String menuCode,
        String menuName,

        Integer menuLevel,
        String menuType,

        String screenId,
        String formPath,
        String iconName,

        Integer sortOrder

) {
}
