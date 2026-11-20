package com.citeolivia.web;

import com.citeolivia.library.Project;
import com.citeolivia.library.ProjectService;
import com.citeolivia.library.citation.CitationStyle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/projects/{projectId}")
class ProjectController {

    private final ProjectService projects;

    ProjectController(ProjectService projects) {
        this.projects = projects;
    }

    record SettingsForm(
            @NotBlank(message = "Bitte einen Namen angeben") @Size(max = 200)
            String name,

            @Size(max = 2000) String description,
            CitationStyle citationStyle) {}

    @ModelAttribute("project")
    Project project(@PathVariable Long projectId) {
        return projects.get(projectId);
    }

    @GetMapping
    String open(@PathVariable Long projectId) {
        return "redirect:/projects/" + projectId + "/references";
    }

    @GetMapping("/settings")
    String settings(@ModelAttribute("project") Project project, org.springframework.ui.Model model) {
        model.addAttribute(
                "form", new SettingsForm(project.getName(), project.getDescription(), project.getCitationStyle()));
        return "projects/settings";
    }

    @PostMapping("/settings")
    String saveSettings(
            @PathVariable Long projectId,
            @Validated @ModelAttribute("form") SettingsForm form,
            BindingResult binding,
            RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return "projects/settings";
        }
        projects.update(projectId, form.name(), form.description(), form.citationStyle());
        redirect.addFlashAttribute("message", "Einstellungen gespeichert.");
        return "redirect:/projects/" + projectId + "/settings";
    }

    @PostMapping("/delete")
    ResponseEntity<Void> delete(
            @PathVariable Long projectId, @RequestHeader(value = Htmx.REQUEST_HEADER, required = false) String htmx) {
        projects.delete(projectId);
        return Htmx.redirect(htmx != null, "/dashboard");
    }
}
