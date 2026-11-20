package com.citeolivia.library;

import com.citeolivia.library.citation.CitationStyle;
import com.citeolivia.security.ProjectAccess;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Knowledge items (Wissenselemente) and the knowledge organizer. */
@Service
public class KnowledgeService {

    private final KnowledgeItemRepository items;
    private final ReferenceRepository references;
    private final ProjectRepository projects;
    private final CategoryService categories;

    KnowledgeService(
            KnowledgeItemRepository items,
            ReferenceRepository references,
            ProjectRepository projects,
            CategoryService categories) {
        this.items = items;
        this.references = references;
        this.projects = projects;
        this.categories = categories;
    }

    /** Input for creating/updating a knowledge item. */
    public record Input(
            KnowledgeType type, String coreStatement, String text, String pageRange, Collection<Long> categoryIds) {}

    /** Section of the knowledge organizer: a category (null = not assigned) with its items. */
    public record Section(CategoryNode category, List<KnowledgeView> items) {}

    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional
    public long create(Long projectId, Long referenceId, Input input) {
        if (referenceId != null) {
            references.findByIdAndProjectId(referenceId, projectId).orElseThrow(() -> new NotFoundException("Titel"));
        }
        KnowledgeItem item = KnowledgeItem.create(projectId, referenceId);
        apply(projectId, item, input);
        return items.save(item).getId();
    }

    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional
    public void update(Long projectId, Long itemId, Input input) {
        KnowledgeItem item = get(projectId, itemId);
        apply(projectId, item, input);
        items.save(item);
    }

    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional
    public void delete(Long projectId, Long itemId) {
        items.delete(get(projectId, itemId));
    }

    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional(readOnly = true)
    public KnowledgeView view(Long projectId, Long itemId) {
        KnowledgeItem item = get(projectId, itemId);
        Reference reference = item.getReferenceEntryId() == null
                ? null
                : references.findById(item.getReferenceEntryId()).orElse(null);
        return toView(item, reference, style(projectId), outlineById(projectId));
    }

    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional(readOnly = true)
    public List<KnowledgeView> forReference(Long projectId, Long referenceId) {
        Reference reference = references
                .findByIdAndProjectId(referenceId, projectId)
                .orElseThrow(() -> new NotFoundException("Titel"));
        CitationStyle style = style(projectId);
        Map<Long, CategoryNode> outline = outlineById(projectId);
        return items.findAllByProjectIdAndReferenceEntryIdOrderByCreatedAtAsc(projectId, referenceId).stream()
                .map(i -> toView(i, reference, style, outline))
                .toList();
    }

    /**
     * Knowledge organizer: all items grouped by category in outline order. An item assigned to several
     * categories appears in each. Items without category come last. With {@code categoryId}, only that
     * category is returned.
     */
    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional(readOnly = true)
    public List<Section> organizer(Long projectId, Long categoryId) {
        CitationStyle style = style(projectId);
        List<CategoryNode> outline = categories.outline(projectId);
        Map<Long, CategoryNode> outlineById =
                outline.stream().collect(Collectors.toMap(CategoryNode::id, Function.identity()));
        Map<Long, Reference> refs = references.findAllByProjectId(projectId).stream()
                .collect(Collectors.toMap(Reference::getId, Function.identity()));

        Map<Long, List<KnowledgeView>> byCategory = new LinkedHashMap<>();
        List<KnowledgeView> unassigned = new ArrayList<>();
        for (KnowledgeItem item : items.findAllByProjectIdOrderByCreatedAtAsc(projectId)) {
            KnowledgeView view = toView(item, refs.get(item.getReferenceEntryId()), style, outlineById);
            if (view.categories().isEmpty()) {
                unassigned.add(view);
            }
            for (CategoryNode c : view.categories()) {
                byCategory.computeIfAbsent(c.id(), k -> new ArrayList<>()).add(view);
            }
        }
        List<Section> sections = new ArrayList<>();
        for (CategoryNode node : outline) {
            if (categoryId == null || node.id().equals(categoryId)) {
                sections.add(new Section(node, byCategory.getOrDefault(node.id(), List.of())));
            }
        }
        if (categoryId == null) {
            sections.add(new Section(null, unassigned));
        }
        return sections;
    }

    /** The knowledge organizer as Markdown: outline headings with quotes and citations – a draft to write from. */
    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional(readOnly = true)
    public String organizerMarkdown(Long projectId) {
        Project project = projects.findById(projectId).orElseThrow(() -> new NotFoundException("Projekt"));
        StringBuilder md = new StringBuilder("# ").append(project.getName()).append("\n\n");
        for (Section section : organizer(projectId, null)) {
            if (section.category() == null && section.items().isEmpty()) {
                continue;
            }
            int level =
                    section.category() == null ? 2 : Math.min(section.category().depth() + 2, 6);
            md.append("#".repeat(level))
                    .append(' ')
                    .append(
                            section.category() == null
                                    ? "Ohne Kategorie"
                                    : section.category().label())
                    .append("\n\n");
            for (KnowledgeView v : section.items()) {
                KnowledgeItem i = v.item();
                if (i.getCoreStatement() != null) {
                    md.append("**")
                            .append(i.getCoreStatement())
                            .append("** _(")
                            .append(i.getType().getLabel())
                            .append(")_\n\n");
                } else {
                    md.append("_").append(i.getType().getLabel()).append("_\n\n");
                }
                String text = i.getText().strip();
                if (i.getType() == KnowledgeType.QUOTE) {
                    md.append("> „").append(text.replace("\n", "\n> ")).append("“");
                } else {
                    md.append(text);
                }
                if (v.citation() != null) {
                    md.append(' ').append(v.citation());
                }
                md.append("\n\n");
            }
        }
        return md.toString();
    }

    private void apply(Long projectId, KnowledgeItem item, Input input) {
        if (input.text() == null || input.text().isBlank()) {
            throw new IllegalArgumentException("Bitte einen Text angeben.");
        }
        KnowledgeType type = input.type() == null ? KnowledgeType.QUOTE : input.type();
        if (item.getReferenceEntryId() == null && type.isCitable()) {
            type = KnowledgeType.THOUGHT;
        }
        item.update(
                type,
                ProjectService.blankToNull(input.coreStatement()),
                input.text().strip(),
                ProjectService.blankToNull(input.pageRange()),
                categories.validIds(projectId, input.categoryIds()));
    }

    private KnowledgeItem get(Long projectId, Long itemId) {
        return items.findByIdAndProjectId(itemId, projectId).orElseThrow(() -> new NotFoundException("Wissenselement"));
    }

    private KnowledgeView toView(
            KnowledgeItem item, Reference reference, CitationStyle style, Map<Long, CategoryNode> outline) {
        String source = null;
        String citation = null;
        if (reference != null) {
            List<Contributor> people = reference.authors().isEmpty() ? reference.editors() : reference.authors();
            source = (people.isEmpty() ? reference.getTitle() : ContributorLines.shortNames(people)) + " "
                    + (reference.getYear() == null ? "o. J." : reference.getYear());
            if (item.getType().isCitable()) {
                citation = style.inText(reference, item.getPageRange());
            }
        }
        List<CategoryNode> cats = outline.values().stream()
                .filter(c -> item.categoryIds().contains(c.id()))
                .sorted(java.util.Comparator.comparing(CategoryNode::number, KnowledgeService::compareNumbers))
                .toList();
        return new KnowledgeView(item, reference, source, citation, cats);
    }

    private Map<Long, CategoryNode> outlineById(Long projectId) {
        Map<Long, CategoryNode> map = new LinkedHashMap<>();
        categories.outline(projectId).forEach(c -> map.put(c.id(), c));
        return map;
    }

    private CitationStyle style(Long projectId) {
        return projects.findById(projectId).map(Project::getCitationStyle).orElse(CitationStyle.APA7);
    }

    /** "2.10" after "2.9". */
    static int compareNumbers(String a, String b) {
        String[] x = a.split("\\.");
        String[] y = b.split("\\.");
        for (int i = 0; i < Math.min(x.length, y.length); i++) {
            int c = Integer.compare(Integer.parseInt(x[i]), Integer.parseInt(y[i]));
            if (c != 0) {
                return c;
            }
        }
        return Integer.compare(x.length, y.length);
    }
}
