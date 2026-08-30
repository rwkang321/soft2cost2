/** 2026.08.15 UserService.java by rwkang.
 * 1. 최초 1회만,
 * todo: S2C_USER 테이블에, "ADMIN", "시스템 관리자" 등록을 위해.
 * todo: S2C_USER_ROLE 테이블에, "ADMIN", "SYSTEM_ADMIN"
 *
 */


package com.soft2cost2.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleMapper {

    int mergeSystemAdminRole(
            @Param("roleCode") String roleCode,
            @Param("roleName") String roleName,
            //@Param("roleType") String roleType,
            @Param("description") String description,
            @Param("createdBy") String createdBy
    );

    Long selectRoleIdByCode(
            @Param("roleCode") String roleCode
    );

    int mergeUserRole(
            @Param("userId") Long userId,
            @Param("roleId") Long roleId,
            @Param("reason") String reason,
            @Param("createdBy") String createdBy
    );

    int mergeSystemAdminMenuPermissions(
            @Param("roleId") Long roleId,
            @Param("menuCode") String menuCode,
            @Param("authLevel") String authLevel,
            @Param("updatedBy") String updatedBy
    );

    List<Long> selectCurrentUserIdsByRoleId(
            @Param("roleId") Long roleId
    );

    int countValidSystemAdminRole(@Param("roleId") Long roleId);
    int countValidBootstrapMenuStructure();
    int countCurrentUserRole(
            @Param("userId") Long userId,
            @Param("roleId") Long roleId
    );
    int countBlockingAdminMenuOverrides(@Param("userId") Long userId);




//    void mergeSystemAdminRole(String systemAdmin, String 시스템관리자, String soft2Cost_시스템_전체_관리자, String bootstrap);
//
//    void mergeUserRole(Long userId, Long roleId, String 최초_시스템_관리자_bootstrap, String bootstrap);
//
//    void mergeSystemAdminMenuPermissions(Long roleId, String bootstrap);
}
