package com.citeolivia.library.citation;

import static com.citeolivia.library.citation.CitationSupport.doiUrl;
import static com.citeolivia.library.citation.CitationSupport.edition;
import static com.citeolivia.library.citation.CitationSupport.fullTitle;
import static com.citeolivia.library.citation.CitationSupport.has;
import static com.citeolivia.library.citation.CitationSupport.initials;
import static com.citeolivia.library.citation.CitationSupport.lastNames;
import static com.citeolivia.library.citation.CitationSupport.pages;

import com.citeolivia.library.Contributor;
import com.citeolivia.library.Reference;
import com.citeolivia.library.ReferenceType;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** APA 7th edition, German conventions (Hrsg., S., Aufl., o. J., Abgerufen am). */
final class Apa7Formatter implements CitationFormatter {

    private static final DateTimeFormatter ACCESSED = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN);

    @Override
    public Formatted bibliography(Reference r) {
        Formatted f = new Formatted();
        ReferenceType type = r.getType();
        String title = fullTitle(r.getTitle(), r.getSubtitle());
        boolean italicTitle = type == ReferenceType.BOOK
                || type == ReferenceType.EDITED_BOOK
                || type == ReferenceType.THESIS
                || type == ReferenceType.REPORT
                || type == ReferenceType.WEB_PAGE;

        // Lead: authors (or editors of an edited book) and year. Without both, the title moves first.
        List<Contributor> lead = r.authors();
        boolean editorsLead = false;
        if (lead.isEmpty() && type == ReferenceType.EDITED_BOOK && !r.editors().isEmpty()) {
            lead = r.editors();
            editorsLead = true;
        }
        if (!lead.isEmpty()) {
            f.text(names(lead));
            if (editorsLead) {
                f.text(" (Hrsg.)");
            }
            f.end(".").text(" (" + year(r) + "). ");
            appendTitle(f, title, italicTitle);
        } else {
            appendTitle(f, title, italicTitle);
            f.text(" (" + year(r) + ").");
        }

        switch (type) {
            case BOOK, EDITED_BOOK -> {
                List<String> extras = new ArrayList<>();
                if (has(r.getEdition())) {
                    extras.add(edition(r.getEdition()));
                }
                if (has(r.getVolume())) {
                    extras.add("Bd. " + r.getVolume());
                }
                if (!extras.isEmpty()) {
                    f.text(" (" + String.join(", ", extras) + ")");
                }
                f.end(".");
                sentence(f, r.getPublisher());
            }
            case BOOK_CHAPTER, CONFERENCE_PAPER -> {
                f.end(".");
                f.text(" In ");
                if (!r.editors().isEmpty()) {
                    f.text(editorsInline(r.editors()) + " (Hrsg.), ");
                }
                f.italic(has(r.getContainerTitle()) ? r.getContainerTitle() : "[o. T.]");
                List<String> extras = new ArrayList<>();
                if (has(r.getEdition())) {
                    extras.add(edition(r.getEdition()));
                }
                if (has(r.getPages())) {
                    extras.add("S. " + pages(r.getPages()));
                }
                if (!extras.isEmpty()) {
                    f.text(" (" + String.join(", ", extras) + ")");
                }
                f.end(".");
                sentence(f, r.getPublisher());
            }
            case JOURNAL_ARTICLE -> {
                f.end(".");
                if (has(r.getContainerTitle())) {
                    f.text(" ").italic(r.getContainerTitle());
                    if (has(r.getVolume())) {
                        f.text(", ").italic(r.getVolume());
                    }
                    if (has(r.getIssue())) {
                        f.text("(" + r.getIssue() + ")");
                    }
                    if (has(r.getPages())) {
                        f.text(", " + pages(r.getPages()));
                    }
                    f.text(".");
                }
            }
            case THESIS -> {
                List<String> parts = new ArrayList<>();
                parts.add(has(r.getGenre()) ? r.getGenre() : "Hochschulschrift");
                if (has(r.getPublisher())) {
                    parts.add(r.getPublisher());
                }
                f.text(" [" + String.join(", ", parts) + "].");
            }
            case REPORT -> {
                if (has(r.getGenre())) {
                    f.text(" (" + r.getGenre() + ")");
                }
                f.end(".");
                sentence(f, r.getPublisher());
            }
            case WEB_PAGE -> {
                f.end(".");
                sentence(f, has(r.getContainerTitle()) ? r.getContainerTitle() : r.getPublisher());
            }
        }

        String link = has(r.getDoi()) ? doiUrl(r.getDoi()) : r.getUrl();
        if (has(link)) {
            if (r.getAccessedOn() != null && !has(r.getDoi())) {
                f.text(" Abgerufen am " + ACCESSED.format(r.getAccessedOn()) + ", von " + link);
            } else {
                f.text(" " + link);
            }
        }
        return f;
    }

    @Override
    public String inText(Reference r, String pages) {
        String who = lastNames(r.authors(), " & ", "et al.");
        if (who.isEmpty()) {
            who = r.getType() == ReferenceType.EDITED_BOOK ? lastNames(r.editors(), " & ", "et al.") : "";
        }
        if (who.isEmpty()) {
            who = "„" + shortTitle(r.getTitle()) + "“";
        }
        String result = who + ", " + year(r);
        if (has(pages)) {
            result += ", S. " + pages(pages);
        }
        return "(" + result + ")";
    }

    private static void appendTitle(Formatted f, String title, boolean italic) {
        if (italic) {
            f.italic(title);
        } else {
            f.text(title);
        }
    }

    private static void sentence(Formatted f, String text) {
        if (has(text)) {
            f.text(" " + text.strip());
            f.end(".");
        }
    }

    private static String year(Reference r) {
        return has(r.getYear()) ? r.getYear() : "o. J.";
    }

    /** "Müller, A. B., Meier, C., & Schulz, D." – up to 20 names, then "… last". */
    static String names(List<Contributor> people) {
        List<String> formatted = people.stream()
                .map(c -> c.hasFirstName() ? c.lastName() + ", " + initials(c.firstName()) : c.lastName())
                .toList();
        if (formatted.size() == 1) {
            return formatted.get(0);
        }
        if (formatted.size() > 20) {
            return String.join(", ", formatted.subList(0, 19)) + ", … " + formatted.get(formatted.size() - 1);
        }
        return String.join(", ", formatted.subList(0, formatted.size() - 1)) + ", & "
                + formatted.get(formatted.size() - 1);
    }

    /** Editors in the "In" part: "A. B. Müller & C. Meier". */
    private static String editorsInline(List<Contributor> editors) {
        List<String> formatted = editors.stream()
                .map(c -> c.hasFirstName() ? initials(c.firstName()) + " " + c.lastName() : c.lastName())
                .toList();
        if (formatted.size() <= 2) {
            return String.join(" & ", formatted);
        }
        return String.join(", ", formatted.subList(0, formatted.size() - 1)) + ", & "
                + formatted.get(formatted.size() - 1);
    }

    private static String shortTitle(String title) {
        String[] words = title.strip().split("\\s+");
        return words.length <= 4
                ? title.strip()
                : String.join(" ", List.of(words).subList(0, 4)) + " …";
    }
}
