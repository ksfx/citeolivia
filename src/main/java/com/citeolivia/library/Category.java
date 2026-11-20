package com.citeolivia.library;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Node of the project outline (Kategoriensystem / Gliederung). A separate aggregate so that ids stay
 * stable – references and knowledge items point to categories by id.
 */
@Table("category")
public class Category {

    @Id
    private Long id;

    private Long projectId;
    private Long parentId;
    private String name;
    private int position;

    Category() {}

    static Category create(long projectId, Long parentId, String name, int position) {
        Category c = new Category();
        c.projectId = projectId;
        c.parentId = parentId;
        c.name = name;
        c.position = position;
        return c;
    }

    void rename(String newName) {
        name = newName;
    }

    void moveTo(int newPosition) {
        position = newPosition;
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public int getPosition() {
        return position;
    }
}
