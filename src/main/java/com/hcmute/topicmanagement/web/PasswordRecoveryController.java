package com.hcmute.topicmanagement.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PasswordRecoveryController {

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }
}
