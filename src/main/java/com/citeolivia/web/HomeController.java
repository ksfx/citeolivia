package com.citeolivia.web;

import com.citeolivia.library.ProjectService;
import com.citeolivia.security.CurrentUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
class HomeController {

    private final ProjectService projects;

    HomeController(ProjectService projects) {
        this.projects = projects;
    }

    record NewProjectForm(
            @NotBlank(message = "Bitte einen Namen angeben") @Size(max = 200)
            String name,

            @Size(max = 2000) String description,
            boolean thesisOutline) {

        static NewProjectForm empty() {
            return new NewProjectForm("", "", true);
        }
    }

    @GetMapping("/")
    String index() {
        return "index";
    }

    /** Public installation guide for the Word add-in. */
    @GetMapping("/word-add-in")
    String wordAddinGuide() {
        return "help/word-addin";
    }

    @GetMapping("/dashboard")
    String dashboard(@AuthenticationPrincipal CurrentUser user, Model model) {
        model.addAttribute("form", NewProjectForm.empty());
        return dashboardPage(user, model);
    }

    @PostMapping("/projects")
    String createProject(
            @AuthenticationPrincipal CurrentUser user,
            @Validated @ModelAttribute("form") NewProjectForm form,
            BindingResult binding,
            Model model) {
        if (binding.hasErrors()) {
            return dashboardPage(user, model);
        }
        long id = projects.create(user.userId(), form.name(), form.description(), form.thesisOutline());
        return "redirect:/projects/" + id + "/references";
    }

    private String dashboardPage(CurrentUser user, Model model) {
        model.addAttribute("user", user);
        model.addAttribute("projects", projects.list(user.userId()));
        return "dashboard";
    }
}
