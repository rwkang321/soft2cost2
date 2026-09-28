package com.soft2cost2.common.i18n;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 언어·메시지·메뉴 및 아이콘 조회. 모든 사용자 값은 바인딩 한다
 */

@Mapper
public interface I18nMapper {
    /**
     * 1. 넥사크로 UI 언어 선택 콤보박스(Combo): 화면 상단이나 환경설정에서 사용자가 선택할 수 있는 언어 목록 드롭다운을 바인딩할 때 사용
     * 2. 다국어 입력 그리드의 동적 컬럼 구성: 다국어 관리 화면을 열 때 시스템이 지원하는 언어 개수만큼.
     *      그리드 컬럼(한국어, 영어, 일본어 등)을 동적으로 생성하거나 탭을 띄울 때 기준 데이터로 사용
     * 3. 다른 메서드들과의 연계: selectLanguages()로 현재 지원 언어 리스트(["ko", "en"])를 먼저 가져온 뒤, 그 결과를
     *      selectTexts(code, languages)나 selectMenuTexts(ids, languages)의 두 번째 파라미터로 넘겨줄 때 기본값으로 활용
     */
    List<String> selectLanguages();

    String selectPreference(@Param("userId") Long userId);
    I18nRows.Message selectMessageByCode(@Param("value") String value);
    I18nRows.Message selectMessageByName(@Param("value") String value);

    /**
     * 다국어 관리·수정 화면:
     * 넥사크로 그리드에서 특정 메시지 코드 하나를 선택하고, 우측 상세 폼이나 팝업에서 한국어/영어/일본어 등 각 언어별 번역본을 나란히 띄워놓고 조회·수정할 때 사용
     * @param code
     * @param languages
     * @return
     */
    List<I18nRows.Text> selectTexts(@Param("code") String code, @Param("languages") List<String> languages);

    int updateTexts(@Param("code") String code, @Param("texts") List<I18nRows.Text> texts);

    List<I18nRows.MenuText> selectMenuTexts(@Param("ids") List<Long> ids, @Param("languages") List<String> languages);

    int updateMenus(@Param("menuId") Long menuId, @Param("menus") List<I18nRows.MenuText> menus);

    List<I18nRows.MenuIcon> selectMenuIcons(@Param("ids") List<Long> ids);

    int syncDefaultMenu(@Param("menuId") Long menuId, @Param("actor") String actor);
}
