package com.citeolivia.library;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/** Generates BibTeX-compatible citation keys like "mueller2020", "mueller2020a". */
final class CitationKeys {

    private CitationKeys() {}

    static String generate(ReferenceDraft draft, Predicate<String> taken) {
        String base;
        if (!draft.authors().isEmpty()) {
            base = ascii(draft.authors().get(0).lastName());
        } else if (!draft.editors().isEmpty()) {
            base = ascii(draft.editors().get(0).lastName());
        } else {
            base = ascii(
                    draft.getTitle() == null ? "" : draft.getTitle().strip().split("\\s+")[0]);
        }
        if (base.isEmpty()) {
            base = "ref";
        }
        String year = draft.getYear() == null ? "" : draft.getYear().replaceAll("\\D", "");
        String key = base + (year.length() >= 4 ? year.substring(0, 4) : year);
        if (!taken.test(key)) {
            return key;
        }
        for (char suffix = 'a'; suffix <= 'z'; suffix++) {
            if (!taken.test(key + suffix)) {
                return key + suffix;
            }
        }
        int n = 2;
        while (taken.test(key + "-" + n)) {
            n++;
        }
        return key + "-" + n;
    }

    /** "Müller-Lüdenscheidt" -> "muellerluedenscheidt" */
    static String ascii(String s) {
        String replaced = s.toLowerCase(Locale.ROOT)
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("ü", "ue")
                .replace("ß", "ss");
        String noAccents = Normalizer.normalize(replaced, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.replaceAll("[^a-z0-9]", "");
    }

    static boolean isValid(String key) {
        return key != null && key.matches("[A-Za-z0-9_:\\-.]{1,100}");
    }
}
