/** 2026.08.13 PermissionService.java by rwkang.
 * 1. 권한 계산 ***** 엄청 중요 *****
 * todo: 여기서 "권한 규칙" 구현.
 * N < R < W
 * OWN < DEPT < SITE < COMPANY
 * CAN_EXCEL
 * CAN_APPROVE
 * CAN_CLOSE
 * CAN_COST_VIEW
 *
 * Role 권한
 * ↓
 * 사용자 예외 권한으로 덮어쓰기
 * todo: 특히 "사용자 예외의 NULL은 역할 권한 상속이다.
 */

/** 2026.08.17 Updated by cdx.
 * 변경 이유:
 * AUTH_LEVEL에는 maxAuth를 사용하고 Y/N 기능 플래그에만 maxFlag를 사용해야 한다.
 * 사용자 예외의 NULL 상속과 명시적 N/R/W 덮어쓰기를 보존한다.
 * DB 제약이 우회되거나 잘못된 문자열이 들어와도 넓은 권한으로 오인하지 않도록 fail-closed 한다.
 * 부모 순환과 고아 부모를 로그인 시점에 즉시 발견한다.
 * 보안 영향:
 * 정적 코드상 R/W가 N으로 남는 합산 위험을 해결한다. 실제 메뉴 응답 회귀 테스트가 필요하다.
 * 여러 역할의 W, 기능 플래그 Y, 가장 넓은 DATA_SCOPE를 의도대로 합산한다.
 * 부모 메뉴는 탐색용으로만 표시되며 부모 자체 권한은 기본 N이다. 업무 API는 별도 PermissionGuard를 반드시 사용해야 한다.
 */

package com.soft2cost2.auth.service;

import com.soft2cost2.auth.model.MenuPermission;
import com.soft2cost2.auth.model.MenuRow;
import com.soft2cost2.auth.model.RoleMenuAuthRow;
import com.soft2cost2.auth.model.UserMenuAuthRow;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    public List<MenuPermission> calculate(
            List<MenuRow> menus,
            List<RoleMenuAuthRow> roleAuthList,
            List<UserMenuAuthRow> userAuthList
    ) {
        Map<Long, PermissionState> states = new HashMap<>();

        // cdx added
        Map<Long, MenuRow> menuMap = new HashMap<>();

        // 1. 모든 메뉴 기본 권한 = N
        for (MenuRow menu : menus) {

            // cdx added
            if (menuMap.put(menu.menuId(), menu) != null) {
                throw new IllegalStateException("중복 MENU_ID=" + menu.menuId());
            }

            states.put(menu.menuId(), new PermissionState());
        }

        // 2. 역할 권한 합산
        for (RoleMenuAuthRow auth : roleAuthList) {

            PermissionState state = states.get(auth.menuId());
            if (state == null) {continue;} //? 미 사용 메뉴 권한 행은 결과에서 제외.

            // state.authLevel = maxYn(state.authLevel, auth.authLevel());
            state.authLevel = maxAuth(state.authLevel, requireAuth(auth.authLevel())); // cdx edited
            // state.canExcel = maxYn(state.canExcel, auth.canExcel());
            state.canExcel = maxFlag(state.canExcel, requireFlag(auth.canExcel())); // cdx edited
            // state.canApprove = maxYn(state.canApprove, auth.canApprove());
            state.canApprove = maxFlag(state.canApprove, requireFlag(auth.canApprove())); // cdx edited
            // state.canClose = maxYn(state.canClose, auth.canClose());
            state.canClose = maxFlag(state.canClose, requireFlag(auth.canClose())); // cdx edited
            // state.canCostView = maxYn(state.canCostView, auth.canCostView());
            state.canCostView = maxFlag(state.canCostView, requireFlag(auth.canCostView())); // cdx edited
            // state.dataScope = maxScope(state.dataScope, auth.dataScope());
            state.dataScope = maxScope(state.dataScope, requireScope(auth.dataScope())); // cdx edited

        }

        // 3. 사용자 개별 예외 적용: Mapper 가 메뉴 별 최신 활성 예외 1건만 반환한다는 계약.
        for (UserMenuAuthRow auth : userAuthList) {

            PermissionState state = states.get(auth.menuId());
            if (state == null) { continue; }

            if (auth.authLevel() != null) {state.authLevel = requireAuth(auth.authLevel());}

            if (auth.canExcel() != null) {state.canExcel = requireFlag(auth.canExcel());}

            if (auth.canApprove() != null) {state.canApprove = requireFlag(auth.canApprove());}

            if (auth.canClose() != null) {state.canClose = requireFlag(auth.canClose());}

            if (auth.canCostView() != null) {state.canCostView = requireFlag(auth.canCostView());}

            if (auth.dataScope() != null) {state.dataScope = requireScope(auth.dataScope());}

        }

        // cdx edited ↓

        // 4. 실제 접근 가능한 메뉴
        //Set<Long> visibleIds = new HashSet<>();
        //for (MenuRow menu : menus) {
        //    PermissionState state = states.get(menu.menuId());
        //    if (!"N".equals(state.authLevel)) {visibleIds.add(menu.menuId());}
        //}

        // ↑ cdx edited.
        Set<Long> visibleIds = states.entrySet().stream()
                .filter(e -> !"N".equals(e.getValue().authLevel))
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));

        for (Long leafId : List.copyOf(visibleIds)) {

            Set<Long> path = new HashSet<>();

            MenuRow current = menuMap.get(leafId);

            while (current != null && current.parentMenuId() != null) {

                if (!path.add(current.menuId())) {
                    throw new IllegalStateException("메뉴 부보 순환 MENU_ID=" + current.menuId());
                }

                Long parentId = current.parentMenuId();
                MenuRow parent = menuMap.get(parentId);
                if (parent == null) {
                    throw new IllegalStateException("없는 부모 MENU_ID=" + parentId);
                }

                visibleIds.add(parentId);
                current = parent;

            }
        }

        return menus.stream()
                .filter(menu -> visibleIds.contains(menu.menuId()))
                .sorted(Comparator.comparing(MenuRow::menuLevel)
                        .thenComparing(MenuRow::sortOrder)
                        .thenComparing(MenuRow::menuId))
                .map(menu -> toPermission(menu, states.get(menu.menuId())))
                .toList();

    }

    private String maxAuth(String a, String b) {
        return authRank(b) > authRank(a) ? b : a;
    }

    // cdx added.
    private String maxFlag(String a, String b) {
        return "Y".equals(a) || "Y".equals(b) ? "Y" : "N";
    }

    private String maxScope(String a, String b) {
        return scopeRank(b) > scopeRank(a) ? b : a;
    }

    // cdx added
    private String requireAuth(String value) {
        if (value == null || !Set.of("N", "R", "W").contains(value)) {
            throw new IllegalStateException("잘못된 AUTH_LEVEL=" + value);
        }
        return value;
    }

    // cdx added
    private String requireFlag(String value) {
        if (value == null || !Set.of("N", "Y").contains(value)) {
            throw new IllegalStateException("잘못된 권한 플래그=" + value);
        }
        return value;
    }

    // cdx added
    private String requireScope(String value) {
        if (value == null || !Set.of("OWN", "DEPT", "SITE", "COMPANY").contains(value)) {
            throw new IllegalStateException("잘못된 DATA_SCOPE=" + value);
        }
        return value;
    }

    private int authRank(String value) {
        return switch (value) {
            case "N" -> 0;
            case "W" -> 2;
            case "R" -> 1;
            default -> throw new IllegalStateException("잘못된 AUTH_LEVEL=" + value);
        };
    }

    private int scopeRank(String value) {
        if (value == null) { return 0; }
        return switch (value) {
            case "COMPANY" -> 3;
            case "SITE" -> 2;
            case "DEPT" -> 1;
            default -> 0; // OWN
        };
    }

    // cdx added.
    private MenuPermission toPermission(MenuRow menu, PermissionState state) {
        return new MenuPermission(
                menu.menuId(),
                menu.parentMenuId(),
                menu.menuCode(),
                menu.menuName(),
                menu.menuLevel(),
                menu.menuType(),
                menu.screenId(),
                menu.formPath(),
                menu.iconName(),
                menu.sortOrder(),
                state.authLevel,
                state.canExcel,
                state.canApprove,
                state.canClose,
                state.canCostView,
                state.dataScope
        );
    }

    private static class PermissionState {

        String authLevel = "N";

        String canExcel = "N";
        String canApprove = "N";
        String canClose = "N";
        String canCostView = "N";

        String dataScope = "OWN";

    }
}
