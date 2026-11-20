package com.citeolivia.library;

/**
 * Reference prepared for display.
 *
 * @param citationHtml bibliography entry in the project's citation style (escaped HTML)
 * @param citationText the same as plain text
 * @param inText in-text citation, e.g. "(Müller, 2020)"
 * @param attachmentCount number of attached PDFs
 */
public record ReferenceSummary(
        Reference reference,
        String authorsShort,
        String citationHtml,
        String citationText,
        String inText,
        long knowledgeCount,
        long attachmentCount) {}
