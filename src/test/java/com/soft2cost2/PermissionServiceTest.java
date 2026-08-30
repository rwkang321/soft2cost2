package com.soft2cost2;

import com.soft2cost2.auth.model.MenuPermission;
import com.soft2cost2.auth.model.MenuRow;
import com.soft2cost2.auth.model.RoleMenuAuthRow;
import com.soft2cost2.auth.service.PermissionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionServiceTest {
    private final PermissionService service = new PermissionService();

    @Test
    void combinesRoleWriteAndIncludesParent() {
        MenuRow group = new MenuRow(
                1L, null, "ADMIN_GRP", "관리", 1, "GROUP",
                null, null, null, 10
        );
        MenuRow screen = new MenuRow(
                2L, 1L, "ADMIN_SCREEN", "관리 화면", 2, "SCREEN",
                "ADMIN_SCREEN", "Admin::Screen.xfdl", null, 10
        );
        RoleMenuAuthRow screenWrite = new RoleMenuAuthRow(
                2L, "W", "Y", "N", "N", "Y", "COMPANY"
        );

        List<MenuPermission> result = service.calculate(
                List.of(group, screen), List.of(screenWrite), List.of()
        );

        assertEquals(List.of("ADMIN_GRP", "ADMIN_SCREEN"),
                result.stream().map(MenuPermission::menuCode).toList());
        assertEquals("N", result.get(0).authLevel());
        assertEquals("W", result.get(1).authLevel());
        assertEquals("Y", result.get(1).canExcel());
        assertEquals("Y", result.get(1).canCostView());
        assertEquals("COMPANY", result.get(1).dataScope());
    }
}