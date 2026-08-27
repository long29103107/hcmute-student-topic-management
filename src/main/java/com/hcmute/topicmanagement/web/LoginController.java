package com.hcmute.topicmanagement.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    private final boolean googleOAuthEnabled;
    private final boolean rememberMeEnabled;

    public LoginController(
            @Value("${google.oauth.enabled:false}") boolean googleOAuthEnabled,
            @Value("${security.remember-me.enabled:false}") boolean rememberMeEnabled) {
        this.googleOAuthEnabled = googleOAuthEnabled;
        this.rememberMeEnabled = rememberMeEnabled;
    }

    @GetMapping("/login")
    public String login(
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "logout", required = false) String logout,
            Model model) {
        model.addAttribute("loginError", error != null);
        model.addAttribute("loggedOut", logout != null);
        model.addAttribute("googleLoginEnabled", googleOAuthEnabled);
        model.addAttribute("rememberMeEnabled", rememberMeEnabled);
        return "login";
    }
}
