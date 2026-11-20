package com.citeolivia.library.citation;

import static com.citeolivia.library.citation.CitationSupport.edition;
import static com.citeolivia.library.citation.CitationSupport.fullTitle;
import static com.citeolivia.library.citation.CitationSupport.has;
import static com.citeolivia.library.citation.CitationSupport.lastNames;
import static com.citeolivia.library.citation.CitationSupport.pages;

import com.citeolivia.library.Contributor;
import com.citeolivia.library.Reference;
import com.citeolivia.library.ReferenceType;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** DIN ISO 690 in the author-year variant common at German universities. */
final class DinIso690Formatter implements CitationFormatter {

    private static final DateTimeFormatter ACCESSED = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    public Formatted bibliography(Reference r) {
        Formatted f = new Formatted();
        ReferenceType type = r.getType();

        if (!r.authors().isEmpty()) {
            f.text(names(r.authors()) + ": ");
        } else if (type == ReferenceType.EDITED_BOOK && !r.editors().isEmpty()) {
            f.text(names(r.editors()) + " (Hrsg.): ");
        }
        f.text(fullTitle(r.getTitle(), r.getSubtitle()));
        f.end(".");

        switch (type) {
            case BOOK, EDITED_BOOK -> {
                if (has(r.getEdition())) {
                    f.text(" " + edition(r.getEdition()));
                    f.end(".");
                }
                f.text(" ").text(imprint(r));
                if (has(r.getSeries())) {
                    f.text(" (" + r.getSeries() + (has(r.getVolume()) ? ", Bd. " + r.getVolume() : "") + ")");
                } else if (has(r.getVolume())) {
                    f.text(", Bd. " + r.getVolume());
                }
                f.end(".");
                if (has(r.getIsbn())) {
                    f.text(" ISBN " + r.getIsbn().strip() + ".");
                }
            }
            case BOOK_CHAPTER, CONFERENCE_PAPER -> {
                f.text(" In: ");
                if (!r.editors().isEmpty()) {
                    f.text(names(r.editors()) + " (Hrsg.): ");
                }
                f.text(has(r.getContainerTitle()) ? r.getContainerTitle() : "[o. T.]");
                f.end(".");
                if (has(r.getEdition())) {
                    f.text(" " + edition(r.getEdition()));
                    f.end(".");
                }
                f.text(" ").text(imprint(r));
                if (has(r.getPages())) {
                    f.text(", S. " + pages(r.getPages()));
                }
                f.end(".");
            }
            case JOURNAL_ARTICLE -> {
                f.text(" In: ").text(has(r.getContainerTitle()) ? r.getContainerTitle() : "[o. T.]");
                if (has(r.getVolume())) {
                    f.text(" " + r.getVolume());
                }
                f.text(" (" + year(r) + ")");
                if (has(r.getIssue())) {
                    f.text(", Nr. " + r.getIssue());
                }
                if (has(r.getPages())) {
                    f.text(", S. " + pages(r.getPages()));
                }
                f.end(".");
            }
            case THESIS -> {
                StringBuilder sb = new StringBuilder();
                if (has(r.getPlace())) {
                    sb.append(r.getPlace()).append(", ");
                }
                if (has(r.getPublisher())) {
                    sb.append(r.getPublisher()).append(", ");
                }
                sb.append(has(r.getGenre()) ? r.getGenre() : "Hochschulschrift")
                        .append(", ")
                        .append(year(r));
                f.text(" " + sb);
                f.end(".");
            }
            case REPORT -> {
                f.text(" ").text(imprint(r));
                if (has(r.getGenre())) {
                    f.text(" (" + r.getGenre() + ")");
                }
                f.end(".");
            }
            case WEB_PAGE -> {
                String site = has(r.getContainerTitle()) ? r.getContainerTitle() : r.getPublisher();
                if (has(site)) {
                    f.text(" " + site + (has(r.getYear()) ? ", " + r.getYear() : ""));
                    f.end(".");
                } else if (has(r.getYear())) {
                    f.text(" " + r.getYear() + ".");
                }
            }
        }

        if (has(r.getDoi())) {
            f.text(" DOI: " + r.getDoi().strip().replaceFirst("^https?://(dx\\.)?doi\\.org/", ""));
            f.end(".");
        } else if (has(r.getUrl())) {
            f.text(" URL: " + r.getUrl().strip());
            if (r.getAccessedOn() != null) {
                f.text(" (Zugriff am: " + ACCESSED.format(r.getAccessedOn()) + ")");
            }
        }
        return f;
    }

    @Override
    public String inText(Reference r, String pages) {
        String who = lastNames(r.authors(), "; ", "u. a.");
        if (who.isEmpty() && r.getType() == ReferenceType.EDITED_BOOK) {
            who = lastNames(r.editors(), "; ", "u. a.");
        }
        if (who.isEmpty()) {
            who = r.getTitle().strip().split("\\s+")[0] + " …";
        }
        String result = who + " " + year(r);
        if (has(pages)) {
            result += ", S. " + pages(pages);
        }
        return "(" + result + ")";
    }

    /** "Ort: Verlag, Jahr" with the parts that are known. */
    private static String imprint(Reference r) {
        StringBuilder sb = new StringBuilder();
        if (has(r.getPlace())) {
            sb.append(r.getPlace());
            if (has(r.getPublisher())) {
                sb.append(": ");
            }
        }
        if (has(r.getPublisher())) {
            sb.append(r.getPublisher());
        }
        if (!sb.isEmpty()) {
            sb.append(", ");
        }
        sb.append(year(r));
        return sb.toString();
    }

    private static String year(Reference r) {
        return has(r.getYear()) ? r.getYear() : "o. J.";
    }

    /** "Müller, Anna; Meier, Carl" – more than three: first one plus "u. a." */
    static String names(List<Contributor> people) {
        List<String> formatted = people.stream().map(Contributor::displayName).toList();
        if (formatted.size() > 3) {
            return formatted.get(0) + " u. a.";
        }
        return String.join("; ", formatted);
    }
}
