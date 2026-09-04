package com.hcmute.topicmanagement.web.form;

import java.util.ArrayList;
import java.util.List;

public class TopicSupervisorForm {

    private List<Long> lecturerIds = new ArrayList<>();

    public List<Long> getLecturerIds() {
        return lecturerIds;
    }

    public void setLecturerIds(List<Long> lecturerIds) {
        this.lecturerIds = lecturerIds == null ? new ArrayList<>() : new ArrayList<>(lecturerIds);
    }
}
