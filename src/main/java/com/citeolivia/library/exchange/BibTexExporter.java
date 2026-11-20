package com.citeolivia.library.exchange;

import com.citeolivia.library.Contributor;
import com.citeolivia.library.Reference;
import com.citeolivia.library.ReferenceType;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Exports references as BibTeX (UTF-8, works with biblatex/biber and BibTeX). */
public final class BibTexExporter {

    private BibTexExporter() {}

    public static String export(List<Reference> references) {
        return references.stream().map(BibTexExporter::entry).collect(Collectors.joining("\n"));
    }

    static String entry(Reference r) {
        Map<String, String> fields = new LinkedHashMap<>();
        put(fields, "author", names(r.authors()));
        put(fields, "editor", names(r.editors()));
        String title = r.getTitle();
        put(fields, "title", title);
        put(fields, "subtitle", r.getSubtitle());
        put(fields, "year", r.getYear());
        String containerField = switch (r.getType()) {
            case JOURNAL_ARTICLE -> "journal";
            case BOOK_CHAPTER, CONFERENCE_PAPER -> "booktitle";
            case WEB_PAGE -> "organization";
            default -> null;
        };
        if (containerField != null) {
            put(fields, containerField, r.getContainerTitle());
        }
        String publisherField = switch (r.getType()) {
            case THESIS -> "school";
            case REPORT -> "institution";
            default -> "publisher";
        };
        put(fields, publisherField, r.getPublisher());
        put(fields, "address", r.getPlace());
        put(fields, "edition", r.getEdition());
        put(fields, "series", r.getSeries());
        put(fields, "volume", r.getVolume());
        put(fields, "number", r.getIssue());
        put(fields, "pages", r.getPages() == null ? null : r.getPages().replaceAll("\\s*[-–]+\\s*", "--"));
        if (r.getType() == ReferenceType.THESIS || r.getType() == ReferenceType.REPORT) {
            put(fields, "type", r.getGenre());
        }
        put(fields, "isbn", r.getIsbn());
        put(fields, "doi", r.getDoi());
        put(fields, "url", r.getUrl());
        if (r.getAccessedOn() != null) {
            put(fields, "urldate", DateTimeFormatter.ISO_LOCAL_DATE.format(r.getAccessedOn()));
        }
        put(fields, "keywords", r.getKeywords());
        put(fields, "abstract", r.getAbstractText());

        StringBuilder sb =
                new StringBuilder("@").append(entryType(r)).append('{').append(r.getCitationKey());
        fields.forEach(
                (k, v) -> sb.append(",\n  ").append(k).append(" = {").append(v).append('}'));
        return sb.append("\n}\n").toString();
    }

    private static String entryType(Reference r) {
        return switch (r.getType()) {
            case BOOK, EDITED_BOOK -> "book";
            case BOOK_CHAPTER -> "incollection";
            case JOURNAL_ARTICLE -> "article";
            case CONFERENCE_PAPER -> "inproceedings";
            case THESIS ->
                r.getGenre() != null && r.getGenre().toLowerCase().contains("diss") ? "phdthesis" : "mastersthesis";
            case REPORT -> "techreport";
            case WEB_PAGE -> "misc";
        };
    }

    /** Organisations are wrapped in braces so that BibTeX does not split them into first/last name. */
    private static String names(List<Contributor> people) {
        if (people.isEmpty()) {
            return null;
        }
        return people.stream()
                .map(c -> c.hasFirstName()
                        ? escape(c.lastName()) + ", " + escape(c.firstName())
                        : "{" + escape(c.lastName()) + "}")
                .collect(Collectors.joining(" and "));
    }

    private static void put(Map<String, String> fields, String key, String value) {
        if (value != null && !value.isBlank()) {
            fields.put(key, key.equals("author") || key.equals("editor") ? value : escape(value.strip()));
        }
    }

    /** Escapes LaTeX special characters; braces are balanced-escaped. */
    static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&', '%', '$', '#', '_', '{', '}' -> sb.append('\\').append(c);
                case '\\' -> sb.append("\\textbackslash{}");
                case '~' -> sb.append("\\textasciitilde{}");
                case '^' -> sb.append("\\textasciicircum{}");
                case '\n', '\r' -> sb.append(' ');
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
