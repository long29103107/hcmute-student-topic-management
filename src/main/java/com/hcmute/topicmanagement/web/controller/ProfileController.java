package com.hcmute.topicmanagement.web.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.service.UserManagementService;
import com.hcmute.topicmanagement.web.form.PasswordChangeForm;

@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final UserManagementService userManagementService;

    public ProfileController(UserRepository userRepository, UserManagementService userManagementService) {
        this.userRepository = userRepository;
        this.userManagementService = userManagementService;
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public String profile(Authentication authentication, Model model) {
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new PasswordChangeForm());
        }
        return renderProfile(authentication, model);
    }

    @PostMapping("/profile/password")
    @PreAuthorize("isAuthenticated()")
    public String changePassword(
            Authentication authentication,
            @Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!samePassword(form.getPassword(), form.getPasswordConfirmation())) {
            bindingResult.rejectValue("passwordConfirmation", "profile.passwordConfirmation",
                    "Passwords do not match.");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("passwordChangeError", true);
            return renderProfile(authentication, model);
        }
        try {
            userManagementService.changeOwnPassword(authentication.getName(), form.getPassword());
            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully.");
            return "redirect:/profile";
        } catch (UserManagementService.UserValidationException exception) {
            bindingResult.reject("profile.invalid", exception.getMessage());
            model.addAttribute("passwordChangeError", true);
            return renderProfile(authentication, model);
        }
    }

    private String renderProfile(Authentication authentication, Model model) {
        if (!model.containsAttribute("passwordChangeError")) {
            model.addAttribute("passwordChangeError", false);
        }
        UserEntity profileUser = userRepository
                .findByEmailIgnoreCaseWithRolesAndDepartment(authentication.getName())
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new IllegalStateException("The authenticated profile could not be loaded."));
        model.addAttribute("pageTitle", "My Profile");
        model.addAttribute("profileUser", profileUser);
        return "profile";
    }

    private static boolean samePassword(String password, String confirmation) {
        String left = password == null ? "" : password;
        String right = confirmation == null ? "" : confirmation;
        return left.equals(right);
    }
}
