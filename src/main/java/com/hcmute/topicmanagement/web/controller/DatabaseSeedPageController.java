package com.hcmute.topicmanagement.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
public class DatabaseSeedPageController {

    @GetMapping("/seed")
    public String seedPage(Model model) {
        model.addAttribute("pageTitle", "Seed data");
        return "seed";
    }
}
