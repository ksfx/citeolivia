package com.citeolivia.user;

import com.citeolivia.security.PasswordHasher;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

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

    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !users.existsByEmail(normalizeEmail(email));
    }

    /**
     * Registers a new user with role USER. Bootstrap rule: the very first user also gets ADMIN, so a
     * fresh installation has an administrator without seeding credentials.
     */
    @Transactional
    public long register(RegistrationCommand command) {
        String email = normalizeEmail(command.email());
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
        boolean firstUser = users.count() == 0;

        AppUser user =
                AppUser.register(email, command.displayName().trim(), passwordHasher.hash(command.rawPassword()));
        user.grant(role(Role.USER));
        if (firstUser) {
            user.grant(role(Role.ADMIN));
        }
        try {
            return users.save(user).getId();
        } catch (DuplicateKeyException e) {
            throw new EmailAlreadyRegisteredException(email);
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
