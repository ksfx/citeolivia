package com.citeolivia.web;

import com.citeolivia.library.ContributorLines;
import com.citeolivia.library.ContributorRole;
import com.citeolivia.library.Reference;
import com.citeolivia.library.ReferenceDraft;
import com.citeolivia.library.ReferenceType;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/** Form backing object for creating/editing a reference. Authors/editors as text, one per line. */
public class ReferenceForm {

    @Valid
    private ReferenceDraft draft = new ReferenceDraft();

    private String authors;
    private String editors;
    private List<Long> categoryIds = new ArrayList<>();
    private String citationKey;

    public static ReferenceForm empty(ReferenceType type) {
        ReferenceForm f = new ReferenceForm();
        f.draft.setType(type == null ? ReferenceType.BOOK : type);
        return f;
    }

    public static ReferenceForm of(ReferenceDraft draft) {
        ReferenceForm f = new ReferenceForm();
        f.draft = draft;
        f.authors = ContributorLines.format(draft.authors());
        f.editors = ContributorLines.format(draft.editors());
        return f;
    }

    public static ReferenceForm of(Reference reference) {
        ReferenceForm f = of(reference.toDraft());
        f.categoryIds = new ArrayList<>(reference.categoryIds());
        f.citationKey = reference.getCitationKey();
        return f;
    }

    /** The draft including the parsed authors and editors. */
    public ReferenceDraft toDraft() {
        List<com.citeolivia.library.Contributor> people =
                new ArrayList<>(ContributorLines.parse(authors, ContributorRole.AUTHOR));
        if (draft.getType() != null && draft.getType().has(com.citeolivia.library.ReferenceField.EDITORS)) {
            people.addAll(ContributorLines.parse(editors, ContributorRole.EDITOR));
        }
        draft.setContributors(people);
        return draft;
    }

    public ReferenceType getType() {
        return draft.getType() == null ? ReferenceType.BOOK : draft.getType();
    }

    public ReferenceDraft getDraft() {
        return draft;
    }

    public void setDraft(ReferenceDraft draft) {
        this.draft = draft;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getEditors() {
        return editors;
    }

    public void setEditors(String editors) {
        this.editors = editors;
    }

    public List<Long> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<Long> categoryIds) {
        this.categoryIds = categoryIds == null ? new ArrayList<>() : categoryIds;
    }

    public String getCitationKey() {
        return citationKey;
    }

    public void setCitationKey(String citationKey) {
        this.citationKey = citationKey;
    }
}
