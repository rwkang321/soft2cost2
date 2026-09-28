package com.soft2cost2.common.i18n;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

/** 요청 언어 선태과 인증 후 사용자 선호 적용을 제공하는 서비스 계약 */
public interface LanguageService {
    void begin(HttpServletRequest request);
    void authenticated(HttpServletRequest request, Long userId);
    LanguageContext context(HttpServletRequest request);
    LanguageContext choose(String explicit, String preference, String accept, Set<String> active);
    String normalize(String raw);

}
