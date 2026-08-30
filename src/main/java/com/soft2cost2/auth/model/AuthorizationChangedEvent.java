package com.soft2cost2.auth.model;

import java.util.List;
import java.util.Objects;

public record AuthorizationChangedEvent(List<Long> userIds) {
    public AuthorizationChangedEvent {
        userIds = userIds == null ? List.of() : userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
