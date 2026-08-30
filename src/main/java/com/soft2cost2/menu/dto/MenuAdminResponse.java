package com.soft2cost2.menu.dto;

import java.time.LocalDateTime;

public record MenuAdminResponse(
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
        String sensitiveYn,
        String useYn,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy,
        LocalDateTime revisionAt
) {
}