/** 2026.08.15 UserService.java by rwkang.
 * 1. 최초 1회만,
 * todo: S2C_USER 테이블에, "ADMIN", "시스템 관리자" 등록을 위해.
 * todo: S2C_USER_ROLE 테이블에, "ADMIN", "SYSTEM_ADMIN"
 *
 */

package com.soft2cost2.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    Long selectUserIdByLoginId(@Param("loginId") String loginId);

    Long selectCompanyIdByCode(@Param("companyCode") String companyCode);

//    int insertInitialAdmin(
//            @Param("loginId") String loginId,
//            @Param("userName") String userName,
//            @Param("companyId") Long companyId,
//            @Param("passwordHash") String passwordHash,
//            @Param("createdBy") String createdBy
//    );

    int mergeInitialAdmin(
            @Param("loginId") String loginId,
            @Param("userName") String userName,
            @Param("companyId") Long companyId,
            @Param("passwordHash") String passwordHash,
            @Param("createdBy") String createdBy
    );

    int countCompanyByCode(@Param("companyCode") String companyCode);

    int countActiveCompanyByCode(@Param("companyCode") String companyCode);

    int countConformingInitialAdmin(
            @Param("userId") Long userId,
            @Param("loginId") String loginId,
            @Param("companyCode") String companyCode
    );

}
