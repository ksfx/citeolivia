package com.citeolivia.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Principal for form login. The password hash is erased after authentication (not stored in the session). */
final class AppUserPrincipal implements UserDetails, CredentialsContainer, CurrentUser {

    private final long userId;
    private final String email;
    private final String displayName;
    private String passwordHash;
    private final boolean enabled;
    private final List<GrantedAuthority> authorities;

    AppUserPrincipal(
            long userId,
            String email,
            String displayName,
            String passwordHash,
            boolean enabled,
            List<GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.authorities = List.copyOf(authorities);
    }

    @Override
    public long userId() {
        return userId;
    }

    @Override
    public String email() {
        return email;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}
