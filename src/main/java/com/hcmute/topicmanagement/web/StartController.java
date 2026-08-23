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
public class StartController {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss XXX");

    @GetMapping({"/", "/start"})
    public String start(Model model) {
        model.addAttribute("applicationName", "HCMUTE Student Topic Management");
        model.addAttribute("status", "RUNNING");
        model.addAttribute("javaVersion", System.getProperty("java.version"));
        model.addAttribute("springVersion", SpringVersion.getVersion());
        model.addAttribute("springBootVersion", SpringBootVersion.getVersion());
        model.addAttribute("serverTime", OffsetDateTime.now(ZoneId.systemDefault()).format(TIME_FORMATTER));
        return "start";
    }
}
