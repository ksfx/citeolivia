package com.citeolivia.user;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/** User aggregate. Roles are a separate aggregate and referenced by id only. */
@Table("app_user")
public class AppUser {

    @Id
    private Long id;

    private String email;
    private String displayName;
    private String passwordHash;
    private boolean enabled;
    private Instant createdAt;

    @MappedCollection(idColumn = "user_id")
    private Set<UserRole> roles = new HashSet<>();

    @PersistenceCreator
    AppUser(
            Long id,
            String email,
            String displayName,
            String passwordHash,
            boolean enabled,
            Instant createdAt,
            Set<UserRole> roles) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.roles = roles == null ? new HashSet<>() : new HashSet<>(roles);
    }

    static AppUser register(String email, String displayName, String passwordHash) {
        return new AppUser(null, email, displayName, passwordHash, true, Instant.now(), Set.of());
    }

    void grant(Role role) {
        roles.add(new UserRole(AggregateReference.to(role.getId())));
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    Set<UserRole> getRoles() {
        return Set.copyOf(roles);
    }
}
