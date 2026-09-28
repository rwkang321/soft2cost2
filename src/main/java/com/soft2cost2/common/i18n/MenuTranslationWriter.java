package com.soft2cost2.common.i18n;

/** 기존 메뉴 저장 트랜잭션 안에서 기본 언어 번역을 동기화하는 서비스 계약 */
public interface MenuTranslationWriter {
    void syncDefault(Long menuId, String actor);
}
