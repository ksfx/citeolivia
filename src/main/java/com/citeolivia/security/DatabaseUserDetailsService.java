package com.citeolivia.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads credentials and authorities via explicit SQL. Authorities are {@code ROLE_<role>} plus all
 * permission names granted through the user's roles.
 */
@Service
class DatabaseUserDetailsService implements UserDetailsService {

    private final JdbcClient jdbc;

    DatabaseUserDetailsService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        String email = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);

        record Credentials(long id, String email, String displayName, String passwordHash, boolean enabled) {}

        Credentials credentials = jdbc.sql("""
                        SELECT id, email, display_name, password_hash, enabled
                        FROM app_user WHERE email = :email
                        """)
                .param("email", email)
                .query((rs, n) -> new Credentials(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("display_name"),
                        rs.getString("password_hash"),
                        rs.getBoolean("enabled")))
                .optional()
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));

        List<GrantedAuthority> authorities = new ArrayList<>();
        jdbc.sql("""
                        SELECT 'ROLE_' || r.name FROM user_role ur JOIN role r ON r.id = ur.role_id
                        WHERE ur.user_id = :id
                        UNION
                        SELECT p.name FROM user_role ur
                          JOIN role_permission rp ON rp.role_id = ur.role_id
                          JOIN permission p ON p.id = rp.permission_id
                        WHERE ur.user_id = :id
                        """)
                .param("id", credentials.id())
                .query(String.class)
                .list()
                .forEach(a -> authorities.add(new SimpleGrantedAuthority(a)));

        return new AppUserPrincipal(
                credentials.id(),
                credentials.email(),
                credentials.displayName(),
                credentials.passwordHash(),
                credentials.enabled(),
                authorities);
    }
}
