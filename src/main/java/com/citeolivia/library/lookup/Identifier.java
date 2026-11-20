package com.citeolivia.library.lookup;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A DOI or ISBN recognised in user input (also within URLs like https://doi.org/...). */
public record Identifier(Kind kind, String value) {

    public enum Kind {
        DOI,
        ISBN
    }

    private static final Pattern DOI = Pattern.compile("(10\\.\\d{4,9}/\\S+)");

    public static Optional<Identifier> parse(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String s = input.strip();
        Matcher doi = DOI.matcher(s);
        if (doi.find()) {
            return Optional.of(new Identifier(Kind.DOI, doi.group(1).replaceAll("[.,;]+$", "")));
        }
        String digits =
                s.replaceAll("(?i)^isbn[:\\s]*", "").replaceAll("[\\s-]", "").toUpperCase();
        if (digits.matches("\\d{13}") && isbn13Valid(digits)) {
            return Optional.of(new Identifier(Kind.ISBN, digits));
        }
        if (digits.matches("\\d{9}[\\dX]") && isbn10Valid(digits)) {
            return Optional.of(new Identifier(Kind.ISBN, digits));
        }
        return Optional.empty();
    }

    static boolean isbn13Valid(String isbn) {
        int sum = 0;
        for (int i = 0; i < 13; i++) {
            sum += (isbn.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return sum % 10 == 0;
    }

    static boolean isbn10Valid(String isbn) {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            char c = isbn.charAt(i);
            int v = c == 'X' ? 10 : c - '0';
            sum += v * (10 - i);
        }
        return sum % 11 == 0;
    }
}
