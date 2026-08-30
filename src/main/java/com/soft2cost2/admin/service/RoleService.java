/** 2026.08.15 UserService.java by rwkang.
 * 1. 최초 1회만, "ADMIN", "시스템 관리자" 등록을 위해.
 * 2. todo: "비밀번호"는 절대 평문을 넣지 않고, "PasswordEncoder"로 단방향 해시 값으로 저장.
 * *. todo: Spring Security PasswordEncoder.encode() and matches
 */

// 2026.08.19 Updated by cdx.
// 변경 이유:
// 새 역할을 만든 후 실제 roleId를 재조회 한다.
// select-then-insert 경합을 Oracle MERGE로 줄인다.
// SYSTEM_ADMIN에 모든 활성 메뉴의 명시적 권한을 부여한다. 숨은 bypass 코드를 두는 것보다 감사 가능하다.
//
//보안 영향:
// 신규 SYSTEM_ADMIN 생성 branch의 null roleId 위험과 “역할은 있지만 메뉴가 없는 관리자” 가능성을 함께 해결한다.
// 사용자 확인은 기존 ADMIN branch였으므로 신규 branch 검증이 별도로 필요하다.
// 새 메뉴를 추가할 때 SYSTEM_ADMIN 권한을 자동 부여할지 승인 워크플로를 거칠지 정책 결정이 필요하다.
// 보안 민감 메뉴는 자동 전체 권한보다 명시적 승인 방식이 안전하다.

package com.soft2cost2.admin.service;

import com.soft2cost2.admin.mapper.RoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {

    public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";
    public static final String BOOTSTRAP = "BOOTSTRAP";

    private final RoleMapper roleMapper;

    public RoleService(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    //? private 메서드 annotation은 Spring proxy 경계를 만들지 않는다. 현재 facade transaction이 있으므로 새 transaction helper는 필요 없다.
    //@Transactional
    private void ensureProtectedMenuPermission(Long roleId, String menuCode, String authLevel) {
        int affected = roleMapper.mergeSystemAdminMenuPermissions(roleId, menuCode, authLevel, BOOTSTRAP);
        if (affected != 1) {
            throw new IllegalArgumentException("활성 보호 메뉴 권한 보장 실패!!! menuCode=" + menuCode);
        }
    }


    public void requireBootstrapMenuStructure() {
        if (roleMapper.countValidBootstrapMenuStructure() != 1) {
            throw new IllegalStateException(
                    "Bootstrap 보호 메뉴가 없거나 활성·부모·type·level 구조가 잘못됐습니다."
            );
        }
    }

    @Transactional
    public Long ensureSystemAdminRole() {
        roleMapper.mergeSystemAdminRole(
                SYSTEM_ADMIN, "시스템관리자",
                "Soft2Cost 시스템 전체 관리자", BOOTSTRAP
        );
        Long roleId = roleMapper.selectRoleIdByCode(SYSTEM_ADMIN);
        if (roleId == null || roleMapper.countValidSystemAdminRole(roleId) != 1) {
            throw new IllegalStateException(
                    "기존 SYSTEM_ADMIN 역할에 type/use drift가 있습니다. 자동 덮어쓰지 않습니다."
            );
        }
        return roleId;
    }

    @Transactional
    public void ensureAssignmentAndPermissions(Long userId, Long roleId) {
        requireId(userId, "userId");
        requireId(roleId, "roleId");
        roleMapper.mergeUserRole(
                userId, roleId, "최초 시스템 관리자 bootstrap", BOOTSTRAP
        );
        if (roleMapper.countCurrentUserRole(userId, roleId) != 1) {
            throw new IllegalStateException(
                    "현재 유효한 ADMIN-SYSTEM_ADMIN 연결이 정확히 1건이 아닙니다."
            );
        }
        ensureProtectedMenuPermission(roleId, "SYS_ADMIN_GRP", "R");
        ensureProtectedMenuPermission(roleId, "SYS_MENU_MGMT", "W");
    }

    public void requireNoBlockingAdminOverrides(Long userId) {
        if (roleMapper.countBlockingAdminMenuOverrides(userId) != 0) {
            throw new IllegalStateException(
                    "ADMIN의 보호 메뉴 역할 권한을 낮추는 사용자 예외가 있습니다. 자동 삭제하지 않습니다."
            );
        }
    }

    private void requireId(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be a positive value.");
        }
    }

}
