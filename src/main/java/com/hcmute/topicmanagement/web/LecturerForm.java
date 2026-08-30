package com.hcmute.topicmanagement.web;

/** Form model for the lecturer directory and lecturer account flow. */
public class LecturerForm extends UserForm {
    public LecturerForm() {
        setAccountType("LECTURER");
    }
}
