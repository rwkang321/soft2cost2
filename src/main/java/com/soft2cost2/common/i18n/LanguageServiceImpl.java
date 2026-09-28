package com.soft2cost2.common.i18n;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * X-Language > 인증된 사용자 선호 > Accept-Language > ko 순서이다.
 * 오류 직렬화에서는 context()만 호출하므로 새 DB 조회가 일어나지 않는다.
 */

@Service("languageService")
public class LanguageServiceImpl implements LanguageService {
    private static final String CONTEXT = LanguageService.class.getName() + ".context";
    private static final String SUPPORTED = LanguageService.class.getName() + ".supported";
    private static final Set<String> SAFETY = Set.of("ko", "en");
    private final I18nService repository;
    private volatile Set<String> lastGood = SAFETY;
    private volatile long expiresAt;
    public LanguageServiceImpl(I18nService repository) { this.repository = repository; }

    private synchronized Set<String> supported() {
        if (expiresAt != 0L && System.nanoTime() - expiresAt < 0L) return lastGood;
        try {
            Set<String> loaded = new LinkedHashSet<>();
            for (String value : repository.languages()) {
                String normalized = normalize(value);
                if (normalized != null && normalized.equals(value)) loaded.add(value);
            }
            if (!loaded.contains("ko")) throw new I18nService.Unavailable();
            lastGood = Set.copyOf(loaded);
            expiresAt = System.nanoTime() + 60_000_000_000L;
        } catch (RuntimeException failure) {
            // 기본 언어 목록 장애 시 마지막 정상 목록을 사용하며 빠른 반복을 제한한다.
            expiresAt = System.nanoTime() + 5_000_000_000L;
        }
        return lastGood;
    }
    @Override
    public void begin(HttpServletRequest request) {
        Set<String> active = supported();
        request.setAttribute(SUPPORTED, active);
        request.setAttribute(CONTEXT, choose(request.getHeader("X-Language"), null,
                request.getHeader("Accept-Language"), active));
    }
    @Override
    public void authenticated(HttpServletRequest request, Long userId) {
        if (userId == null || userId <= 0) return;
        Set<String> active = active(request);
        String explicit = match(normalize(request.getHeader("X-Language")), active);
        String preference = null;
        if (explicit == null) {
            try { preference = repository.preference(userId); }
            catch (RuntimeException ignored) { } // 인증 결과와 권한은 바꾸지 않는다.
        }
        request.setAttribute(CONTEXT, choose(request.getHeader("X-Language"), preference,
                request.getHeader("Accept-Language"), active));
    }
    @SuppressWarnings("unchecked")
    private Set<String> active(HttpServletRequest request) {
        Object value = request.getAttribute(SUPPORTED);
        return value instanceof Set<?> ? (Set<String>) value : SAFETY;
    }
    @Override
    public LanguageContext context(HttpServletRequest request) {
        Object value = request.getAttribute(CONTEXT);
        if (value instanceof LanguageContext context) return context;
        return choose(request.getHeader("X-Language"), null,
                request.getHeader("Accept-Language"), SAFETY);
    }
    @Override
    public LanguageContext choose(String explicit, String preference,
                                  String accept, Set<String> active) {
        String selected = match(normalize(explicit), active);
        if (selected == null) selected = match(normalize(preference), active);
        if (selected == null && accept != null && accept.length() <= 512) {
            try {
                // Java 표준 파서는 품질 값 내림차순으로 언어 범위를 반환한다.
                for (Locale.LanguageRange range : Locale.LanguageRange.parse(accept)) {
                    if (range.getWeight() <= 0 || range.getRange().contains("*")) continue;
                    selected = match(normalize(range.getRange()), active);
                    if (selected != null) break;
                }
            } catch (IllegalArgumentException ignored) { } // 기본 언어로 계속한다.
        }
        if (selected == null) selected = "ko";
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String current = selected;
        while (current != null) {
            if (active.contains(current)) candidates.add(current);
            int split = current.lastIndexOf('-');
            current = split < 0 ? null : current.substring(0, split);
        }
        candidates.add("ko");
        return new LanguageContext(selected, new ArrayList<>(candidates));
    }
    @Override
    public String normalize(String raw) {
        if (raw == null || raw.length() > 64) return null;
        String value = raw.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        return value.length() <= 35 && value.matches("[a-z]{2,3}(-[a-z0-9]{2,8})*")
                ? value : null;
    }
    private static String match(String value, Set<String> active) {
        while (value != null) {
            if (active.contains(value)) return value;
            int split = value.lastIndexOf('-');
            value = split < 0 ? null : value.substring(0, split);
        }
        return null;
    }
}

