package com.citeolivia.user;

public record RegistrationCommand(String email, String displayName, String rawPassword) {}
