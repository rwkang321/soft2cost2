package com.soft2cost2.common.i18n;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// List<?> : 1. 모든 타입 수용, 2. 새로운 요소 추가 방지, 3. 제네릭 시스템 안에서 안전하게 다루겠다는 의미
public record MessageRef(Kind kind, String value, List<?> arguments) {
    public enum Kind { CODE, NAME }
    public MessageRef {
        // if (kind == null || value == null || value.length() > 230) {
        if (kind == null || value == null || value.length() > (kind == kind.CODE ? 64 : 120)) {
            throw new IllegalArgumentException("Invalid message identifier");
        }
        // arguments 불면화=immutable. List.copyOf() : 기존 자료 복사 ∴) 불변 자료
        // arguments = arguments == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(arguments)); JDK 9.0↓
         arguments = arguments == null ? List.of() : List.copyOf(arguments); // JDK 10.0↑
    }
    public static MessageRef code(String code, Object... arguments) {
        return new MessageRef(Kind.CODE, code, Arrays.asList(arguments == null ? new Object[0] : arguments));
    }
    public static MessageRef name(String name, Object... arguments) {
         return new MessageRef(Kind.NAME, name, Arrays.asList(arguments == null ? new Object[0] : arguments));
    }
}
