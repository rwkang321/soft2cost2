package com.soft2cost2.menu.service;

import com.soft2cost2.admin.mapper.RoleMapper;
import com.soft2cost2.auth.model.AuthSessionContext;
import com.soft2cost2.auth.model.AuthorizationChangedEvent;
import com.soft2cost2.common.error.ApiException;
import com.soft2cost2.common.security.PermissionGuard;
import com.soft2cost2.menu.dto.MenuAdminResponse;
import com.soft2cost2.menu.dto.MenuCreateRequest;
import com.soft2cost2.menu.dto.MenuUpdateRequest;
import com.soft2cost2.menu.mapper.MenuAdminMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class MenuAdminService {

    private static final String MENU_CODE = "SYS_MENU_MGMT";
    private static final Set<String> PROTECTED_MENU_CODES = Set.of(
            "SYS_ADMIN_GRP",
            "SYS_MENU_MGMT"
    );

    private final PermissionGuard permissionGuard;
    private final RoleMapper roleMapper;
    private final MenuAdminMapper menuMapper;
    private final ApplicationEventPublisher eventPublisher;

    public MenuAdminService(
            PermissionGuard permissionGuard,
            RoleMapper roleMapper,
            MenuAdminMapper menuMapper,
            ApplicationEventPublisher eventPublisher
    ) {
        this.permissionGuard = permissionGuard;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<MenuAdminResponse> findAll(AuthSessionContext session) {
        permissionGuard.require(session, MENU_CODE, "R");
        return List.copyOf(menuMapper.selectAll());
    }

    @Transactional
    public MenuAdminResponse create(
            AuthSessionContext session,
            MenuCreateRequest request
    ) {
        permissionGuard.require(session, MENU_CODE, "W");

        String menuCode = request.menuCode().trim().toUpperCase(Locale.ROOT);
        String menuName = request.menuName().trim();
        String menuType = request.menuType().trim().toUpperCase(Locale.ROOT);
        String screenId = nullableTrim(request.screenId());
        String formPath = nullableTrim(request.formPath());
        String iconName = nullableTrim(request.iconName());

        validateShape(menuType, screenId, formPath);
        int menuLevel = resolveParentLevel(request.parentMenuId());
        if (menuMapper.countMenuCode(menuCode, null) > 0) {
            throw conflict("이미 존재하는 메뉴 코드입니다.");
        }

        int inserted = menuMapper.insert(
                request.parentMenuId(),
                menuCode,
                menuName,
                menuLevel,
                menuType,
                screenId,
                formPath,
                iconName,
                request.sortOrder(),
                request.sensitiveYn(),
                actor(session)
        );
        if (inserted != 1) {
            throw new ApiException("E500", HttpStatus.INTERNAL_SERVER_ERROR, "메뉴 등록에 실패했습니다.");
        }

        MenuAdminResponse created = menuMapper.selectByCode(menuCode);
        if (created == null) {
            throw new ApiException("E500", HttpStatus.INTERNAL_SERVER_ERROR, "등록된 메뉴를 조회하지 못했습니다.");
        }

        Long systemAdminRoleId = roleMapper.selectRoleIdByCode("SYSTEM_ADMIN");
        if (systemAdminRoleId == null
                || roleMapper.countValidSystemAdminRole(systemAdminRoleId) != 1) {
            throw new ApiException("E500", HttpStatus.INTERNAL_SERVER_ERROR,
                    "활성 SYSTEM_ADMIN 역할을 찾지 못했습니다.");
        }

        String authLevel = "GROUP".equals(menuType) ? "R" : "W";
        int permissionAffected = roleMapper.mergeSystemAdminMenuPermissions(
                systemAdminRoleId, menuCode, authLevel, actor(session)
        );
        if (permissionAffected != 1) {
            throw new ApiException("E500", HttpStatus.INTERNAL_SERVER_ERROR,
                    "신규 메뉴의 SYSTEM_ADMIN 권한 생성에 실패했습니다.");
        }

        List<Long> affectedUserIds = roleMapper.selectCurrentUserIdsByRoleId(systemAdminRoleId);
        publishAuthorizationChanged(affectedUserIds);

        return created;
    }


    @Transactional
    public MenuAdminResponse update(
            AuthSessionContext session,
            Long menuId,
            MenuUpdateRequest request
    ) {
        permissionGuard.require(session, MENU_CODE, "W");
        requirePositiveId(menuId);

        List<MenuAdminResponse> subtree = menuMapper.selectSubtreeForUpdate(menuId);
        MenuAdminResponse current = subtree.stream()
                .filter(menu -> Objects.equals(menu.menuId(), menuId))
                .findFirst()
                .orElseThrow(() -> notFound(menuId));

        assertRevision(current.revisionAt(), request.expectedRevisionAt());

        if (Objects.equals(menuId, request.parentMenuId())) {
            throw badRequest("메뉴 자신을 부모로 지정할 수 없습니다.");
        }
        if (request.parentMenuId() != null
                && menuMapper.countInSubtree(menuId, request.parentMenuId()) > 0) {
            throw badRequest("메뉴의 자손을 부모로 지정할 수 없습니다.");
        }

        String menuName = request.menuName().trim();
        String menuType = request.menuType().trim().toUpperCase(Locale.ROOT);
        String screenId = nullableTrim(request.screenId());
        String formPath = nullableTrim(request.formPath());
        String iconName = nullableTrim(request.iconName());

        validateShape(menuType, screenId, formPath);
        protectSystemMenuStructure(current, request.parentMenuId(), menuType);

        if (!"GROUP".equals(menuType) && subtree.size() > 1) {
            throw badRequest("자식 메뉴가 있는 GROUP은 SCREEN 또는 POPUP으로 변경할 수 없습니다.");
        }

        int newLevel = resolveParentLevel(request.parentMenuId());
        int levelDelta = newLevel - current.menuLevel();

        List<Long> affectedUserIds = new java.util.ArrayList<>(
                menuMapper.selectAffectedUserIds(menuId)
        );

        int updated = menuMapper.updateRoot(
                menuId,
                request.parentMenuId(),
                menuName,
                newLevel,
                menuType,
                screenId,
                formPath,
                iconName,
                request.sortOrder(),
                request.sensitiveYn(),
                actor(session)
        );
        if (updated != 1) {
            throw conflict("메뉴가 동시에 수정되었거나 존재하지 않습니다.");
        }

        if (levelDelta != 0) {
            menuMapper.shiftDescendantLevels(menuId, levelDelta, actor(session));
        }

        Long systemAdminRoleId = roleMapper.selectRoleIdByCode("SYSTEM_ADMIN");
        if (systemAdminRoleId == null
                || roleMapper.countValidSystemAdminRole(systemAdminRoleId) != 1) {
            throw new ApiException(
                    "E500",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "활성 SYSTEM_ADMIN 역할을 찾지 못했습니다."
            );
        }

        String authLevel = "GROUP".equals(menuType) ? "R" : "W";
        int permissionAffected = roleMapper.mergeSystemAdminMenuPermissions(
                systemAdminRoleId,
                current.menuCode(),
                authLevel,
                actor(session)
        );
        if (permissionAffected != 1) {
            throw new ApiException(
                    "E500",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "수정된 메뉴의 SYSTEM_ADMIN 권한 동기화에 실패했습니다."
            );
        }

        affectedUserIds.addAll(
                roleMapper.selectCurrentUserIdsByRoleId(systemAdminRoleId)
        );

        publishAuthorizationChanged(affectedUserIds);
        return menuMapper.selectById(menuId);
    }

    /**
    @Transactional
    public MenuAdminResponse update(
            AuthSessionContext session,
            Long menuId,
            MenuUpdateRequest request
    ) {
        permissionGuard.require(session, MENU_CODE, "W");
        requirePositiveId(menuId);

        List<MenuAdminResponse> subtree = menuMapper.selectSubtreeForUpdate(menuId);
        MenuAdminResponse current = subtree.stream()
                .filter(menu -> Objects.equals(menu.menuId(), menuId))
                .findFirst()
                .orElseThrow(() -> notFound(menuId));

        assertRevision(current.revisionAt(), request.expectedRevisionAt());

        if (Objects.equals(menuId, request.parentMenuId())) {
            throw badRequest("메뉴 자신을 부모로 지정할 수 없습니다.");
        }
        if (request.parentMenuId() != null
                && menuMapper.countInSubtree(menuId, request.parentMenuId()) > 0) {
            throw badRequest("메뉴의 자손을 부모로 지정할 수 없습니다.");
        }

        String menuName = request.menuName().trim();
        String menuType = request.menuType().trim().toUpperCase(Locale.ROOT);
        String screenId = nullableTrim(request.screenId());
        String formPath = nullableTrim(request.formPath());
        String iconName = nullableTrim(request.iconName());
        validateShape(menuType, screenId, formPath);
        protectSystemMenuStructure(current, request.parentMenuId(), menuType);

        int newLevel = resolveParentLevel(request.parentMenuId());
        int levelDelta = newLevel - current.menuLevel();
        List<Long> affectedUserIds = menuMapper.selectAffectedUserIds(menuId);

        int updated = menuMapper.updateRoot(
                menuId,
                request.parentMenuId(),
                menuName,
                newLevel,
                menuType,
                screenId,
                formPath,
                iconName,
                request.sortOrder(),
                request.sensitiveYn(),
                actor(session)
        );
        if (updated != 1) {
            throw conflict("메뉴가 동시에 수정되었거나 존재하지 않습니다.");
        }

        if (levelDelta != 0) {
            menuMapper.shiftDescendantLevels(menuId, levelDelta, actor(session));
        }

        publishAuthorizationChanged(affectedUserIds);
        return menuMapper.selectById(menuId);
    }
     */

    @Transactional
    public void disable(AuthSessionContext session, Long menuId) {
        permissionGuard.require(session, MENU_CODE, "W");
        requirePositiveId(menuId);

        List<MenuAdminResponse> subtree = menuMapper.selectSubtreeForUpdate(menuId);
        if (subtree.isEmpty()) {
            throw notFound(menuId);
        }
        if (subtree.stream().anyMatch(menu -> PROTECTED_MENU_CODES.contains(menu.menuCode()))) {
            throw conflict("시스템 관리 진입에 필요한 보호 메뉴는 비활성화할 수 없습니다.");
        }

        List<Long> affectedUserIds = menuMapper.selectAffectedUserIds(menuId);
        int changed = menuMapper.disableSubtree(menuId, actor(session));
        if (changed < 1) {
            throw conflict("메뉴 비활성화 대상이 없습니다.");
        }
        publishAuthorizationChanged(affectedUserIds);
    }

    private int resolveParentLevel(Long parentMenuId) {
        if (parentMenuId == null) {
            return 1;
        }

        MenuAdminResponse parent = menuMapper.selectByIdForUpdate(parentMenuId);
        if (parent == null) {
            throw notFound(parentMenuId);
        }
        if (!"Y".equals(parent.useYn())) {
            throw badRequest("비활성 메뉴는 부모로 지정할 수 없습니다.");
        }
        if (!"GROUP".equals(parent.menuType())) {
            throw badRequest("GROUP 메뉴만 부모로 지정할 수 있습니다.");
        }
        return parent.menuLevel() + 1;
    }

    private void validateShape(
            String menuType,
            String screenId,
            String formPath
    ) {
        if ("GROUP".equals(menuType)) {
            if (screenId != null || formPath != null) {
                throw badRequest("GROUP 메뉴에는 screenId와 formPath를 지정할 수 없습니다.");
            }
            return;
        }
        if (!List.of("SCREEN", "POPUP").contains(menuType)) {
            throw badRequest("menuType은 GROUP, SCREEN, POPUP 중 하나여야 합니다.");
        }
        if (screenId == null || formPath == null) {
            throw badRequest("SCREEN과 POPUP 메뉴에는 screenId와 formPath가 필요합니다.");
        }
    }

    private void assertRevision(
            LocalDateTime currentRevision,
            LocalDateTime expectedRevision
    ) {
        if (!Objects.equals(currentRevision, expectedRevision)) {
            throw conflict("다른 사용자가 먼저 메뉴를 수정했습니다. 다시 조회한 후 재시도하십시오.");
        }
    }

    private void protectSystemMenuStructure(
            MenuAdminResponse current,
            Long requestedParentMenuId,
            String requestedMenuType
    ) {
        if (!PROTECTED_MENU_CODES.contains(current.menuCode())) {
            return;
        }
        if (!Objects.equals(current.parentMenuId(), requestedParentMenuId)
                || !Objects.equals(current.menuType(), requestedMenuType)) {
            throw conflict("시스템 보호 메뉴의 부모 또는 메뉴 유형은 변경할 수 없습니다.");
        }
    }

    private void publishAuthorizationChanged(List<Long> userIds) {
        if (userIds != null && !userIds.isEmpty()) {
            eventPublisher.publishEvent(new AuthorizationChangedEvent(userIds));
        }
    }

    private String actor(AuthSessionContext session) {
        return session.loginId();
    }

    private String nullableTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void requirePositiveId(Long menuId) {
        if (menuId == null || menuId <= 0) {
            throw badRequest("menuId는 양수여야 합니다.");
        }
    }

    private ApiException badRequest(String message) {
        return new ApiException("E302", HttpStatus.BAD_REQUEST, message);
    }

    private ApiException notFound(Long menuId) {
        return new ApiException("E404", HttpStatus.NOT_FOUND, "메뉴를 찾을 수 없습니다. menuId=" + menuId);
    }

    private ApiException conflict(String message) {
        return new ApiException("E409", HttpStatus.CONFLICT, message);
    }
}