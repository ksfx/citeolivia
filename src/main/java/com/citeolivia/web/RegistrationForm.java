package com.citeolivia.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;

public record RegistrationForm(
        @NotBlank(message = "Bitte E-Mail angeben") @Email(message = "Keine gültige E-Mail-Adresse") @Size(max = 320)
        String email,

        @NotBlank(message = "Bitte Namen angeben") @Size(max = 100, message = "Maximal 100 Zeichen")
        String displayName,

        @NotBlank(message = "Bitte Passwort angeben") @Size(min = 12, max = 128, message = "Mindestens 12 Zeichen")
        String password,

        String passwordConfirm) {

    public RegistrationForm {
        email = email == null ? null : email.strip();
        displayName = displayName == null ? null : displayName.strip();
    }

    public static RegistrationForm empty() {
        return new RegistrationForm("", "", "", "");
    }

    @AssertTrue(message = "Passwörter stimmen nicht überein")
    public boolean isPasswordConfirmed() {
        return Objects.equals(password, passwordConfirm);
    }
}
