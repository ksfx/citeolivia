package com.citeolivia.web;

import com.citeolivia.library.DocumentCitationService;
import com.citeolivia.library.ProjectService;
import com.citeolivia.library.ReferenceService;
import com.citeolivia.library.ReferenceSummary;
import com.citeolivia.library.citation.CitationStyle;
import com.citeolivia.security.CurrentUser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Word add-in: manifest, login and task pane. The task pane is server-rendered (Thymeleaf + htmx);
 * only the document operations in {@code static/js/word-addin.js} (Office.js) exchange data via
 * {@code /citations} (JSON) and {@code /bibliography} (HTML for Word).
 */
@Controller
@RequestMapping("/word")
class WordAddinController {

    private static final int MAX_RESULTS = 30;

    private final ProjectService projects;
    private final ReferenceService references;
    private final DocumentCitationService citations;
    private final String publicUrl;

    WordAddinController(
            ProjectService projects,
            ReferenceService references,
            DocumentCitationService citations,
            @Value("${citeolivia.public-url}") String publicUrl) {
        this.projects = projects;
        this.references = references;
        this.citations = citations;
        this.publicUrl = publicUrl;
    }

    record CitationsRequest(List<DocumentCitationService.Citation> citations) {}

    record CitationsResponse(List<String> texts) {}

    record BibliographyRequest(List<Long> referenceIds) {}

    @GetMapping(value = "/manifest.xml")
    ResponseEntity<String> manifest() {
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("citeolivia-word-addin.xml")
                                .build()
                                .toString())
                .contentType(new MediaType(MediaType.APPLICATION_XML, StandardCharsets.UTF_8))
                .body(WordManifest.xml(publicUrl));
    }

    @GetMapping("/login")
    String login() {
        return "word/login";
    }

    @GetMapping({"", "/", "/taskpane"})
    String taskpane(@AuthenticationPrincipal CurrentUser user, Model model) {
        model.addAttribute("user", user);
        model.addAttribute("projects", projects.list(user.userId()));
        return "word/taskpane";
    }

    @GetMapping("/projects/{projectId}/panel")
    String panel(@PathVariable Long projectId, Model model) {
        return panelFragment(projectId, model);
    }

    @GetMapping("/projects/{projectId}/search")
    String search(@PathVariable Long projectId, @RequestParam(required = false) String q, Model model) {
        List<ReferenceSummary> found = references.search(projectId, new ReferenceService.Filter(q, null, null));
        model.addAttribute("results", found.stream().limit(MAX_RESULTS).toList());
        model.addAttribute("more", Math.max(0, found.size() - MAX_RESULTS));
        model.addAttribute("q", q);
        return "word/taskpane :: results";
    }

    /** htmx: changes the citation style; the JS then updates all citations in the document. */
    @PostMapping("/projects/{projectId}/style")
    String changeStyle(@PathVariable Long projectId, @RequestParam CitationStyle style, Model model) {
        projects.changeCitationStyle(projectId, style);
        model.addAttribute("styleChanged", true);
        return panelFragment(projectId, model);
    }

    @PostMapping("/projects/{projectId}/citations")
    @ResponseBody
    CitationsResponse citations(@PathVariable Long projectId, @RequestBody CitationsRequest request) {
        List<DocumentCitationService.Citation> list = request.citations() == null ? List.of() : request.citations();
        return new CitationsResponse(citations.inText(projectId, list));
    }

    @PostMapping("/projects/{projectId}/bibliography")
    String bibliography(@PathVariable Long projectId, @RequestBody BibliographyRequest request, Model model) {
        List<Long> ids = request.referenceIds() == null ? List.of() : request.referenceIds();
        model.addAttribute("entries", citations.bibliography(projectId, ids));
        return "word/bibliography :: entries";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<String> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(e.getMessage());
    }

    private String panelFragment(Long projectId, Model model) {
        model.addAttribute("project", projects.get(projectId));
        model.addAttribute("citationStyles", CitationStyle.values());
        return "word/taskpane :: panel";
    }
}
