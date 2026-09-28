package com.soft2cost2.common.i18n;

import java.util.List;

/** 선택한 언어와 번역 검색 후보를 요청 단위로 보관 한다. */
public record LanguageContext(String selected, List<String> candidates) {
    public LanguageContext { candidates = List.copyOf(candidates); }
}
