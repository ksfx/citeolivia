package com.citeolivia.library;

import com.citeolivia.library.citation.CitationStyle;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** A literature project, e.g. a thesis. Owned by one user. */
@Table("project")
public class Project {

    @Id
    private Long id;

    private Long ownerId;
    private String name;
    private String description;
    private CitationStyle citationStyle;
    private Instant createdAt;

    Project() {}

    static Project create(long ownerId, String name, String description) {
        Project p = new Project();
        p.ownerId = ownerId;
        p.name = name;
        p.description = description;
        p.citationStyle = CitationStyle.APA7;
        p.createdAt = Instant.now();
        return p;
    }

    void update(String name, String description, CitationStyle style) {
        this.name = name;
        this.description = description;
        this.citationStyle = style;
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public CitationStyle getCitationStyle() {
        return citationStyle;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
