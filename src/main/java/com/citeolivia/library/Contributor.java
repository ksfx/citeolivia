package com.citeolivia.library;

import org.springframework.data.relational.core.mapping.Table;

/**
 * Author or editor of a reference. Organisations have only a {@code lastName} (e.g. "Statistisches
 * Bundesamt").
 */
@Table("reference_contributor")
public record Contributor(ContributorRole role, String lastName, String firstName) {

    public static Contributor author(String lastName, String firstName) {
        return new Contributor(ContributorRole.AUTHOR, lastName, firstName);
    }

    public static Contributor editor(String lastName, String firstName) {
        return new Contributor(ContributorRole.EDITOR, lastName, firstName);
    }

    public boolean hasFirstName() {
        return firstName != null && !firstName.isBlank();
    }

    /** "Nachname, Vorname" – the input format of the forms. */
    public String displayName() {
        return hasFirstName() ? lastName + ", " + firstName : lastName;
    }
}
