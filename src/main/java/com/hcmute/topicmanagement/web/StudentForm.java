package com.hcmute.topicmanagement.web;

/** Form model for the student directory and student account flow. */
public class StudentForm extends UserForm {
    public StudentForm() {
        setAccountType("STUDENT");
    }
}
