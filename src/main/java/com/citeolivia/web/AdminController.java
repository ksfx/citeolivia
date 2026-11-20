package com.citeolivia.web;

import com.citeolivia.user.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Access is enforced by method security on {@link UserService#listUsers()}. */
@Controller
class AdminController {

    private final UserService userService;

    AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/admin/users")
    String users(Model model) {
        model.addAttribute("users", userService.listUsers());
        return "admin/users";
    }
}
