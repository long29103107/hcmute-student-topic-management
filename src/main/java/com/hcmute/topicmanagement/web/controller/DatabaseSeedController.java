package com.hcmute.topicmanagement.web.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hcmute.topicmanagement.service.DatabaseSeedService;
import com.hcmute.topicmanagement.service.DatabaseSeedService.SeedResult;

@RestController
@RequestMapping("/api/admin/seed")
public class DatabaseSeedController {

    private final DatabaseSeedService databaseSeedService;

    public DatabaseSeedController(DatabaseSeedService databaseSeedService) {
        this.databaseSeedService = databaseSeedService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SeedResult resetAndSeed() {
        return databaseSeedService.resetAndSeed();
    }
}
