package com.citeolivia.library.exchange;

import com.citeolivia.library.Contributor;
import com.citeolivia.library.Reference;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Exports references as RIS – importable by Citavi, Zotero, EndNote, Mendeley and Word add-ins. */
public final class RisExporter {

    private RisExporter() {}

    public static String export(List<Reference> references) {
        StringBuilder sb = new StringBuilder();
        references.forEach(r -> entry(sb, r));
        return sb.toString();
    }

    private static void entry(StringBuilder sb, Reference r) {
        line(
                sb,
                "TY",
                switch (r.getType()) {
                    case BOOK -> "BOOK";
                    case EDITED_BOOK -> "EDBOOK";
                    case BOOK_CHAPTER -> "CHAP";
                    case JOURNAL_ARTICLE -> "JOUR";
                    case CONFERENCE_PAPER -> "CPAPER";
                    case THESIS -> "THES";
                    case REPORT -> "RPRT";
                    case WEB_PAGE -> "ELEC";
                });
        line(sb, "ID", r.getCitationKey());
        for (Contributor c : r.authors()) {
            line(sb, "AU", c.displayName());
        }
        for (Contributor c : r.editors()) {
            line(sb, r.getType() == com.citeolivia.library.ReferenceType.EDITED_BOOK ? "A2" : "ED", c.displayName());
        }
        line(sb, "TI", r.getSubtitle() == null ? r.getTitle() : r.getTitle() + ": " + r.getSubtitle());
        line(sb, "T2", r.getContainerTitle());
        line(sb, "PY", r.getYear());
        line(sb, "PB", r.getPublisher());
        line(sb, "CY", r.getPlace());
        line(sb, "ET", r.getEdition());
        line(sb, "T3", r.getSeries());
        line(sb, "VL", r.getVolume());
        line(sb, "IS", r.getIssue());
        if (r.getPages() != null) {
            String[] p = r.getPages().split("\\s*[-–]+\\s*", 2);
            line(sb, "SP", p[0]);
            if (p.length > 1) {
                line(sb, "EP", p[1]);
            }
        }
        line(sb, "M3", r.getGenre());
        line(sb, "SN", r.getIsbn());
        line(sb, "DO", r.getDoi());
        line(sb, "UR", r.getUrl());
        if (r.getAccessedOn() != null) {
            line(sb, "Y2", DateTimeFormatter.ofPattern("yyyy/MM/dd").format(r.getAccessedOn()));
        }
        line(sb, "AB", r.getAbstractText());
        if (r.getKeywords() != null) {
            for (String kw : List.of(r.getKeywords().split("[,;]"))) {
                line(sb, "KW", kw.strip());
            }
        }
        line(sb, "N1", r.getNotes());
        sb.append("ER  - \r\n\r\n");
    }

    private static void line(StringBuilder sb, String tag, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(tag)
                    .append("  - ")
                    .append(value.strip().replaceAll("\\R", " "))
                    .append("\r\n");
        }
    }
}
