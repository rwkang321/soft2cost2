package com.soft2cost2.menu.controller;

import com.soft2cost2.auth.model.AuthSessionContext;
import com.soft2cost2.common.security.PermissionGuard;
import com.soft2cost2.menu.dto.MenuAdminResponse;
import com.soft2cost2.menu.dto.MenuCreateRequest;
import com.soft2cost2.menu.dto.MenuUpdateRequest;
import com.soft2cost2.menu.service.MenuAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/menus")
public class MenuAdminController {

    private static final String MENU_CODE = "SYS_MENU_MGMT";

    private final PermissionGuard permissionGuard;
    private final MenuAdminService menuAdminService;

    public MenuAdminController(
            PermissionGuard permissionGuard,
            MenuAdminService menuAdminService
    ) {
        this.permissionGuard = permissionGuard;
        this.menuAdminService = menuAdminService;
    }

    @GetMapping
    public List<MenuAdminResponse> list(
            @AuthenticationPrincipal AuthSessionContext session
    ) {
        permissionGuard.require(session, MENU_CODE, "R");
        return menuAdminService.findAll(session);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MenuAdminResponse create(
            @AuthenticationPrincipal AuthSessionContext session,
            @Valid @RequestBody MenuCreateRequest request
    ) {
        permissionGuard.require(session, MENU_CODE, "W");
        return menuAdminService.create(session, request);
    }

    @PutMapping("/{menuId}")
    public MenuAdminResponse update(
            @AuthenticationPrincipal AuthSessionContext session,
            @PathVariable Long menuId,
            @Valid @RequestBody MenuUpdateRequest request
    ) {
        permissionGuard.require(session, MENU_CODE, "W");
        return menuAdminService.update(session, menuId, request);
    }

    @DeleteMapping("/{menuId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(
            @AuthenticationPrincipal AuthSessionContext session,
            @PathVariable Long menuId
    ) {
        permissionGuard.require(session, MENU_CODE, "W");
        menuAdminService.disable(session, menuId);
    }
}