package com.citeolivia.web;

import com.citeolivia.library.CategoryService;
import com.citeolivia.library.KnowledgeService;
import com.citeolivia.library.KnowledgeType;
import com.citeolivia.library.KnowledgeView;
import com.citeolivia.library.Project;
import com.citeolivia.library.ProjectService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/projects/{projectId}")
class KnowledgeController {

    private final ProjectService projects;
    private final KnowledgeService knowledge;
    private final CategoryService categories;

    KnowledgeController(ProjectService projects, KnowledgeService knowledge, CategoryService categories) {
        this.projects = projects;
        this.knowledge = knowledge;
        this.categories = categories;
    }

    @ModelAttribute("project")
    Project project(@PathVariable Long projectId) {
        return projects.get(projectId);
    }

    /** Knowledge organizer (Wissensorganisator). */
    @GetMapping("/knowledge")
    String organizer(@PathVariable Long projectId, @RequestParam(required = false) Long category, Model model) {
        model.addAttribute("sections", knowledge.organizer(projectId, category));
        model.addAttribute("outline", categories.outline(projectId));
        model.addAttribute("category", category);
        if (!model.containsAttribute("knowledgeForm")) {
            KnowledgeForm form = KnowledgeForm.empty(KnowledgeType.THOUGHT);
            if (category != null) {
                form.getCategoryIds().add(category);
            }
            model.addAttribute("knowledgeForm", form);
        }
        return "knowledge/organizer";
    }

    /** Own thought without a source (from the organizer). */
    @PostMapping("/knowledge")
    String createThought(
            @PathVariable Long projectId,
            @Valid @ModelAttribute("knowledgeForm") KnowledgeForm form,
            BindingResult binding,
            Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("openThought", true);
            return organizer(projectId, null, model);
        }
        knowledge.create(projectId, null, form.toInput());
        return "redirect:/projects/" + projectId + "/knowledge";
    }

    @GetMapping(value = "/knowledge/export.md", produces = "text/markdown;charset=UTF-8")
    @ResponseBody
    ResponseEntity<String> exportMarkdown(@PathVariable Long projectId) {
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("wissen.md", StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .contentType(new MediaType("text", "markdown", StandardCharsets.UTF_8))
                .body(knowledge.organizerMarkdown(projectId));
    }

    /** htmx: adds a knowledge item to a reference; returns the whole knowledge section (list + empty form). */
    @PostMapping("/references/{referenceId}/knowledge")
    String createForReference(
            @PathVariable Long projectId,
            @PathVariable Long referenceId,
            @Valid @ModelAttribute("knowledgeForm") KnowledgeForm form,
            BindingResult binding,
            Model model) {
        if (!binding.hasErrors()) {
            knowledge.create(projectId, referenceId, form.toInput());
            model.addAttribute("knowledgeForm", KnowledgeForm.empty(form.getType()));
            model.addAttribute("added", true);
        }
        model.addAttribute("referenceId", referenceId);
        model.addAttribute("knowledgeItems", knowledge.forReference(projectId, referenceId));
        model.addAttribute("outline", categories.outline(projectId));
        return "references/detail :: knowledge";
    }

    /** htmx: a single card (e.g. after cancelling an edit). */
    @GetMapping("/knowledge/{itemId}")
    String card(
            @PathVariable Long projectId,
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "false") boolean source,
            Model model) {
        model.addAttribute("k", knowledge.view(projectId, itemId));
        model.addAttribute("showSource", source);
        return "knowledge/card :: card";
    }

    /** htmx: inline edit form replacing the card. */
    @GetMapping("/knowledge/{itemId}/edit")
    String edit(
            @PathVariable Long projectId,
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "false") boolean source,
            Model model) {
        KnowledgeView view = knowledge.view(projectId, itemId);
        model.addAttribute("k", view);
        model.addAttribute("showSource", source);
        model.addAttribute("editForm", KnowledgeForm.of(view.item()));
        model.addAttribute("outline", categories.outline(projectId));
        return "knowledge/card :: edit";
    }

    @PostMapping("/knowledge/{itemId}")
    String update(
            @PathVariable Long projectId,
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "false") boolean source,
            @Valid @ModelAttribute("editForm") KnowledgeForm form,
            BindingResult binding,
            Model model) {
        model.addAttribute("showSource", source);
        if (binding.hasErrors()) {
            model.addAttribute("k", knowledge.view(projectId, itemId));
            model.addAttribute("outline", categories.outline(projectId));
            return "knowledge/card :: edit";
        }
        knowledge.update(projectId, itemId, form.toInput());
        model.addAttribute("k", knowledge.view(projectId, itemId));
        return "knowledge/card :: card";
    }

    /** htmx: deletes and removes the card (empty response). */
    @PostMapping("/knowledge/{itemId}/delete")
    @ResponseBody
    String delete(@PathVariable Long projectId, @PathVariable Long itemId) {
        knowledge.delete(projectId, itemId);
        return "";
    }
}
