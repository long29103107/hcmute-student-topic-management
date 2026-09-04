package com.hcmute.topicmanagement.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Form model for creating and editing faculty departments. */
public class DepartmentForm {

    @NotBlank(message = "Department code is required.")
    @Size(max = 30, message = "Department code must be at most 30 characters.")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$",
            message = "Department code may contain letters, numbers, hyphens and underscores only.")
    private String code;

    @NotBlank(message = "Department name is required.")
    @Size(max = 150, message = "Department name must be at most 150 characters.")
    @Pattern(regexp = ".*[\\p{L}\\p{N}].*", message = "Department name must contain a letter or number.")
    private String name;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
