package com.soft2cost2.common.i18n;

/** 다국어 메시지 조회와 장애 시 안전 응답을 제공하는 서비스 계약 */
public interface MessageService {
     /** 번역문, 실제 적용 언어, 내장 안전 문구 사용 여부를 함께 반환 */
     record Resolved(String text, String language, boolean safety) {}

     Resolved resolved(String apiCode, MessageRef reference, LanguageContext context);
     Resolved safetyFallback(String code, String selected);
}
