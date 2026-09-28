package com.soft2cost2.common.i18n;

/**
 * MyBatis 가 생성자 매핑으로 읽는 다국어 전용 데이터 모델
 */

public class I18nRows {
    private I18nRows() {}
    public record Message(String messageCode, String messageName, String apiCode, Integer argCount, String useYn) {}
    public record Text(String langCode, String messageText) {}
    public record MenuText(Long menuId, String langCode, String menuName, String description, String iconAltText, String tooltipText) {}
    public record MenuIcon(Long menuId, String resourcePath, String resourceType) {}
}
