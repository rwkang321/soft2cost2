package com.soft2cost2.common.i18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service("messageService")
public class MessageServiceImpl implements MessageService{
    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);
    private static final Map<String, String> DEFAULT_CODES = Map.ofEntries(
            Map.entry("E301", "MSG_LOGIN_FAILED"),
            Map.entry("E302", "MSG_REQUEST_INVALID"),
            Map.entry("E303", "MSG_SESSION_INVALID"),
            Map.entry("E304", "MSG_ACCESS_DENIED"),
            Map.entry("E305", "MSG_PASSWORD_CHANGE_REQUIRED"),
            Map.entry("E306", "MSG_ACCOUNT_LOCKED"),
            Map.entry("E307", "MSG_PASSWORD_POLICY"),
            Map.entry("E404", "MSG_NOT_FOUND"),
            Map.entry("E409", "MSG_CONFLICT"),
            Map.entry("E500", "MSG_SERVER_ERROR"),
            Map.entry("E503", "MSG_SERVICE_UNAVAILABLE"));

    private final I18nService repository;
    public MessageServiceImpl(I18nService repository) { this.repository = repository;}

    @Override
    public Resolved resolved(String apiCode, MessageRef reference, LanguageContext context) {
        try {
            Resolved result = lookup(apiCode, reference, context);

            if (result != null) return result;

            // 세부 메시지 실패 뒤에만 명시적인 일반 메시지로 한 번 대체
            String fallback = DEFAULT_CODES.get(apiCode);
            if (fallback != null && (reference == null || reference.kind() != MessageRef.Kind.CODE
            || !fallback.equals(reference.value()))) {
                result = lookup(apiCode, MessageRef.code(fallback), context);
                if (result != null) return result;
            }

        } catch (RuntimeException failure) {
            log.warn("I18n message resolution failed: {}", failure.getClass().getSimpleName());
        }
        return safetyFallback(apiCode, context.selected());
    }

    private Resolved lookup(String apiCode, MessageRef reference, LanguageContext context) {
        if (reference == null) return null;

        I18nRows.Message master = repository.message(reference);
        if (master == null || !"Y".equals(master.useYn())
                || !apiCode.equals(master.apiCode()) || master.argCount() == null) return null;

        List<I18nRows.Text> rows = repository.texts(master.messageCode(), context.candidates());
        for (String language : context.candidates()) {
            for (I18nRows.Text row : rows) {
                if (!language.equals(row.langCode())) continue;
                try {
                    return new Resolved(SafeTemplate.format(row.messageText(), master.argCount(),
                            reference.arguments()), language, false);
                } catch (IllegalArgumentException invalid) {
                    log.warn("Invalid i18n template: {} / {}", master.messageCode(), language);
                }
            }
        }
        return null;
    }

    /** DB and Redis가 모두 불능이어도 기존 code/HTTP 상태를 보존할 최소 문구 */
    @Override
    public Resolved safetyFallback(String code, String selected) {
        boolean en = selected != null && (selected.equals("en") || selected.equals("en-"));
        String text;
        if ("E503".equals(code)) {
            text = en ? "The service is temporarily unavailable." : "서비스를 일시적으로 사용활 수 없습니다!!!";
        } else if ("E500".equals(code)) {
            text = en ? "A server error has occurred" : "서버 율가 발생";
        } else if ("E301".equals(code) || "E303".equals(code)) {
            text = en ? "Authentication is required" : "인증이 필요합니다!!!";
        } else if ("E304".equals(code)) {
            text = en ? "Access is denied" : "접근 권한이 없습니다!!!";
        } else {
            text = en ? "The request could not be processed" : "요청을 처리할 수 없습니다!!!";
        }
        return new Resolved(text, en ? "en" : "ko", true);
    }
}
