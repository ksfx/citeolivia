package com.citeolivia.security;

/**
 * The authenticated user as seen by the rest of the application – independent of how the user
 * logged in (form login today, OIDC later). Use with {@code @AuthenticationPrincipal CurrentUser}.
 */
public interface CurrentUser {

    long userId();

    String email();

    String displayName();
}
