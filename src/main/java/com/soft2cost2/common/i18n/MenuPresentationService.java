package com.soft2cost2.common.i18n;

import com.soft2cost2.auth.model.MenuPermission;

import java.util.List;
import java.util.Map;

/** 기존 권한 목록을 보존하면서 메뉴 번역과 아이콘 표시 값을 제공 한다. */
public interface MenuPresentationService {
    List<Map<String, Object>> present(List<MenuPermission> permissions, LanguageContext context, boolean extended);

}
