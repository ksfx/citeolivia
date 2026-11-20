package com.citeolivia.library;

import com.citeolivia.library.citation.CitationStyle;
import com.citeolivia.security.ProjectAccess;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    /** Outline template for theses (Gliederungsvorlage). */
    static final List<String> THESIS_OUTLINE =
            List.of("Einleitung", "Theoretische Grundlagen", "Methodik", "Ergebnisse", "Diskussion", "Fazit");

    private final ProjectRepository projects;
    private final CategoryRepository categories;
    private final JdbcClient jdbc;

    ProjectService(ProjectRepository projects, CategoryRepository categories, JdbcClient jdbc) {
        this.projects = projects;
        this.categories = categories;
        this.jdbc = jdbc;
    }

    /** Project with counters for overview lists. */
    public record ProjectOverview(Project project, long references, long knowledgeItems) {}

    @PreAuthorize(ProjectAccess.OWNER)
    @Transactional(readOnly = true)
    public List<ProjectOverview> list(Long ownerId) {
        return projects.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(p ->
                        new ProjectOverview(p, count("reference_entry", p.getId()), count("knowledge_item", p.getId())))
                .toList();
    }

    @PreAuthorize(ProjectAccess.OWNER)
    @Transactional
    public long create(Long ownerId, String name, String description, boolean thesisOutline) {
        Project project = projects.save(Project.create(ownerId, name.strip(), blankToNull(description)));
        if (thesisOutline) {
            for (int i = 0; i < THESIS_OUTLINE.size(); i++) {
                categories.save(Category.create(project.getId(), null, THESIS_OUTLINE.get(i), i));
            }
        }
        return project.getId();
    }

    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional(readOnly = true)
    public Project get(Long projectId) {
        return projects.findById(projectId).orElseThrow(() -> new NotFoundException("Projekt"));
    }

    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional
    public void update(Long projectId, String name, String description, CitationStyle style) {
        Project project = get(projectId);
        project.update(name.strip(), blankToNull(description), style == null ? project.getCitationStyle() : style);
        projects.save(project);
    }

    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional
    public void changeCitationStyle(Long projectId, CitationStyle style) {
        Project project = get(projectId);
        project.update(project.getName(), project.getDescription(), style);
        projects.save(project);
    }

    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional
    public void delete(Long projectId) {
        projects.deleteById(projectId);
    }

    private long count(String table, Long projectId) {
        return jdbc.sql("SELECT count(*) FROM " + table + " WHERE project_id = :p")
                .param("p", projectId)
                .query(Long.class)
                .single();
    }

    static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
