package com.citeolivia.web;

import com.citeolivia.library.Project;
import com.citeolivia.library.ProjectService;
import com.citeolivia.library.ReferenceService;
import com.citeolivia.library.ReferenceSummary;
import com.citeolivia.library.citation.CitationStyle;
import com.citeolivia.library.exchange.BibTexExporter;
import com.citeolivia.library.exchange.RisExporter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Bibliography (Literaturverzeichnis), citation style, import and export. */
@Controller
@RequestMapping("/projects/{projectId}")
class BibliographyController {

    private static final long MAX_IMPORT_BYTES = 5 * 1024 * 1024;

    private final ProjectService projects;
    private final ReferenceService references;

    BibliographyController(ProjectService projects, ReferenceService references) {
        this.projects = projects;
        this.references = references;
    }

    @ModelAttribute("project")
    Project project(@PathVariable Long projectId) {
        return projects.get(projectId);
    }

    @GetMapping("/bibliography")
    String bibliography(@PathVariable Long projectId, Model model) {
        model.addAttribute("entries", entries(projectId));
        return "bibliography/index";
    }

    /** htmx: changes the project's citation style and returns the re-formatted list. */
    @PostMapping("/citation-style")
    String changeStyle(@PathVariable Long projectId, @RequestParam CitationStyle style, Model model) {
        projects.changeCitationStyle(projectId, style);
        model.addAttribute("project", projects.get(projectId));
        model.addAttribute("entries", entries(projectId));
        model.addAttribute("styleChanged", true);
        return "bibliography/index :: list";
    }

    @GetMapping("/export.bib")
    ResponseEntity<String> exportBibTex(@PathVariable Long projectId) {
        return download(
                BibTexExporter.export(references.all(projectId)), fileName(projectId, "bib"), "application/x-bibtex");
    }

    @GetMapping("/export.ris")
    ResponseEntity<String> exportRis(@PathVariable Long projectId) {
        return download(
                RisExporter.export(references.all(projectId)),
                fileName(projectId, "ris"),
                "application/x-research-info-systems");
    }

    @GetMapping("/export.txt")
    ResponseEntity<String> exportText(@PathVariable Long projectId) {
        StringBuilder sb = new StringBuilder();
        entries(projectId).forEach(e -> sb.append(e.citationText()).append("\r\n\r\n"));
        return download(sb.toString(), fileName(projectId, "txt"), "text/plain");
    }

    @PostMapping("/import")
    String importBibTex(
            @PathVariable Long projectId,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String text,
            RedirectAttributes redirect)
            throws IOException {
        String input;
        if (file != null && !file.isEmpty()) {
            if (file.getSize() > MAX_IMPORT_BYTES) {
                redirect.addFlashAttribute("importError", "Die Datei ist zu groß (maximal 5 MB).");
                return "redirect:/projects/" + projectId + "/bibliography#import";
            }
            input = new String(file.getBytes(), StandardCharsets.UTF_8);
        } else {
            input = text == null ? "" : text;
        }
        if (input.isBlank()) {
            redirect.addFlashAttribute("importError", "Bitte eine BibTeX-Datei auswählen oder BibTeX-Text einfügen.");
            return "redirect:/projects/" + projectId + "/bibliography#import";
        }
        ReferenceService.ImportResult result = references.importBibTex(projectId, input);
        redirect.addFlashAttribute("importResult", result);
        return "redirect:/projects/" + projectId + "/bibliography#import";
    }

    private List<ReferenceSummary> entries(Long projectId) {
        CitationStyle style = projects.get(projectId).getCitationStyle();
        return references.all(projectId).stream()
                .map(r -> ReferenceService.summarize(r, style, 0))
                .toList();
    }

    private String fileName(Long projectId, String extension) {
        String name = projects.get(projectId)
                .getName()
                .replaceAll("[^\\p{L}\\p{N}_-]+", "-")
                .replaceAll("^-|-$", "");
        return (name.isEmpty() ? "literatur" : name) + "." + extension;
    }

    private static ResponseEntity<String> download(String body, String fileName, String contentType) {
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(fileName, StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .contentType(new MediaType(MediaType.parseMediaType(contentType), StandardCharsets.UTF_8))
                .body(body);
    }
}
