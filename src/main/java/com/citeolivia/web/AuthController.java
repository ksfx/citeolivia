package com.citeolivia.web;

import com.citeolivia.user.RegistrationCommand;
import com.citeolivia.user.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class AuthController {

    private final UserService userService;

    AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    String registerForm(Model model) {
        model.addAttribute("form", RegistrationForm.empty());
        return "auth/register";
    }

    /**
     * On submit, an already registered address is reported (decision of the user, 2026-11-20). There is
     * deliberately no live availability check while typing – that would allow cheap mass enumeration.
     */
    @PostMapping("/register")
    String register(
            @Valid @ModelAttribute("form") RegistrationForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return "auth/register";
        }
        if (userService
                .register(new RegistrationCommand(form.email(), form.displayName(), form.password()))
                .isEmpty()) {
            binding.rejectValue("email", "duplicate", "Diese E-Mail-Adresse ist bereits registriert.");
            model.addAttribute("emailTaken", true);
            return "auth/register";
        }
        redirect.addAttribute("registered", "");
        return "redirect:/login";
    }
}
