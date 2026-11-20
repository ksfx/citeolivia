package com.citeolivia.library;

import static com.citeolivia.library.ReferenceField.ACCESSED_ON;
import static com.citeolivia.library.ReferenceField.CONTAINER_TITLE;
import static com.citeolivia.library.ReferenceField.DOI;
import static com.citeolivia.library.ReferenceField.EDITION;
import static com.citeolivia.library.ReferenceField.EDITORS;
import static com.citeolivia.library.ReferenceField.GENRE;
import static com.citeolivia.library.ReferenceField.ISBN;
import static com.citeolivia.library.ReferenceField.ISSUE;
import static com.citeolivia.library.ReferenceField.PAGES;
import static com.citeolivia.library.ReferenceField.PLACE;
import static com.citeolivia.library.ReferenceField.PUBLISHER;
import static com.citeolivia.library.ReferenceField.SERIES;
import static com.citeolivia.library.ReferenceField.URL;
import static com.citeolivia.library.ReferenceField.VOLUME;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Kinds of references (Dokumenttypen), with the fields relevant for each. */
public enum ReferenceType {
    BOOK("Buch", EnumSet.of(PUBLISHER, PLACE, EDITION, SERIES, VOLUME, ISBN, DOI, URL)),
    EDITED_BOOK("Sammelwerk (Herausgeberwerk)", EnumSet.of(EDITORS, PUBLISHER, PLACE, EDITION, SERIES, ISBN, DOI, URL)),
    BOOK_CHAPTER(
            "Beitrag in Sammelwerk",
            EnumSet.of(EDITORS, CONTAINER_TITLE, PUBLISHER, PLACE, EDITION, PAGES, ISBN, DOI, URL)),
    JOURNAL_ARTICLE("Zeitschriftenaufsatz", EnumSet.of(CONTAINER_TITLE, VOLUME, ISSUE, PAGES, DOI, URL)),
    CONFERENCE_PAPER("Beitrag in Tagungsband", EnumSet.of(EDITORS, CONTAINER_TITLE, PUBLISHER, PLACE, PAGES, DOI, URL)),
    THESIS("Hochschulschrift", EnumSet.of(GENRE, PUBLISHER, PLACE, DOI, URL)),
    REPORT("Bericht / Graue Literatur", EnumSet.of(GENRE, PUBLISHER, PLACE, SERIES, DOI, URL, ACCESSED_ON)),
    WEB_PAGE("Internetdokument", EnumSet.of(CONTAINER_TITLE, PUBLISHER, URL, ACCESSED_ON));

    private static final Map<ReferenceField, String> DEFAULT_LABELS = Map.ofEntries(
            Map.entry(EDITORS, "Herausgeber"),
            Map.entry(CONTAINER_TITLE, "Titel des übergeordneten Werks"),
            Map.entry(PUBLISHER, "Verlag"),
            Map.entry(PLACE, "Ort"),
            Map.entry(EDITION, "Auflage"),
            Map.entry(SERIES, "Reihe"),
            Map.entry(VOLUME, "Band"),
            Map.entry(ISSUE, "Heft"),
            Map.entry(PAGES, "Seiten"),
            Map.entry(GENRE, "Art der Arbeit"),
            Map.entry(ISBN, "ISBN"),
            Map.entry(DOI, "DOI"),
            Map.entry(URL, "URL"),
            Map.entry(ACCESSED_ON, "Zugriff am"));

    private final String label;
    private final Set<ReferenceField> fields;

    ReferenceType(String label, Set<ReferenceField> fields) {
        this.label = label;
        this.fields = fields;
    }

    public String getLabel() {
        return label;
    }

    public boolean has(ReferenceField field) {
        return fields.contains(field);
    }

    /** Field label, adapted to the type (e.g. "Zeitschrift" instead of a generic container title). */
    public String label(ReferenceField field) {
        return switch (this) {
            case BOOK_CHAPTER -> field == CONTAINER_TITLE ? "Titel des Sammelwerks" : DEFAULT_LABELS.get(field);
            case JOURNAL_ARTICLE ->
                switch (field) {
                    case CONTAINER_TITLE -> "Zeitschrift";
                    case VOLUME -> "Jahrgang / Band";
                    default -> DEFAULT_LABELS.get(field);
                };
            case CONFERENCE_PAPER -> field == CONTAINER_TITLE ? "Tagungsband" : DEFAULT_LABELS.get(field);
            case THESIS ->
                switch (field) {
                    case PUBLISHER -> "Hochschule";
                    case GENRE -> "Art der Arbeit (z. B. Masterarbeit, Dissertation)";
                    default -> DEFAULT_LABELS.get(field);
                };
            case REPORT ->
                switch (field) {
                    case PUBLISHER -> "Institution / Herausgebende Stelle";
                    case GENRE -> "Art und Nummer (z. B. Arbeitspapier Nr. 12)";
                    default -> DEFAULT_LABELS.get(field);
                };
            case WEB_PAGE ->
                switch (field) {
                    case CONTAINER_TITLE -> "Name der Website";
                    case PUBLISHER -> "Betreiber / Institution";
                    default -> DEFAULT_LABELS.get(field);
                };
            default -> DEFAULT_LABELS.get(field);
        };
    }
}
