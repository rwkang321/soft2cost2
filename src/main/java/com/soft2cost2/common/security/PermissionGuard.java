/** 2026.08.19 PermissionGuard.java by cdx.
 * 인증된 사용자 여부만 검사한다.
 * authLevel, canExcel, canApprove, canClose, canCostView,
 * dataScope를 사용하는 업무 API 코드가 없다.
 */
/**
 * 변경 이유:
 * 메뉴 표시 권한과 서버 실행 권한을 분리하지 않고 같은 Redis 스냅샷에서 일관되게 검사한다.
 * 직접 URL 호출, 변조된 Nexacro 요청, 화면 숨김 우회를 서버에서 차단한다.
 * 보안 영향:
 * Controller annotation만으로 끝내지 말고 Service 진입점에서도 권한 검사를 유지해야 한다. 내부 호출이나 향후 다른 transport가 Controller 검사를 우회할 수 있기 때문이다.
 * MENU_CODE를 안정적인 권한 resource key로 쓸지 별도 PERMISSION_CODE를 둘지 확정해야 한다.
 *
 */

package com.soft2cost2.common.security;

import com.soft2cost2.auth.model.AuthSessionContext;
import com.soft2cost2.auth.model.MenuPermission;
import com.soft2cost2.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component("permissionGuard")
public class PermissionGuard {

    public boolean has(
            Authentication authentication,
            String menuCode,
            String requiredLevel
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthSessionContext session)) {
            return false;
        }
        if (!Set.of("R", "W").contains(requiredLevel)) { return false; }
        return session.menus().stream()
                .filter(permission -> menuCode.equals(permission.menuCode()))
                .anyMatch(permission -> rank(permission.authLevel()) >= rank(requiredLevel));
        /**
        try {
            return session.menus().stream()
                    .filter(permission -> menuCode.equals(permission.menuCode()))
                    .findFirst()
                    .map(permission -> rank(permission.authLevel()) >= rank(requiredLevel))
                    .orElse(false);
        } catch (IllegalAccessError exception) {
            return false;
        }
         */

    }

    public MenuPermission require(
            AuthSessionContext session,
            String menuCode,
            String requiredLevel
    ) {

        if (session == null) {
            throw denied();
        }

        MenuPermission permission = session.menus().stream()
                .filter(p -> menuCode.equals(p.menuCode()))
                .findFirst()
                .orElseThrow(() -> denied());

        if (rank(permission.authLevel()) < rank(requiredLevel)) { throw denied(); }

        return permission;

    }

    public void requireFeature(MenuPermission permission, String feature) {
        boolean allowed = switch (feature) {
            case "EXCEL" -> "Y".equals(permission.canExcel());
            case "APPROVE" -> "Y".equals(permission.canApprove());
            case "CLOSE" -> "Y".equals(permission.canClose());
            case "COST_VIEW" -> "Y".equals(permission.canCostView());
            default -> false;
        };
        if (!allowed) { throw denied(); }
    }

    public void requireDataScope(
            MenuPermission permission,
            AuthSessionContext session,
            Long resourceCompanyId,
            Long resourceSiteId,
            Long resourceDeptId,
            Long resourceOwnerUserId
    ) {
        boolean allowed = switch (permission.dataScope()) {
            case "OWN" ->
                    Objects.equals(
                            session.userId(),
                            resourceOwnerUserId
                    );
            case "DEPT" ->
                    Objects.equals(
                            session.companyId(),
                            resourceCompanyId
                    )
                            && session.deptId() != null
                            && Objects.equals(
                            session.deptId(),
                            resourceDeptId
                    );
            case "SITE" ->
                    Objects.equals(
                            session.companyId(),
                            resourceCompanyId
                    )
                            && session.siteId() != null
                            && Objects.equals(
                            session.siteId(),
                            resourceSiteId
                    );
            case "COMPANY" ->
                    Objects.equals(
                            session.companyId(),
                            resourceCompanyId
                    );
            default -> false;
        };
        if (!allowed) {
            throw denied();
        }
    }

    private int rank(String authLevel) {
        return switch (authLevel) {
            case "N" -> 0;
            case "R" -> 1;
            case "W" -> 2;
            default -> throw new IllegalStateException("Unknown auth level!!!");
        };
    }

    private ApiException denied() {
        return new ApiException(
                "E304",
                HttpStatus.FORBIDDEN,
                "요청한 기능에 대한 권한이 없습니다!!!"
        );
    }

}
