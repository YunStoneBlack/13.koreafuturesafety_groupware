package com.kfsc21c.groupware.common;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal UserDetails principal, Model model) {
        model.addAttribute("username", principal != null ? principal.getUsername() : "guest");
        return "home";
    }
}
