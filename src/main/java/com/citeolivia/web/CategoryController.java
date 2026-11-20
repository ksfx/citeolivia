package com.citeolivia.web;

import com.citeolivia.library.CategoryService;
import com.citeolivia.library.Project;
import com.citeolivia.library.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Outline (Gliederung / Kategoriensystem). */
@Controller
@RequestMapping("/projects/{projectId}/categories")
class CategoryController {

    private final ProjectService projects;
    private final CategoryService categories;

    CategoryController(ProjectService projects, CategoryService categories) {
        this.projects = projects;
        this.categories = categories;
    }

    @ModelAttribute("project")
    Project project(@PathVariable Long projectId) {
        return projects.get(projectId);
    }

    @GetMapping
    String index(@PathVariable Long projectId, Model model) {
        model.addAttribute("outline", categories.outline(projectId));
        return "categories/index";
    }

    @PostMapping
    String create(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long parentId,
            @RequestParam String name,
            RedirectAttributes redirect) {
        if (name.isBlank()) {
            redirect.addFlashAttribute("error", "Bitte einen Namen für die Kategorie angeben.");
        } else if (name.strip().length() > 200) {
            redirect.addFlashAttribute("error", "Der Name darf höchstens 200 Zeichen lang sein.");
        } else {
            categories.create(projectId, parentId, name);
        }
        return "redirect:/projects/" + projectId + "/categories";
    }

    @PostMapping("/{categoryId}/rename")
    String rename(
            @PathVariable Long projectId,
            @PathVariable Long categoryId,
            @RequestParam String name,
            RedirectAttributes redirect) {
        if (name.isBlank() || name.strip().length() > 200) {
            redirect.addFlashAttribute("error", "Der Name muss 1 bis 200 Zeichen lang sein.");
        } else {
            categories.rename(projectId, categoryId, name);
        }
        return "redirect:/projects/" + projectId + "/categories";
    }

    @PostMapping("/{categoryId}/move")
    String move(@PathVariable Long projectId, @PathVariable Long categoryId, @RequestParam int direction) {
        categories.move(projectId, categoryId, direction);
        return "redirect:/projects/" + projectId + "/categories";
    }

    @PostMapping("/{categoryId}/delete")
    ResponseEntity<Void> delete(
            @PathVariable Long projectId,
            @PathVariable Long categoryId,
            @RequestHeader(value = Htmx.REQUEST_HEADER, required = false) String htmx) {
        categories.delete(projectId, categoryId);
        return Htmx.redirect(htmx != null, "/projects/" + projectId + "/categories");
    }
}
