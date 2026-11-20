package com.citeolivia.library;

import java.util.List;

/**
 * Knowledge item prepared for display.
 *
 * @param reference the source (null for own thoughts)
 * @param source short source, e.g. "Müller 2020"
 * @param citation in-text citation including the page, e.g. "(Müller, 2020, S. 12)"
 * @param categories assigned categories in outline order
 */
public record KnowledgeView(
        KnowledgeItem item, Reference reference, String source, String citation, List<CategoryNode> categories) {}
