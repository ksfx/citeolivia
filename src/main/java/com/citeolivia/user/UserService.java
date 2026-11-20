package com.citeolivia.user;

import com.citeolivia.security.PasswordHasher;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordHasher passwordHasher;

    UserService(UserRepository users, RoleRepository roles, PasswordHasher passwordHasher) {
        this.users = users;
        this.roles = roles;
        this.passwordHasher = passwordHasher;
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Registers a new user with role USER. Bootstrap rule: the very first user also gets ADMIN, so a
     * fresh installation has an administrator without seeding credentials.
     *
     * <p>For an already registered address nothing is created or changed. The password is hashed in
     * both cases, so the response time does not differ.
     *
     * @return the id of the new user, or empty if the address was already registered
     */
    @Transactional
    public Optional<Long> register(RegistrationCommand command) {
        String email = normalizeEmail(command.email());
        String passwordHash = passwordHasher.hash(command.rawPassword()); // always – constant timing
        if (users.existsByEmail(email)) {
            log.info("Registration for an already registered address ignored");
            return Optional.empty();
        }
        boolean firstUser = users.count() == 0;

        AppUser user = AppUser.register(email, command.displayName().trim(), passwordHash);
        user.grant(role(Role.USER));
        if (firstUser) {
            user.grant(role(Role.ADMIN));
        }
        try {
            return Optional.of(users.save(user).getId());
        } catch (DuplicateKeyException e) {
            // registered concurrently – same silent outcome
            log.info("Registration for an already registered address ignored");
            return Optional.empty();
        }
    }

    @PreAuthorize("hasAuthority('users:read')")
    @Transactional(readOnly = true)
    public List<UserSummary> listUsers() {
        return users.findAllSummaries();
    }

    private Role role(String name) {
        return roles.findByName(name).orElseThrow(() -> new IllegalStateException("Missing role " + name));
    }
}
