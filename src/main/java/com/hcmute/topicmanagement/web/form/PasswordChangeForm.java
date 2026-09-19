package com.hcmute.topicmanagement.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordChangeForm {

    @NotBlank(message = "New password is required.")
    @Size(max = 72, message = "Password must be 72 characters or fewer.")
    private String password;

    @NotBlank(message = "Password confirmation is required.")
    @Size(max = 72, message = "Password confirmation must be 72 characters or fewer.")
    private String passwordConfirmation;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirmation() {
        return passwordConfirmation;
    }

    public void setPasswordConfirmation(String passwordConfirmation) {
        this.passwordConfirmation = passwordConfirmation;
    }
}
