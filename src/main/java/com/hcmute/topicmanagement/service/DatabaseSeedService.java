package com.hcmute.topicmanagement.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.hcmute.topicmanagement.model.PermissionEntity;
import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.RolePermissionEntity;
import com.hcmute.topicmanagement.model.StudentProfileEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.repository.PermissionRepository;
import com.hcmute.topicmanagement.repository.RolePermissionRepository;
import com.hcmute.topicmanagement.repository.RoleRepository;
import com.hcmute.topicmanagement.repository.StudentProfileRepository;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.repository.UserRoleRepository;

@Service
public class DatabaseSeedService {

    private static final String LOCAL_PASSWORD_HASH =
            "$2a$10$VI1jWffo.Jg/04uyrX73TufViz1kOmzLTa9trum0bK61bf9gwh5cq";
    private static final String ADMIN_PASSWORD_HASH =
            "$2a$10$Tib/thYqs.dQRhB17iTfIO7qY0KKHBywglurPCoADhi9VRnjep79i";

    private static final List<String> TABLES = List.of(
            "evaluations",
            "reports",
            "topic_registrations",
            "group_members",
            "topic_supervisors",
            "student_groups",
            "topics",
            "registration_periods",
            "departments",
            "student_profiles",
            "user_roles",
            "role_permissions",
            "users",
            "permissions",
            "roles");

    private static final List<RoleSeed> ROLES = List.of(
            new RoleSeed("ADMIN", "Administrator", "Manage system accounts, roles, and permissions."),
            new RoleSeed("FACULTY_HEAD", "Faculty Head",
                    "Lecturer capabilities plus faculty workflow and registration management."),
            new RoleSeed("LECTURER", "Lecturer", "Propose topics, supervise students, and submit evaluations."),
            new RoleSeed("STUDENT", "Student", "Create groups, register topics, submit reports, and view group results."));

    private static final List<PermissionSeed> PERMISSIONS = List.of(
            new PermissionSeed("DASHBOARD_VIEW", "View dashboard", "Dashboard", "View the administration dashboard."),
            new PermissionSeed("USER_READ", "View accounts", "Users", "View the account list and account details."),
            new PermissionSeed("USER_CREATE", "Create account", "Users", "Create a new user account."),
            new PermissionSeed("USER_UPDATE", "Update account", "Users", "Update account information."),
            new PermissionSeed("USER_LOCK", "Lock/unlock account", "Users", "Lock or unlock an account."),
            new PermissionSeed("USER_DELETE", "Delete account", "Users", "Delete an account when it has no dependent records."),
            new PermissionSeed("USER_ROLE_ASSIGN", "Assign role to user", "Users", "Assign or remove a user role."),
            new PermissionSeed("ROLE_READ", "View roles", "Roles", "View the role list and current permissions."),
            new PermissionSeed("ROLE_UPDATE", "Manage role permissions", "Roles",
                    "Open the role permission assignment screen."),
            new PermissionSeed("PERMISSION_ASSIGN", "Assign permissions to role", "Roles",
                    "Enable or disable permissions for a role."),
            new PermissionSeed("DEPARTMENT_MANAGE", "Manage departments", "Departments",
                    "Create and update faculty departments."),
            new PermissionSeed("PERIOD_MANAGE", "Manage registration periods", "Registration periods",
                    "Create and manage topic registration periods."),
            new PermissionSeed("TOPIC_PROPOSE", "Propose topics", "Topics", "Create and update topic proposals."),
            new PermissionSeed("TOPIC_REVIEW", "Review topics", "Topics",
                    "Review, approve, reject, and publish topic proposals."),
            new PermissionSeed("TOPIC_VIEW", "View published topics", "Topics",
                    "View topics published for registration."),
            new PermissionSeed("GROUP_MANAGE", "Manage student groups", "Student groups",
                    "Create and manage student group membership."),
            new PermissionSeed("REGISTRATION_SUBMIT", "Submit topic registrations", "Registrations",
                    "Submit a topic registration for a student group."),
            new PermissionSeed("REPORT_SUBMIT", "Submit reports", "Reports",
                    "Submit reports for an approved topic registration."),
            new PermissionSeed("EVALUATION_SUBMIT", "Submit evaluations", "Evaluations",
                    "Submit evaluation scores and comments."),
            new PermissionSeed("REGISTRATION_REVIEW", "Review topic registrations", "Registrations",
                    "Approve or reject student topic registrations."),
            new PermissionSeed("RESULT_VIEW", "View results", "Results",
                    "View published results for permitted users."));

    private static final List<String> LECTURER_PERMISSIONS = List.of(
            "TOPIC_PROPOSE", "TOPIC_VIEW", "EVALUATION_SUBMIT", "RESULT_VIEW");

    private static final Map<String, List<String>> ROLE_PERMISSIONS = Map.of(
            "ADMIN", List.of("DASHBOARD_VIEW", "USER_READ", "USER_CREATE", "USER_UPDATE", "USER_LOCK",
                    "USER_DELETE", "USER_ROLE_ASSIGN", "ROLE_READ", "ROLE_UPDATE", "PERMISSION_ASSIGN"),
            "FACULTY_HEAD", withLecturerPermissions(
                    "DEPARTMENT_MANAGE", "PERIOD_MANAGE", "TOPIC_REVIEW", "REGISTRATION_REVIEW"),
            "LECTURER", LECTURER_PERMISSIONS,
            "STUDENT", List.of("TOPIC_VIEW", "GROUP_MANAGE", "REGISTRATION_SUBMIT", "REPORT_SUBMIT", "RESULT_VIEW"));

    private static final List<UserSeed> USERS = List.of(
            new UserSeed("admin", "System Administrator", "admin@hcmute.local", "ADMIN"),
            new UserSeed("faculty.head.test", "Faculty Head Test", "faculty.head.test@hcmute.local", "FACULTY_HEAD"),
            new UserSeed("lecturer.test", "Lecturer Test", "lecturer.test@hcmute.local", "LECTURER"),
            new UserSeed("student.test", "Student Test", "24110000@student.hcmute.edu.vn", "STUDENT"));

    private static List<String> withLecturerPermissions(String... additionalPermissions) {
        List<String> permissions = new ArrayList<>(LECTURER_PERMISSIONS);
        permissions.addAll(List.of(additionalPermissions));
        return List.copyOf(permissions);
    }

    private final DataSource dataSource;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final StudentProfileRepository studentProfileRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public DatabaseSeedService(
            DataSource dataSource,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            StudentProfileRepository studentProfileRepository) {
        this.dataSource = dataSource;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    @Transactional
    public SeedResult resetAndSeed() {
        truncateAllTables();

        Map<String, RoleEntity> roles = seedRoles();
        Map<String, PermissionEntity> permissions = seedPermissions();
        seedRolePermissions(roles, permissions);
        Map<String, UserEntity> users = seedUsers(roles);
        seedStudentProfile(users.get("student.test"));
        entityManager.clear();

        return new SeedResult(
                ROLES.size(),
                PERMISSIONS.size(),
                USERS.size(),
                1,
                LocalDateTime.now());
    }

    @Transactional
    public SeedStepResult seedPermissionsStep() {
        List<PermissionEntity> permissions = PERMISSIONS.stream()
                .map(this::upsertPermission)
                .toList();
        permissionRepository.saveAllAndFlush(permissions);
        return new SeedStepResult("permissions", permissions.size(), LocalDateTime.now());
    }

    @Transactional
    public SeedStepResult seedRolesStep() {
        List<RoleEntity> roles = ROLES.stream()
                .map(this::upsertRole)
                .toList();
        roleRepository.saveAllAndFlush(roles);
        return new SeedStepResult("roles", roles.size(), LocalDateTime.now());
    }

    @Transactional
    public SeedStepResult seedRolePermissionsStep() {
        Map<String, RoleEntity> roles = roleRepository.findAll().stream()
                .collect(Collectors.toMap(RoleEntity::getCode, Function.identity()));
        Map<String, PermissionEntity> permissions = permissionRepository.findAll().stream()
                .collect(Collectors.toMap(PermissionEntity::getCode, Function.identity()));

        if (!roles.keySet().containsAll(ROLE_PERMISSIONS.keySet())
                || !permissions.keySet().containsAll(ROLE_PERMISSIONS.values().stream()
                        .flatMap(List::stream)
                        .toList())) {
            throw new IllegalStateException("Seed roles and permissions before role-permissions.");
        }

        rolePermissionRepository.deleteAllInBatch();
        List<RolePermissionEntity> rolePermissions = ROLE_PERMISSIONS.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(permissionCode -> new RolePermissionEntity(
                                roles.get(entry.getKey()), permissions.get(permissionCode))))
                .toList();
        rolePermissionRepository.saveAllAndFlush(rolePermissions);
        return new SeedStepResult("role-permissions", rolePermissions.size(), LocalDateTime.now());
    }

    @Transactional
    public SeedStepResult seedUsersStep() {
        Map<String, RoleEntity> roles = roleRepository.findAll().stream()
                .collect(Collectors.toMap(RoleEntity::getCode, Function.identity()));
        if (!roles.keySet().containsAll(USERS.stream().map(UserSeed::roleCode).toList())) {
            throw new IllegalStateException("Seed roles before users.");
        }

        List<UserEntity> users = USERS.stream()
                .map(user -> upsertUser(user, roles.get(user.roleCode())))
                .toList();
        userRepository.saveAllAndFlush(users);
        return new SeedStepResult("users", users.size(), LocalDateTime.now());
    }

    @Transactional
    public SeedStepResult seedStudentProfilesStep() {
        UserEntity student = userRepository.findByLoginIdentifier("student.test")
                .orElseThrow(() -> new IllegalStateException("Seed users before student-profiles."));
        StudentProfileEntity profile = studentProfileRepository.findByUser_Id(student.getId())
                .orElseGet(() -> new StudentProfileEntity(
                        student, "24110000", "2024-2025", "Information Technology", "22110CL1"));
        profile.setStudentCode("24110000");
        profile.setAcademicYear("2024-2025");
        profile.setMajor("Information Technology");
        profile.setClassName("22110CL1");
        studentProfileRepository.saveAndFlush(profile);
        return new SeedStepResult("student-profiles", 1, LocalDateTime.now());
    }

    private void truncateAllTables() {
        boolean h2 = databaseProductName().contains("h2");
        entityManager.clear();
        entityManager.createNativeQuery(h2
                ? "SET REFERENTIAL_INTEGRITY FALSE"
                : "SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
        try {
            for (String table : TABLES) {
                entityManager.createNativeQuery("TRUNCATE TABLE " + table).executeUpdate();
            }
        } finally {
            entityManager.createNativeQuery(h2
                    ? "SET REFERENTIAL_INTEGRITY TRUE"
                    : "SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
        }
    }

    private String databaseProductName() {
        try (var connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot detect the configured database before seeding.", exception);
        }
    }

    private Map<String, RoleEntity> seedRoles() {
        List<RoleEntity> savedRoles = roleRepository.saveAllAndFlush(ROLES.stream()
                .map(role -> new RoleEntity(role.code(), role.name(), role.description()))
                .toList());
        return savedRoles.stream().collect(Collectors.toMap(RoleEntity::getCode, Function.identity()));
    }

    private RoleEntity upsertRole(RoleSeed role) {
        RoleEntity entity = roleRepository.findByCode(role.code())
                .orElseGet(() -> new RoleEntity(role.code(), role.name(), role.description()));
        entity.setCode(role.code());
        entity.setName(role.name());
        entity.setDescription(role.description());
        entity.setActive(true);
        return entity;
    }

    private Map<String, PermissionEntity> seedPermissions() {
        List<PermissionEntity> savedPermissions = permissionRepository.saveAllAndFlush(PERMISSIONS.stream()
                .map(permission -> {
                    PermissionEntity entity = new PermissionEntity(
                            permission.code(), permission.name(), permission.permissionGroup());
                    entity.setDescription(permission.description());
                    return entity;
                })
                .toList());
        return savedPermissions.stream().collect(Collectors.toMap(PermissionEntity::getCode, Function.identity()));
    }

    private PermissionEntity upsertPermission(PermissionSeed permission) {
        PermissionEntity entity = permissionRepository.findByCode(permission.code())
                .orElseGet(() -> new PermissionEntity(
                        permission.code(), permission.name(), permission.permissionGroup()));
        entity.setCode(permission.code());
        entity.setName(permission.name());
        entity.setPermissionGroup(permission.permissionGroup());
        entity.setDescription(permission.description());
        entity.setActive(true);
        return entity;
    }

    private void seedRolePermissions(
            Map<String, RoleEntity> roles,
            Map<String, PermissionEntity> permissions) {
        List<RolePermissionEntity> rolePermissions = ROLE_PERMISSIONS.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(permissionCode -> new RolePermissionEntity(
                                roles.get(entry.getKey()), permissions.get(permissionCode))))
                .toList();
        rolePermissionRepository.saveAllAndFlush(rolePermissions);
    }

    private Map<String, UserEntity> seedUsers(Map<String, RoleEntity> roles) {
        List<UserEntity> savedUsers = userRepository.saveAllAndFlush(USERS.stream()
                .map(user -> {
                    UserEntity entity = new UserEntity(
                            user.loginIdentifier(), user.fullName(), passwordHashFor(user));
                    entity.setEmailOrCode(user.emailOrCode());
                    return entity;
                })
                .toList());

        List<UserRoleEntity> userRoles = savedUsers.stream()
                .map(user -> new UserRoleEntity(user, roles.get(USERS.stream()
                        .filter(seed -> seed.loginIdentifier().equals(user.getLoginIdentifier()))
                        .findFirst()
                        .orElseThrow()
                        .roleCode())))
                .toList();
        userRoleRepository.saveAllAndFlush(userRoles);
        return savedUsers.stream().collect(Collectors.toMap(UserEntity::getLoginIdentifier, Function.identity()));
    }

    private UserEntity upsertUser(UserSeed user, RoleEntity role) {
        UserEntity entity = userRepository.findByLoginIdentifier(user.loginIdentifier())
                .orElseGet(() -> new UserEntity(
                        user.loginIdentifier(), user.fullName(), passwordHashFor(user)));
        entity.setLoginIdentifier(user.loginIdentifier());
        entity.setFullName(user.fullName());
        entity.setEmailOrCode(user.emailOrCode());
        entity.setPasswordHash(passwordHashFor(user));
        entity.setActive(true);

        userRepository.saveAndFlush(entity);
        UserRoleEntity userRole = userRoleRepository.findByUser_Id(entity.getId()).stream()
                .filter(existing -> existing.getRole().getId().equals(role.getId()))
                .findFirst()
                .orElseGet(() -> new UserRoleEntity(entity, role));
        userRole.setActive(true);
        userRoleRepository.saveAndFlush(userRole);
        return entity;
    }

    private String passwordHashFor(UserSeed user) {
        return "admin".equals(user.loginIdentifier()) ? ADMIN_PASSWORD_HASH : LOCAL_PASSWORD_HASH;
    }

    private void seedStudentProfile(UserEntity student) {
        StudentProfileEntity profile = new StudentProfileEntity(
                student, "24110000", "2024-2025", "Information Technology", "22110CL1");
        studentProfileRepository.saveAndFlush(profile);
    }

    public record SeedResult(int roles, int permissions, int users, int studentProfiles, LocalDateTime completedAt) {
    }

    private record RoleSeed(String code, String name, String description) {
    }

    private record PermissionSeed(String code, String name, String permissionGroup, String description) {
    }

    private record UserSeed(String loginIdentifier, String fullName, String emailOrCode, String roleCode) {
    }
}
