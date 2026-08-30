package com.soft2cost2.menu.mapper;

import com.soft2cost2.menu.dto.MenuAdminResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MenuAdminMapper {

    List<MenuAdminResponse> selectAll();

    MenuAdminResponse selectById(@Param("menuId") Long menuId);

    MenuAdminResponse selectByIdForUpdate(@Param("menuId") Long menuId);

    MenuAdminResponse selectByCode(@Param("menuCode") String menuCode);

    List<MenuAdminResponse> selectSubtreeForUpdate(@Param("menuId") Long menuId);

    int countMenuCode(
            @Param("menuCode") String menuCode,
            @Param("excludedMenuId") Long excludedMenuId
    );

    int countInSubtree(
            @Param("rootMenuId") Long rootMenuId,
            @Param("candidateMenuId") Long candidateMenuId
    );

    List<Long> selectAffectedUserIds(@Param("rootMenuId") Long rootMenuId);

    int insert(
            @Param("parentMenuId") Long parentMenuId,
            @Param("menuCode") String menuCode,
            @Param("menuName") String menuName,
            @Param("menuLevel") Integer menuLevel,
            @Param("menuType") String menuType,
            @Param("screenId") String screenId,
            @Param("formPath") String formPath,
            @Param("iconName") String iconName,
            @Param("sortOrder") Integer sortOrder,
            @Param("sensitiveYn") String sensitiveYn,
            @Param("actor") String actor
    );

    int updateRoot(
            @Param("menuId") Long menuId,
            @Param("parentMenuId") Long parentMenuId,
            @Param("menuName") String menuName,
            @Param("menuLevel") Integer menuLevel,
            @Param("menuType") String menuType,
            @Param("screenId") String screenId,
            @Param("formPath") String formPath,
            @Param("iconName") String iconName,
            @Param("sortOrder") Integer sortOrder,
            @Param("sensitiveYn") String sensitiveYn,
            @Param("actor") String actor
    );

    int shiftDescendantLevels(
            @Param("menuId") Long menuId,
            @Param("levelDelta") Integer levelDelta,
            @Param("actor") String actor
    );

    int disableSubtree(
            @Param("menuId") Long menuId,
            @Param("actor") String actor
    );



}
