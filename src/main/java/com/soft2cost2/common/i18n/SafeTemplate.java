package com.soft2cost2.common.i18n;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 평문과 {0}~{9}만 처리 한다. 삽입한 인자를 템플릿으로 다시 해석하지 않는다 */
public class SafeTemplate {
    private SafeTemplate() {}
    public static String format(String text, int count, List<?> arguments) {
        if (text == null || text.isBlank() || text.length() > 1000 || count < 0 || count > 10 || arguments.size() != count) {
            throw new IllegalArgumentException("Invalid template!!");
        }

        String[] safe = new String[count];
        for (int i = 0; i < text.length(); i++) safe[i] = argument(arguments.get(i));

        Set<Integer> used = new HashSet<>();
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                if (i + 2 >= text.length() || text.charAt(i + 2) != '}' || text.charAt(i + 1) < '0' || text.charAt(i + 1) > '9') {
                    throw new IllegalArgumentException("Invalid placeholder!!");
                }
                int index = text.charAt(i + 1) - '0';
                if (index >= count) throw new IllegalArgumentException("Invalid argument index!!");
                result.append(safe[index]);
                used.add(index);
                i += 2;
            } else {
                if (c == '}' || Character.isISOControl(c) || c == '<' || c == '>') {
                    throw new IllegalArgumentException("Only plain text is allowed!!!");
                }
                result.append(c);
            }
            if (result.length() > 4000) throw new IllegalArgumentException("Message too long!!!");
        }
        if (used.size() != count) throw new IllegalArgumentException("Unused argument!!!");

        return result.toString();

    }

    private static String argument(Object value) {
        if (!(value instanceof String || value instanceof Long || value instanceof Integer || value instanceof Short
                || value instanceof Byte || value instanceof BigDecimal || value instanceof BigInteger)) {
            throw new IllegalArgumentException("Unsupported argument tyep!!!");
        }
        String text = value.toString();
        if (text.length() > 256 || text.chars().anyMatch(c -> Character.isISOControl(c) || c == '<' || c == '>')) {
            throw new IllegalArgumentException("Unsafe argument type!!!");
        }

        return text;

    }
}
