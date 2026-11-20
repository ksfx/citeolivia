package com.citeolivia.library;

import com.citeolivia.library.citation.CitationStyle;
import com.citeolivia.security.ProjectAccess;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Citations inside a text document (Word add-in): formats in-text citations and the bibliography of
 * exactly the references cited in the document, in the project's citation style.
 */
@Service
public class DocumentCitationService {

    /** Upper bound per request, protects against oversized documents/requests. */
    public static final int MAX_CITATIONS = 2000;

    private final ReferenceRepository references;
    private final ProjectRepository projects;

    DocumentCitationService(ReferenceRepository references, ProjectRepository projects) {
        this.references = references;
        this.projects = projects;
    }

    /** A citation in the document: the cited reference and an optional page range ("436" or "436-438"). */
    public record Citation(long referenceId, String pages) {}

    /**
     * In-text citations in the order of {@code citations}. An entry is {@code null} if the reference
     * does not exist in this project (deleted, or the document was linked to another project).
     */
    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional(readOnly = true)
    public List<String> inText(Long projectId, List<Citation> citations) {
        limit(citations.size());
        Map<Long, Reference> found =
                load(projectId, citations.stream().map(Citation::referenceId).toList());
        CitationStyle style = style(projectId);
        List<String> result = new ArrayList<>(citations.size());
        for (Citation c : citations) {
            Reference r = found.get(c.referenceId());
            result.add(r == null ? null : style.inText(r, blankToNull(c.pages())));
        }
        return result;
    }

    /** Bibliography entries of the cited references (each once), in bibliography order. */
    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional(readOnly = true)
    public List<ReferenceSummary> bibliography(Long projectId, Collection<Long> referenceIds) {
        limit(referenceIds.size());
        List<Reference> cited = new ArrayList<>(load(projectId, referenceIds).values());
        cited.sort(ReferenceService.bibliographyOrder());
        CitationStyle style = style(projectId);
        return cited.stream().map(r -> ReferenceService.summarize(r, style, 0)).toList();
    }

    private Map<Long, Reference> load(Long projectId, Collection<Long> ids) {
        Map<Long, Reference> found = new HashMap<>();
        for (Reference r : references.findAllById(new LinkedHashSet<>(ids))) {
            if (r.getProjectId().equals(projectId)) {
                found.put(r.getId(), r);
            }
        }
        return found;
    }

    private CitationStyle style(Long projectId) {
        return projects.findById(projectId).map(Project::getCitationStyle).orElse(CitationStyle.APA7);
    }

    private static void limit(int size) {
        if (size > MAX_CITATIONS) {
            throw new IllegalArgumentException("Zu viele Zitate in einer Anfrage (maximal " + MAX_CITATIONS + ").");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
