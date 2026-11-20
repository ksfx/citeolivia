package com.citeolivia.web;

import com.citeolivia.library.AttachmentService;
import com.citeolivia.library.CategoryService;
import com.citeolivia.library.KnowledgeService;
import com.citeolivia.library.KnowledgeType;
import com.citeolivia.library.Project;
import com.citeolivia.library.ProjectService;
import com.citeolivia.library.Reference;
import com.citeolivia.library.ReferenceDraft;
import com.citeolivia.library.ReferenceService;
import com.citeolivia.library.ReferenceType;
import com.citeolivia.library.lookup.LookupService;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/projects/{projectId}/references")
class ReferenceController {

    private final ProjectService projects;
    private final ReferenceService references;
    private final CategoryService categories;
    private final KnowledgeService knowledge;
    private final LookupService lookup;
    private final AttachmentService attachments;

    ReferenceController(
            ProjectService projects,
            ReferenceService references,
            CategoryService categories,
            KnowledgeService knowledge,
            LookupService lookup,
            AttachmentService attachments) {
        this.projects = projects;
        this.references = references;
        this.categories = categories;
        this.knowledge = knowledge;
        this.lookup = lookup;
        this.attachments = attachments;
    }

    @ModelAttribute("project")
    Project project(@PathVariable Long projectId) {
        return projects.get(projectId);
    }

    @GetMapping
    String list(
            @PathVariable Long projectId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ReferenceType type,
            @RequestParam(required = false) Long category,
            @RequestHeader(value = Htmx.REQUEST_HEADER, required = false) String htmx,
            Model model) {
        model.addAttribute("references", references.search(projectId, new ReferenceService.Filter(q, type, category)));
        model.addAttribute("q", q);
        model.addAttribute("type", type);
        model.addAttribute("category", category);
        model.addAttribute("outline", categories.outline(projectId));
        model.addAttribute(
                "total",
                references.search(projectId, ReferenceService.Filter.none()).size());
        return htmx != null ? "references/list :: results" : "references/list";
    }

    @GetMapping("/new")
    String newForm(@PathVariable Long projectId, @RequestParam(required = false) ReferenceType type, Model model) {
        model.addAttribute("form", ReferenceForm.empty(type));
        return formPage(projectId, model, null);
    }

    /** htmx: re-renders the type-specific fields after the document type was changed. */
    @PostMapping("/fields")
    String fields(@PathVariable Long projectId, @ModelAttribute("form") ReferenceForm form, Model model) {
        model.addAttribute("outline", categories.outline(projectId));
        return "references/form :: fields";
    }

    /** htmx: looks up a DOI/ISBN and returns the prefilled form. */
    @PostMapping("/lookup")
    String lookup(@PathVariable Long projectId, @RequestParam String identifier, Model model) {
        LookupService.Result result = lookup.lookup(identifier);
        model.addAttribute("lookupMessage", result.message());
        model.addAttribute("lookupFound", result.found());
        ReferenceForm form =
                result.draft().map(ReferenceForm::of).orElseGet(() -> ReferenceForm.empty(ReferenceType.BOOK));
        result.draft()
                .flatMap(d -> references.findDuplicate(projectId, d, null))
                .ifPresent(dup -> model.addAttribute("duplicate", dup));
        model.addAttribute("form", form);
        model.addAttribute("outline", categories.outline(projectId));
        return "references/form :: referenceForm";
    }

    @PostMapping
    String create(
            @PathVariable Long projectId,
            @Valid @ModelAttribute("form") ReferenceForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return formPage(projectId, model, null);
        }
        long id = references.create(projectId, form.toDraft(), form.getCategoryIds());
        redirect.addFlashAttribute("message", "Titel gespeichert.");
        return "redirect:/projects/" + projectId + "/references/" + id;
    }

    @GetMapping("/{referenceId}")
    String detail(@PathVariable Long projectId, @PathVariable Long referenceId, Model model) {
        model.addAttribute("summary", references.summary(projectId, referenceId));
        model.addAttribute("outline", categories.outline(projectId));
        model.addAttribute("knowledgeItems", knowledge.forReference(projectId, referenceId));
        model.addAttribute("knowledgeForm", KnowledgeForm.empty(KnowledgeType.QUOTE));
        model.addAttribute("attachments", attachments.list(projectId, referenceId));
        return "references/detail";
    }

    @GetMapping("/{referenceId}/edit")
    String editForm(@PathVariable Long projectId, @PathVariable Long referenceId, Model model) {
        Reference reference = references.get(projectId, referenceId);
        model.addAttribute("form", ReferenceForm.of(reference));
        return formPage(projectId, model, reference);
    }

    @PostMapping("/{referenceId}")
    String update(
            @PathVariable Long projectId,
            @PathVariable Long referenceId,
            @Valid @ModelAttribute("form") ReferenceForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect) {
        Reference reference = references.get(projectId, referenceId);
        if (!binding.hasErrors()) {
            try {
                references.update(projectId, referenceId, form.toDraft(), form.getCategoryIds(), form.getCitationKey());
                redirect.addFlashAttribute("message", "Änderungen gespeichert.");
                return "redirect:/projects/" + projectId + "/references/" + referenceId;
            } catch (IllegalArgumentException e) {
                binding.rejectValue("citationKey", "invalid", e.getMessage());
            }
        }
        return formPage(projectId, model, reference);
    }

    @PostMapping("/{referenceId}/delete")
    ResponseEntity<Void> delete(
            @PathVariable Long projectId,
            @PathVariable Long referenceId,
            @RequestHeader(value = Htmx.REQUEST_HEADER, required = false) String htmx) {
        references.delete(projectId, referenceId);
        return Htmx.redirect(htmx != null, "/projects/" + projectId + "/references");
    }

    private String formPage(Long projectId, Model model, Reference existing) {
        model.addAttribute("outline", categories.outline(projectId));
        model.addAttribute("existing", existing);
        Object form = model.getAttribute("form");
        if (form instanceof ReferenceForm f && existing == null) {
            ReferenceDraft draft = f.getDraft();
            Optional<Reference> dup = draft.getDoi() == null && draft.getIsbn() == null
                    ? Optional.empty()
                    : references.findDuplicate(projectId, draft, null);
            dup.ifPresent(d -> model.addAttribute("duplicate", d));
        }
        return "references/form";
    }
}
