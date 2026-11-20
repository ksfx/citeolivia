package com.citeolivia.library.citation;

import com.citeolivia.library.Contributor;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Shared helpers for citation styles. */
final class CitationSupport {

    private static final Pattern DIGITS = Pattern.compile("\\d+");
    private static final Pattern RANGE = Pattern.compile("(\\w)\\s*-{1,2}\\s*(\\w)");

    private CitationSupport() {}

    static boolean has(String s) {
        return s != null && !s.isBlank();
    }

    /** "12-34" -> "12–34" (en dash). */
    static String pages(String pages) {
        return has(pages) ? RANGE.matcher(pages.strip()).replaceAll("$1–$2") : pages;
    }

    /** "2" -> "2. Aufl."; free text is kept. */
    static String edition(String edition) {
        if (!has(edition)) {
            return null;
        }
        String e = edition.strip();
        if (DIGITS.matcher(e).matches()) {
            return e + ". Aufl.";
        }
        return e.toLowerCase().contains("aufl") ? e : e + " Aufl.";
    }

    /** "Anna Maria" -> "A. M.", "Hans-Peter" -> "H.-P." */
    static String initials(String firstNames) {
        if (!has(firstNames)) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (String name : firstNames.strip().split("\\s+")) {
            String[] hyphenated = name.split("-");
            List<String> initials = new ArrayList<>();
            for (String h : hyphenated) {
                if (!h.isEmpty()) {
                    initials.add(h.endsWith(".") && h.length() <= 3 ? h : h.charAt(0) + ".");
                }
            }
            parts.add(String.join("-", initials));
        }
        return String.join(" ", parts);
    }

    static String fullTitle(String title, String subtitle) {
        if (!has(subtitle)) {
            return title;
        }
        String t = title.strip();
        return t.endsWith(":") || t.endsWith("?") || t.endsWith("!") || t.endsWith(".")
                ? t + " " + subtitle.strip()
                : t + ": " + subtitle.strip();
    }

    static String doiUrl(String doi) {
        if (!has(doi)) {
            return null;
        }
        String d = doi.strip();
        return d.startsWith("http") ? d : "https://doi.org/" + d;
    }

    static String lastNames(List<Contributor> people, String twoSeparator, String etAl) {
        return switch (people.size()) {
            case 0 -> "";
            case 1 -> people.get(0).lastName();
            case 2 -> people.get(0).lastName() + twoSeparator + people.get(1).lastName();
            default -> people.get(0).lastName() + " " + etAl;
        };
    }
}
