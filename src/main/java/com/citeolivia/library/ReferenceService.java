package com.citeolivia.library;

import com.citeolivia.library.citation.CitationStyle;
import com.citeolivia.library.citation.Formatted;
import com.citeolivia.library.exchange.BibTexImporter;
import com.citeolivia.security.ProjectAccess;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** References (Titel) of a project: CRUD, search, duplicates, import and display preparation. */
@Service
public class ReferenceService {

    private final ReferenceRepository references;
    private final ProjectRepository projects;
    private final CategoryService categories;
    private final JdbcClient jdbc;

    ReferenceService(
            ReferenceRepository references, ProjectRepository projects, CategoryService categories, JdbcClient jdbc) {
        this.references = references;
        this.projects = projects;
        this.categories = categories;
        this.jdbc = jdbc;
    }

    /** Filter for the reference list. All parts are optional. */
    public record Filter(String query, ReferenceType type, Long categoryId) {

        public static Filter none() {
            return new Filter(null, null, null);
        }
    }

    /** Serializable: shown via flash attribute, i.e. stored in the (JDBC) session. */
    public record ImportResult(int imported, List<String> skipped) implements java.io.Serializable {

        public ImportResult {
            skipped = List.copyOf(skipped);
        }
    }

    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional(readOnly = true)
    public Reference get(Long projectId, Long referenceId) {
        return references
                .findByIdAndProjectId(referenceId, projectId)
                .orElseThrow(() -> new NotFoundException("Titel"));
    }

    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional(readOnly = true)
    public ReferenceSummary summary(Long projectId, Long referenceId) {
        Reference r = get(projectId, referenceId);
        return summarize(
                r,
                style(projectId),
                knowledgeCounts(projectId).getOrDefault(r.getId(), 0L),
                attachmentCounts(projectId).getOrDefault(r.getId(), 0L));
    }

    /** References matching the filter, sorted like a bibliography (author, year, title). */
    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional(readOnly = true)
    public List<ReferenceSummary> search(Long projectId, Filter filter) {
        StringBuilder sql = new StringBuilder("SELECT r.id FROM reference_entry r WHERE r.project_id = :p");
        Map<String, Object> params = new HashMap<>();
        params.put("p", projectId);
        if (filter.type() != null) {
            sql.append(" AND r.type = :type");
            params.put("type", filter.type().name());
        }
        if (filter.categoryId() != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM reference_category rc"
                    + " WHERE rc.reference_entry_id = r.id AND rc.category_id = :cat)");
            params.put("cat", filter.categoryId());
        }
        if (filter.query() != null && !filter.query().isBlank()) {
            int i = 0;
            for (String term : filter.query().strip().split("\\s+")) {
                String name = "q" + i++;
                sql.append(" AND (r.title ILIKE :")
                        .append(name)
                        .append(" OR r.subtitle ILIKE :")
                        .append(name)
                        .append(" OR r.container_title ILIKE :")
                        .append(name)
                        .append(" OR r.keywords ILIKE :")
                        .append(name)
                        .append(" OR r.year ILIKE :")
                        .append(name)
                        .append(" OR r.citation_key ILIKE :")
                        .append(name)
                        .append(" OR r.abstract_text ILIKE :")
                        .append(name)
                        .append(" OR EXISTS (SELECT 1 FROM reference_contributor c WHERE c.reference_entry_id = r.id"
                                + " AND (c.last_name ILIKE :")
                        .append(name)
                        .append(" OR c.first_name ILIKE :")
                        .append(name)
                        .append(")))");
                params.put(name, "%" + escapeLike(term) + "%");
            }
        }
        List<Long> ids =
                jdbc.sql(sql.toString()).params(params).query(Long.class).list();
        CitationStyle style = style(projectId);
        Map<Long, Long> counts = knowledgeCounts(projectId);
        Map<Long, Long> attachments = attachmentCounts(projectId);
        List<ReferenceSummary> result = new ArrayList<>();
        for (Reference r : references.findAllById(ids)) {
            result.add(
                    summarize(r, style, counts.getOrDefault(r.getId(), 0L), attachments.getOrDefault(r.getId(), 0L)));
        }
        result.sort(Comparator.comparing(ReferenceSummary::reference, bibliographyOrder()));
        return result;
    }

    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional(readOnly = true)
    public List<Reference> all(Long projectId) {
        List<Reference> all = new ArrayList<>(references.findAllByProjectId(projectId));
        all.sort(bibliographyOrder());
        return all;
    }

    /** Existing reference with the same DOI or ISBN, if any. */
    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional(readOnly = true)
    public Optional<Reference> findDuplicate(Long projectId, ReferenceDraft draft, Long ignoreId) {
        String doi = draft.getDoi() == null ? null : draft.getDoi().strip().toLowerCase(Locale.ROOT);
        String isbn = normalizeIsbn(draft.getIsbn());
        if (doi == null && isbn == null) {
            return Optional.empty();
        }
        return references.findAllByProjectId(projectId).stream()
                .filter(r -> !r.getId().equals(ignoreId))
                .filter(r ->
                        (doi != null && r.getDoi() != null && r.getDoi().strip().equalsIgnoreCase(doi))
                                || (isbn != null && isbn.equals(normalizeIsbn(r.getIsbn()))))
                .findFirst();
    }

    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional
    public long create(Long projectId, ReferenceDraft draft, Collection<Long> categoryIds) {
        draft.normalize();
        String key = CitationKeys.generate(draft, k -> references.existsByProjectIdAndCitationKey(projectId, k));
        Reference r = Reference.create(projectId, draft, key);
        r.assignCategories(categories.validIds(projectId, categoryIds));
        return references.save(r).getId();
    }

    /**
     * Updates a reference. {@code citationKey} may be null (unchanged); an invalid or already used key
     * is rejected with {@link IllegalArgumentException}.
     */
    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional
    public void update(
            Long projectId, Long referenceId, ReferenceDraft draft, Collection<Long> categoryIds, String citationKey) {
        Reference r = get(projectId, referenceId);
        draft.normalize();
        if (citationKey != null
                && !citationKey.isBlank()
                && !citationKey.strip().equals(r.getCitationKey())) {
            String key = citationKey.strip();
            if (!CitationKeys.isValid(key)) {
                throw new IllegalArgumentException(
                        "Zitierschlüssel darf nur Buchstaben, Ziffern und _ : - . enthalten.");
            }
            if (references.existsByProjectIdAndCitationKey(projectId, key)) {
                throw new IllegalArgumentException("Zitierschlüssel „" + key + "“ wird schon verwendet.");
            }
            r.changeCitationKey(key);
        }
        r.apply(draft);
        r.assignCategories(categories.validIds(projectId, categoryIds));
        references.save(r);
    }

    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional
    public void delete(Long projectId, Long referenceId) {
        references.delete(get(projectId, referenceId));
    }

    /** Imports BibTeX; entries with a DOI/ISBN already in the project are skipped. */
    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional
    public ImportResult importBibTex(Long projectId, String bibtex) {
        BibTexImporter.Result parsed = BibTexImporter.parse(bibtex);
        List<String> skipped = new ArrayList<>(parsed.problems());
        int imported = 0;
        for (BibTexImporter.Entry entry : parsed.entries()) {
            ReferenceDraft draft = entry.draft();
            Optional<Reference> duplicate = findDuplicate(projectId, draft, null);
            if (duplicate.isPresent()) {
                skipped.add("„" + draft.getTitle() + "“ ist schon vorhanden (gleiche DOI/ISBN).");
                continue;
            }
            String key = entry.key();
            Reference r;
            if (CitationKeys.isValid(key) && !references.existsByProjectIdAndCitationKey(projectId, key)) {
                r = Reference.create(projectId, draft, key);
            } else {
                r = Reference.create(
                        projectId,
                        draft,
                        CitationKeys.generate(draft, k -> references.existsByProjectIdAndCitationKey(projectId, k)));
            }
            references.save(r);
            imported++;
        }
        return new ImportResult(imported, skipped);
    }

    /** Formats a single reference in the given style (used for previews and the bibliography). */
    public static ReferenceSummary summarize(Reference r, CitationStyle style, long knowledgeCount) {
        return summarize(r, style, knowledgeCount, 0);
    }

    public static ReferenceSummary summarize(
            Reference r, CitationStyle style, long knowledgeCount, long attachmentCount) {
        Formatted f = style.bibliography(r);
        List<Contributor> names = r.authors().isEmpty() ? r.editors() : r.authors();
        return new ReferenceSummary(
                r,
                ContributorLines.shortNames(names),
                f.html(),
                f.plain(),
                style.inText(r, null),
                knowledgeCount,
                attachmentCount);
    }

    /** Bibliography order: first author/editor (or title), year, title – German collation. */
    public static Comparator<Reference> bibliographyOrder() {
        Collator collator = Collator.getInstance(Locale.GERMAN);
        collator.setStrength(Collator.SECONDARY);
        return Comparator.<Reference, String>comparing(ReferenceService::sortName, collator)
                .thenComparing(r -> r.getYear() == null ? "" : r.getYear())
                .thenComparing(Reference::getTitle, collator);
    }

    private static String sortName(Reference r) {
        List<Contributor> people = r.authors().isEmpty() ? r.editors() : r.authors();
        if (people.isEmpty()) {
            return r.getTitle();
        }
        Contributor c = people.get(0);
        return c.lastName() + " " + (c.firstName() == null ? "" : c.firstName());
    }

    private CitationStyle style(Long projectId) {
        return projects.findById(projectId).map(Project::getCitationStyle).orElse(CitationStyle.APA7);
    }

    private Map<Long, Long> knowledgeCounts(Long projectId) {
        Map<Long, Long> counts = new LinkedHashMap<>();
        jdbc.sql("""
                        SELECT reference_entry_id, count(*) AS n FROM knowledge_item
                        WHERE project_id = :p AND reference_entry_id IS NOT NULL GROUP BY reference_entry_id
                        """)
                .param("p", projectId)
                .query((rs, n) -> counts.put(rs.getLong(1), rs.getLong(2)))
                .list();
        return counts;
    }

    private Map<Long, Long> attachmentCounts(Long projectId) {
        Map<Long, Long> counts = new LinkedHashMap<>();
        jdbc.sql(
                        "SELECT reference_entry_id, count(*) FROM reference_attachment WHERE project_id = :p GROUP BY reference_entry_id")
                .param("p", projectId)
                .query((rs, n) -> counts.put(rs.getLong(1), rs.getLong(2)))
                .list();
        return counts;
    }

    private static String normalizeIsbn(String isbn) {
        if (isbn == null) {
            return null;
        }
        String n = isbn.replaceAll("[^0-9Xx]", "").toUpperCase(Locale.ROOT);
        return n.isEmpty() ? null : n;
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    static Set<Long> ids(Collection<Long> ids) {
        return ids == null ? new HashSet<>() : new HashSet<>(ids);
    }
}
