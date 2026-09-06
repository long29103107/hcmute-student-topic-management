package com.hcmute.topicmanagement.web.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.hcmute.topicmanagement.service.DashboardService;

@Controller
public class HomeController {

    private final DashboardService dashboardService;

    public HomeController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        DashboardService.DashboardView dashboard = dashboardService.load(authentication.getName());
        model.addAttribute("applicationName", "HCMUTE Student Topic Management");
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("dashboard", dashboard.data());
        return "dashboard/" + dashboard.template();
    }
}
