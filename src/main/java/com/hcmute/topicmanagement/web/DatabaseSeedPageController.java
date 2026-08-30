package com.hcmute.topicmanagement.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
public class DatabaseSeedPageController {

    @GetMapping("/seed")
    @PreAuthorize("hasRole('ADMIN')")
    public String seedPage(Model model) {
        model.addAttribute("pageTitle", "Seed data");
        return "seed";
    }
}
