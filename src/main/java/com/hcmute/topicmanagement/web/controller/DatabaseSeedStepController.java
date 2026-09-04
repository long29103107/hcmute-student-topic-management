package com.hcmute.topicmanagement.web.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hcmute.topicmanagement.service.DatabaseSchemaService;
import com.hcmute.topicmanagement.service.DatabaseSeedService;
import com.hcmute.topicmanagement.service.SeedStepResult;

@RestController
@RequestMapping("/api/seed")
public class DatabaseSeedStepController {

    private final DatabaseSchemaService databaseSchemaService;
    private final DatabaseSeedService databaseSeedService;

    public DatabaseSeedStepController(
            DatabaseSchemaService databaseSchemaService,
            DatabaseSeedService databaseSeedService) {
        this.databaseSchemaService = databaseSchemaService;
        this.databaseSeedService = databaseSeedService;
    }

    @PostMapping("/ddl")
    public SeedStepResult createSchema() {
        return databaseSchemaService.createSchema();
    }

    @PostMapping("/permissions")
    public SeedStepResult seedPermissions() {
        return databaseSeedService.seedPermissionsStep();
    }

    @PostMapping("/roles")
    public SeedStepResult seedRoles() {
        return databaseSeedService.seedRolesStep();
    }

    @PostMapping("/role-permissions")
    public SeedStepResult seedRolePermissions() {
        return databaseSeedService.seedRolePermissionsStep();
    }

    @PostMapping("/users")
    public SeedStepResult seedUsers() {
        return databaseSeedService.seedUsersStep();
    }

    @PostMapping("/departments")
    public SeedStepResult seedDepartments() {
        return databaseSeedService.seedDepartmentsStep();
    }

    @PostMapping("/registration-periods")
    public SeedStepResult seedRegistrationPeriods() {
        return databaseSeedService.seedRegistrationPeriodsStep();
    }

}
