package com.citeolivia.library.citation;

import com.citeolivia.library.Reference;

/** A citation style: bibliography entry and in-text citation. */
public interface CitationFormatter {

    Formatted bibliography(Reference reference);

    /** In-text citation, e.g. "(Müller, 2020, S. 12)". {@code pages} may be null. */
    String inText(Reference reference, String pages);
}
