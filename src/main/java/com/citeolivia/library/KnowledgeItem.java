package com.citeolivia.library;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Knowledge item (Wissenselement): quote, paraphrase, summary or comment on a reference, or an own
 * thought without a reference. Assigned to categories of the outline.
 */
@Table("knowledge_item")
public class KnowledgeItem {

    @Id
    private Long id;

    private Long projectId;

    /** {@code null} for own thoughts not bound to a source. */
    private Long referenceEntryId;

    private KnowledgeType type;
    private String coreStatement;
    private String text;
    private String pageRange;
    private Instant createdAt;
    private Instant updatedAt;

    @MappedCollection(idColumn = "knowledge_item_id")
    private Set<KnowledgeItemCategory> categories = new HashSet<>();

    KnowledgeItem() {}

    static KnowledgeItem create(long projectId, Long referenceId) {
        KnowledgeItem k = new KnowledgeItem();
        k.projectId = projectId;
        k.referenceEntryId = referenceId;
        k.createdAt = Instant.now();
        return k;
    }

    void update(KnowledgeType type, String coreStatement, String text, String pageRange, Set<Long> categoryIds) {
        this.type = type;
        this.coreStatement = coreStatement;
        this.text = text;
        this.pageRange = pageRange;
        this.categories = new HashSet<>();
        categoryIds.forEach(id -> categories.add(new KnowledgeItemCategory(id)));
        this.updatedAt = Instant.now();
    }

    public Set<Long> categoryIds() {
        Set<Long> ids = new HashSet<>();
        categories.forEach(c -> ids.add(c.categoryId()));
        return ids;
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getReferenceEntryId() {
        return referenceEntryId;
    }

    public KnowledgeType getType() {
        return type;
    }

    public String getCoreStatement() {
        return coreStatement;
    }

    public String getText() {
        return text;
    }

    public String getPageRange() {
        return pageRange;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
