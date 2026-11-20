package com.citeolivia.web;

import com.citeolivia.library.KnowledgeType;
import com.citeolivia.library.ReferenceField;
import com.citeolivia.library.ReferenceType;
import com.citeolivia.library.citation.CitationStyle;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Enum values for the templates of the literature management. */
@ControllerAdvice(
        assignableTypes = {
            ProjectController.class,
            ReferenceController.class,
            KnowledgeController.class,
            CategoryController.class,
            BibliographyController.class
        })
class LibraryModelAdvice {

    private static final Map<String, ReferenceField> FIELDS = new LinkedHashMap<>();

    static {
        for (ReferenceField f : ReferenceField.values()) {
            FIELDS.put(f.name(), f);
        }
    }

    @ModelAttribute
    void enums(Model model) {
        model.addAttribute("referenceTypes", ReferenceType.values());
        model.addAttribute("knowledgeTypes", KnowledgeType.values());
        model.addAttribute("citationStyles", CitationStyle.values());
        model.addAttribute("F", FIELDS);
    }
}
