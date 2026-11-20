package com.citeolivia.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * The only entry point for password hashing outside the security package. Enforced by ArchUnit.
 */
@Component
public class PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    PasswordHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
