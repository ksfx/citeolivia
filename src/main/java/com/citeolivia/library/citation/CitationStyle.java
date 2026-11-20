package com.citeolivia.library.citation;

import com.citeolivia.library.Reference;

/** Supported citation styles (Zitationsstile). */
public enum CitationStyle {
    APA7("APA 7 (deutsch)", new Apa7Formatter()),
    DIN_ISO_690("DIN ISO 690 (Autor-Jahr)", new DinIso690Formatter());

    private final String label;
    private final CitationFormatter formatter;

    CitationStyle(String label, CitationFormatter formatter) {
        this.label = label;
        this.formatter = formatter;
    }

    public String getLabel() {
        return label;
    }

    public Formatted bibliography(Reference reference) {
        return formatter.bibliography(reference);
    }

    public String inText(Reference reference, String pages) {
        return formatter.inText(reference, pages);
    }
}
