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
     * Same response for new and already registered addresses (no user enumeration): always back to the
     * login page with a neutral message. Only input errors (format, password rules) are shown.
     */
    @PostMapping("/register")
    String register(
            @Valid @ModelAttribute("form") RegistrationForm form, BindingResult binding, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return "auth/register";
        }
        userService.register(new RegistrationCommand(form.email(), form.displayName(), form.password()));
        redirect.addAttribute("registered", "");
        return "redirect:/login";
    }
}
