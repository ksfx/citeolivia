package com.citeolivia.security;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Authorization checks for the literature management, used in {@code @PreAuthorize} expressions on
 * service methods. A project is only accessible for its owner.
 */
@Component("projectAccess")
public class ProjectAccess {

    /** Service methods with a {@code projectId} parameter. */
    public static final String PROJECT = "hasAuthority('library:use') and @projectAccess.canAccess(#projectId)";

    /** Service methods with an {@code ownerId} parameter. */
    public static final String OWNER = "hasAuthority('library:use') and @projectAccess.isCurrentUser(#ownerId)";

    private final JdbcClient jdbc;

    ProjectAccess(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean canAccess(Long projectId) {
        Long userId = currentUserId();
        if (projectId == null || userId == null) {
            return false;
        }
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM project WHERE id = :projectId AND owner_id = :userId)")
                .param("projectId", projectId)
                .param("userId", userId)
                .query(Boolean.class)
                .single();
    }

    public boolean isCurrentUser(Long ownerId) {
        Long userId = currentUserId();
        return userId != null && userId.equals(ownerId);
    }

    private static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CurrentUser user) {
            return user.userId();
        }
        return null;
    }
}
