package com.citeolivia.library;

/** Kinds of knowledge items (Wissenselemente). */
public enum KnowledgeType {
    QUOTE("Wörtliches Zitat", true),
    PARAPHRASE("Indirektes Zitat", true),
    SUMMARY("Zusammenfassung", true),
    COMMENT("Kommentar", false),
    THOUGHT("Eigener Gedanke", false);

    private final String label;
    private final boolean citable;

    KnowledgeType(String label, boolean citable) {
        this.label = label;
        this.citable = citable;
    }

    public String getLabel() {
        return label;
    }

    /** Whether a citation of the source belongs to it (quotes and paraphrases). */
    public boolean isCitable() {
        return citable;
    }
}
