package com.citeolivia.library;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Text format for authors/editors in forms: one person per line as "Nachname, Vorname".
 * Lines without a comma are organisations ("Statistisches Bundesamt").
 */
public final class ContributorLines {

    private ContributorLines() {}

    public static List<Contributor> parse(String text, ContributorRole role) {
        List<Contributor> result = new ArrayList<>();
        if (text == null) {
            return result;
        }
        for (String line : text.split("\\R|;")) {
            String l = line.strip();
            if (l.isEmpty()) {
                continue;
            }
            int comma = l.indexOf(',');
            if (comma > 0) {
                String first = l.substring(comma + 1).strip();
                result.add(new Contributor(role, l.substring(0, comma).strip(), first.isEmpty() ? null : first));
            } else {
                result.add(new Contributor(role, l, null));
            }
        }
        return result;
    }

    public static String format(List<Contributor> people) {
        return people.stream().map(Contributor::displayName).collect(Collectors.joining("\n"));
    }

    /** "Müller, Meier u. a." – compact display for lists. */
    public static String shortNames(List<Contributor> people) {
        return switch (people.size()) {
            case 0 -> "";
            case 1 -> people.get(0).lastName();
            case 2 -> people.get(0).lastName() + ", " + people.get(1).lastName();
            default -> people.get(0).lastName() + ", " + people.get(1).lastName() + " u. a.";
        };
    }
}
