package com.hcmute.topicmanagement.web.controller;

import java.time.LocalDateTime;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hcmute.topicmanagement.service.TopicPublicationService;

@Controller
public class TopicCatalogController {

    private final TopicPublicationService topicPublicationService;

    public TopicCatalogController(TopicPublicationService topicPublicationService) {
        this.topicPublicationService = topicPublicationService;
    }

    @GetMapping("/topics")
    @PreAuthorize("hasAuthority('TOPIC_VIEW')")
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long periodId,
            @RequestParam(defaultValue = "topic") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {
        LocalDateTime now = LocalDateTime.now();
        TopicPublicationService.PublishedTopicPage topicPage = topicPublicationService.listPublishedPage(
                search, departmentId, periodId, page, size, sort, direction, now);
        model.addAttribute("pageTitle", "Published topics");
        model.addAttribute("topicPage", topicPage);
        model.addAttribute("topics", topicPage.getTopics());
        model.addAttribute("topicSearch", topicPage.getSearch());
        model.addAttribute("topicSort", topicPage.getSort());
        model.addAttribute("topicDirection", topicPage.getDirection());
        model.addAttribute("departments", topicPublicationService.listDepartmentOptions());
        model.addAttribute("periods", topicPublicationService.listPeriodOptions(now));
        return "topics";
    }
}
