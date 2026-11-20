package com.citeolivia.user;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Read model for user lists – never exposes the password hash. */
public record UserSummary(Long id, String email, String displayName, boolean enabled, Instant createdAt, String roles) {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public String createdAtFormatted() {
        return createdAt == null ? "" : FORMAT.format(createdAt.atZone(ZoneId.systemDefault()));
    }
}
