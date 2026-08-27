package com.hcmute.topicmanagement.web;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.SpringBootVersion;
import org.springframework.core.SpringVersion;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss XXX");

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("applicationName", "HCMUTE Student Topic Management");
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("activePeriod", "Project registration period — Semester 1");
        model.addAttribute("periodStatus", "Open");
        model.addAttribute("topicCount", 24);
        model.addAttribute("registrationCount", 12);
        model.addAttribute("groupCount", 8);
        model.addAttribute("announcementCount", 3);
        model.addAttribute("serverTime", OffsetDateTime.now(ZoneId.systemDefault()).format(TIME_FORMATTER));
        model.addAttribute("javaVersion", System.getProperty("java.version"));
        model.addAttribute("springVersion", SpringVersion.getVersion());
        model.addAttribute("springBootVersion", SpringBootVersion.getVersion());
        return "home";
    }
}
