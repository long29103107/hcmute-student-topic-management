package com.hcmute.topicmanagement.web.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.hcmute.topicmanagement.service.DashboardService;
import com.hcmute.topicmanagement.service.AnnouncementService;

@Controller
public class HomeController {

    private final DashboardService dashboardService;
    private final AnnouncementService announcementService;

    public HomeController(DashboardService dashboardService, AnnouncementService announcementService) {
        this.dashboardService = dashboardService;
        this.announcementService = announcementService;
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
        model.addAttribute("announcements", announcementService.listPublished(authentication.getName()));
        return "dashboard/" + dashboard.template();
    }
}
