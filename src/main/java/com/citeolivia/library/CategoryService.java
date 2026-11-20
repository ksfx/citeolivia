package com.citeolivia.library;

import com.citeolivia.security.ProjectAccess;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The outline (Kategoriensystem) of a project. */
@Service
public class CategoryService {

    private final CategoryRepository categories;

    CategoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    /** All categories in outline order with numbers like "2.1". */
    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional(readOnly = true)
    public List<CategoryNode> outline(Long projectId) {
        List<Category> all = categories.findAllByProjectIdOrderByPositionAscIdAsc(projectId);
        Map<Long, List<Category>> byParent = new LinkedHashMap<>();
        for (Category c : all) {
            byParent.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
        }
        List<CategoryNode> result = new ArrayList<>();
        walk(byParent, null, "", 0, result);
        return result;
    }

    private void walk(
            Map<Long, List<Category>> byParent, Long parent, String prefix, int depth, List<CategoryNode> out) {
        List<Category> children = byParent.getOrDefault(parent, List.of());
        for (int i = 0; i < children.size(); i++) {
            Category c = children.get(i);
            String number = prefix + (i + 1);
            out.add(new CategoryNode(
                    c.getId(), c.getParentId(), c.getName(), number, depth, i == 0, i == children.size() - 1));
            walk(byParent, c.getId(), number + ".", depth + 1, out);
        }
    }

    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional
    public long create(Long projectId, Long parentId, String name) {
        if (parentId != null) {
            categories.findByIdAndProjectId(parentId, projectId).orElseThrow(() -> new NotFoundException("Kategorie"));
        }
        int position = siblings(projectId, parentId).stream()
                        .mapToInt(Category::getPosition)
                        .max()
                        .orElse(-1)
                + 1;
        return categories
                .save(Category.create(projectId, parentId, name.strip(), position))
                .getId();
    }

    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional
    public void rename(Long projectId, Long categoryId, String name) {
        Category c = get(projectId, categoryId);
        c.rename(name.strip());
        categories.save(c);
    }

    /** Deletes the category with all subcategories; assignments are removed, references and quotes stay. */
    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional
    public void delete(Long projectId, Long categoryId) {
        categories.delete(get(projectId, categoryId));
    }

    /** Moves a category one position up (-1) or down (+1) among its siblings. */
    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional
    public void move(Long projectId, Long categoryId, int direction) {
        Category c = get(projectId, categoryId);
        List<Category> siblings = siblings(projectId, c.getParentId());
        int index = -1;
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).getId().equals(categoryId)) {
                index = i;
            }
        }
        int target = index + Integer.signum(direction);
        if (index < 0 || target < 0 || target >= siblings.size()) {
            return;
        }
        Category other = siblings.get(target);
        siblings.set(target, c);
        siblings.set(index, other);
        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).moveTo(i);
        }
        categories.saveAll(siblings);
    }

    /** Keeps only ids of categories that belong to the project (internal helper, no data is returned). */
    public java.util.Set<Long> validIds(Long projectId, java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new java.util.HashSet<>();
        }
        java.util.Set<Long> valid = new java.util.HashSet<>();
        categories.findAllByProjectIdOrderByPositionAscIdAsc(projectId).forEach(c -> valid.add(c.getId()));
        valid.retainAll(ids);
        return valid;
    }

    private Category get(Long projectId, Long categoryId) {
        return categories
                .findByIdAndProjectId(categoryId, projectId)
                .orElseThrow(() -> new NotFoundException("Kategorie"));
    }

    private List<Category> siblings(Long projectId, Long parentId) {
        return new ArrayList<>(categories.findAllByProjectIdOrderByPositionAscIdAsc(projectId).stream()
                .filter(c -> Objects.equals(c.getParentId(), parentId))
                .sorted(Comparator.comparingInt(Category::getPosition).thenComparing(Category::getId))
                .toList());
    }
}
