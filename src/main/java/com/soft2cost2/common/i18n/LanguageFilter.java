package com.soft2cost2.common.i18n;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** 로그인 본문 검증과 보안 오류 보다 먼저 언어 문맥을 준비하는 Servlet 필터 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class LanguageFilter extends OncePerRequestFilter {
    private final LanguageService languages;
    public LanguageFilter(LanguageService languages) { this.languages = languages; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod()) || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        languages.begin(request);
        chain.doFilter(request, response);
    }

}
