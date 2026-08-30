---
phase: 001
title: Identity and Access — Users, Roles, Permissions and Login
status: in_progress
created_at: 2026-08-20
updated_at: 2026-08-29
current_task: 001_001
task_count: 6
done_count: 0
depends_on: []
---

# Phase 001 Summary

## Vision alignment

This phase delivers the active `Identity and access` milestone in
[`PRODUCT_VISION.md`](../../../PRODUCT_VISION.md). No task from later product
areas may be added here without an explicit user decision.

## Phase Goal

Complete Login, User, Role and Permission modules, including CRUD and
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
  lecturer and faculty-head views and reuses the same account modal template.
- Student Profile uses a separate `user_id` relation and normal User edit never
  changes MSSV.
- Users without a configured password cannot authenticate; setting a password
  is an explicit edit/reset action.
- `FACULTY_HEAD` and `LECTURER` remain shared access roles in `roles` and
  `user_roles`; the default Faculty Head permission bundle includes all
  Lecturer permissions plus faculty/registration workflow permissions. The
  bundle is explicit in `role_permissions`, and Faculty Head assignment is
  separate from Add Lecturer.
- Roles and permissions support CRUD and role-permission assignment.
- Role/permission changes are enforced by server-side checks, not hidden UI.
- All mutations have server validation, authorization and meaningful feedback.
- Local identity fixtures can be reset reproducibly through the authenticated
  admin seed API; the API truncates the fixed application table set before
  reseeding.
- Relevant tests and `mvn test` pass; skipped environment checks are recorded.

## Scope

In: User, Role, Permission and Login CRUD/authorization only.

Out: departments, periods, topics, groups, registrations, reports, evaluations,
results, announcements, dashboards, email and audit logging.

## Task Index

| Task | Title | Status | Done At |
|---|---|---|---|
| 001_001 | Reconcile identity baseline and persistence contracts | in_progress | |
| 001_002 | Login, logout, session and protected-route behavior | planned | |
| 001_003 | Role-aware User CRUD, Student Profile/MSSV handling, status and credential reset | planned | |
| 001_004 | Role and permission CRUD with assignment management | planned | |
| 001_005 | Permission enforcement and cross-route authorization audit | planned | |
| 001_006 | Identity-and-access verification and phase closure | planned | |

## Current Task

`001_001` — compare the existing implementation with the active product vision,
then create the smallest gap-closing task. Do not start academic-workflow work.

The identity seed source of truth is now `DatabaseSeedService`; the legacy
`database/2.seed.sql` file is retained only as a pointer to its API endpoint.
Existing MySQL databases must run `database/4.update-ddl.sql` before starting
the application when they predate the current User and Student Profile schema.
Shared admin feedback now uses the reusable layout toast fragment for success,
warning and error flash messages; form validation feedback remains inline.
Student and lecturer directories now have typed canonical routes and separate
controller/form models; the combined `/admin/users` route remains only for
legacy compatibility.

## Next Task Proposal

After the baseline is reconciled, complete the first missing identity behavior
from `001_002` through `001_005`; prioritize the smallest user-facing gap.

## Task Notes

Each task note must use `.okf/templates/task.md`, begin with `Vision alignment`,
and reference the specific row in `PRODUCT_VISION.md` it advances.
