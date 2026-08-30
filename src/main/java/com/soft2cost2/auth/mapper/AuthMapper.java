/** 2026.08.13 AuthMapper.java by rwkang.
 * 1. AuthMapper.xml 과 매칭 되는 파일
 *
 */

/** 2026.08.17 Updated by cdx.
 * 변경 이유:
 * 서비스와 SQL의 행 수 계약을 int로 확인하고, 실패 증가와 잠금을 원자적 SQL 하나로 합친다.
 */

package com.soft2cost2.auth.mapper;

import com.soft2cost2.auth.model.MenuRow;
import com.soft2cost2.auth.model.RoleMenuAuthRow;
import com.soft2cost2.auth.model.UserAccount;
import com.soft2cost2.auth.model.UserMenuAuthRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuthMapper {

    UserAccount selectUserByLoginId(
            @Param("loginId") String loginId
    );

    List<String> selectRoleCodes(
            @Param("userId") Long userId
    );

    List<MenuRow> selectAllMenus();

    List<RoleMenuAuthRow> selectRoleMenuAuth(
            @Param("userId") Long userid
    );

    List<UserMenuAuthRow> selectUserMenuAuth(
            @Param("userId") Long userId
    );

    /**
    void increaseLoginFailure( @Param("userId") Long userId );
    void lockUser( @Param("userId") Long userId );
    void resetLoginFailure( @Param("userId") Long userId );
     */

    // cdx added
    int unlockExpiredUser(@Param("loginId") String loginId);

    // cdx added
    int recordLoginFailure(
            @Param("userId") Long userId,
            @Param("threshold") int threshold,
            @Param("lockMinutes") int lockMinutes
    );

    // cdx added
    int recordLoginSuccess(@Param("userId") Long userId);

    // cdx added
    int updatePassword(
            @Param("userId") Long userId,
            @Param("passwordHash") String passwordHash,
            @Param("updatedBy") String updatedBy
    );



}
