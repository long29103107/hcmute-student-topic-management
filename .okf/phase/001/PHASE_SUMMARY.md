---
phase: 001
title: Identity and Access — Users, Roles, Permissions and Login
status: complete
created_at: 2026-08-20
updated_at: 2026-08-30
current_task: 001_006
task_count: 6
done_count: 6
depends_on: []
---

# Phase 001 Summary

## Vision alignment

This phase delivers the active `Identity and access` milestone in
[`PRODUCT_VISION.md`](../../../PRODUCT_VISION.md). No task from later product
areas may be added here without an explicit user decision.

## Phase Goal

Complete Login, User, and seed-managed Role/Permission modules, including
server-side authorization, as one usable identity-and-access foundation.

## Phase Done Criteria

- Login/logout/session and protected-route behavior are verified.
- Users can be safely created, viewed, edited, activated/deactivated or deleted
  according to the data policy; credentials are securely reset.
- Student creation requires a unique MSSV, uses it as the login identifier,
  auto-assigns `STUDENT`, generates a readonly
  `<MSSV>@student.hcmute.edu.vn` email and leaves the password unset until an
  administrator sets it. Lecturer creation auto-assigns `LECTURER` and accepts
  a manually entered email/password. The directory separates student,
  lecturer-capability view (including Faculty Head accounts) and reuses the
  same account modal template.
- Student MSSV is stored as the immutable unique `users.login_identifier`; the
  revised schema has no separate Student Profile table.
- Users without a configured password cannot authenticate; setting a password
  is an explicit edit/reset action.
- `FACULTY_HEAD` and `LECTURER` remain shared access roles in `roles` and
  `user_roles`; the default Faculty Head permission bundle includes all
  Lecturer permissions plus faculty/registration workflow permissions. The
  bundle is explicit in `role_permissions`, and Faculty Head assignment is
  separate from Add Lecturer.
- Roles and permissions are system-managed seed data; administrators can view
  the catalog and maintain role-permission assignments, while catalog changes
  require a reviewed seed/schema change rather than runtime CRUD.
- Role/permission changes are enforced by server-side checks, not hidden UI.
- All mutations have server validation, authorization and meaningful feedback.
- Local identity fixtures can be reset reproducibly through the seed pipeline;
  anonymous access is supported for local bootstrap, while shared/staging/
  production deployments must disable it and require an authenticated admin.
  The API truncates the fixed application table set before reseeding.
- Relevant tests and `mvn test` pass; skipped environment checks are recorded.

## Scope

In: User and Login CRUD/authorization, plus seed-managed Role/Permission
catalog and role-permission assignment.

Out: departments, periods, topics, groups, registrations, reports, evaluations,
results, announcements, dashboards, email and audit logging.

## Task Index

| Task | Title | Status | Done At |
|---|---|---|---|
| 001_001 | Reconcile identity baseline and persistence contracts | completed | 2026-08-30 |
| 001_002 | Login, logout, session and protected-route behavior | completed | 2026-08-30 |
| 001_003 | Role-aware User CRUD, MSSV handling, status and credential reset | completed | 2026-08-30 |
| 001_004 | Seed-managed role/permission catalog and assignment policy | completed | 2026-08-30 |
| 001_005 | Permission enforcement and cross-route authorization audit | completed | 2026-08-30 |
| 001_006 | Identity-and-access verification and phase closure | completed | 2026-08-30 |

## Current Task

`001_006` — run final verification and close the identity-and-access phase.
Do not start academic-workflow work in this phase.

The identity seed source of truth is `DatabaseSeedService`; the only database
script kept in the repository is `database/1.ddl.sql`. Existing databases must
be recreated or brought to the current schema before starting the application;
the deleted legacy `database/4.update-ddl.sql` is not part of the pipeline.
Shared admin feedback now uses the reusable layout toast fragment for success,
warning and error flash messages; form validation feedback remains inline.
Student and lecturer directories now have typed canonical routes and separate
controller/form models; the combined `/admin/users` route remains only for
legacy compatibility.

## Next Task Proposal

Phase 001 is complete. Move academic workflow work to the next explicitly
selected milestone. The existing role/permission catalog remains seed-managed
by design; do not create runtime CRUD tickets for it.

## Verification Evidence

- `mvn test` — pass, 42 tests, 0 failures, 0 errors, 0 skipped.
- `mvn package -DskipTests` — pass; produced
  `target/student-topic-management-0.0.1-SNAPSHOT.jar` as an executable
  Spring Boot JAR.
- `git diff --check` — pass.
- MySQL and deployed-browser/Tomcat smoke checks — not run in this workspace;
  the automated suite uses H2 and the runbook records the required environment
  checks for handoff.

## Task Notes

Each task note must use `.okf/templates/task.md`, begin with `Vision alignment`,
and reference the specific row in `PRODUCT_VISION.md` it advances.
