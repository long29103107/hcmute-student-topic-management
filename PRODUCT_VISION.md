# Product Vision

## Product

HCMUTE Student Topic Management is a server-rendered administration portal for
managing student-topic workflows. The product must keep authorization,
validation and data-integrity rules on the server.

## Source of truth for task creation

`PRODUCT_VISION.md` is the **primary product-scope and priority source** for
every new task, plan, phase task note and scope decision.

Before creating or proposing a task, every agent must:

1. Read this file.
2. Identify the active milestone and the capability the task advances.
3. Confirm that the task is inside that milestone's `In scope` list.
4. Add a `Vision alignment` section to the task or plan with the milestone,
   user outcome and explicit out-of-scope boundary.

Do not create work from historical phase files alone. `REQUEST.md`, `docs/`
and `.okf/standards/` remain implementation and business-rule references when
they do not conflict with this vision. An explicit user decision overrides all
repository planning documents.

## Active milestone: 001 — Identity and access

### Goal

Complete the User, Role, Permission and Login modules so the application has a
fully usable, server-authorized identity and access-management foundation.

### In scope

- Login, logout, session lifecycle and access-denied behavior.
- Protected routes and server-side authorization based on assigned roles and
  permissions.
- User CRUD: list, search/filter where useful, view, create, edit,
  activate/deactivate or safely delete and reset credentials. New accounts
  select an allowed role; Student creation uses a unique MSSV as the login
  identifier, assigns `STUDENT` automatically, generates a readonly
  `<MSSV>@student.hcmute.edu.vn` email and leaves the password unset until an
  administrator sets it. Lecturer creation assigns `LECTURER` automatically
  and uses a manually entered email as the login identifier and email, leaving
  the password unset until an administrator sets it. The directory has separate
  student, lecturer and faculty-head views with a reusable account modal.
- Role CRUD and role-to-permission assignment.
- Permission CRUD and permission-to-role assignment.
- Duplicate/invalid input validation, PRG feedback and safe password hashing.
- Reproducible local identity fixtures through an admin-only seed endpoint and
  `/seed` page that resets the application database before reseeding.
- Flowbite/Thymeleaf UI for the above, including reusable user add/edit modal
  fragments.
- Service/controller authorization tests and an end-to-end smoke of the
  identity flow.

### Identity role model

- `STUDENT`, `LECTURER`, `FACULTY_HEAD` and `ADMIN` are access roles stored in
  the shared `roles` table and assigned through `user_roles`; do not create a
  separate user table for each role.
- `FACULTY_HEAD` is a separate access role whose default permission bundle
  includes every Lecturer capability plus department, registration-period,
  topic-review and topic-registration-review permissions. This is an explicit
  role-permission assignment, not runtime role inheritance.
- Creating a Lecturer account assigns `LECTURER`; `FACULTY_HEAD` is granted
  separately through role assignment. A Faculty Head does not need a second
  `LECTURER` role to propose topics or submit evaluations.
- The sidebar has separate Manage students, Manage lecturers and Manage
  faculty heads views. Manage lecturers means users with Lecturer capabilities
  (`LECTURER` or `FACULTY_HEAD`), while Manage faculty heads means users with
  `FACULTY_HEAD`.
- Role-specific profile data belongs in profile tables keyed by `user_id` (for
  example `student_profiles`), not in duplicate authentication tables.

### Out of scope

- Departments, registration periods, topics, groups, registrations, reports,
  evaluations, results, announcements and dashboards.
- Email invitations, audit logs, OAuth expansion, advanced analytics and other
  product modules not explicitly brought into this milestone by the user.

### Definition of done

- A valid active account can log in and log out; invalid or inactive accounts
  cannot establish a session.
- Direct access to protected routes is blocked for anonymous users and users
  without the required permission.
- Authorized administrators can complete the agreed CRUD flows for users,
  roles and permissions through the UI and direct requests.
- Role and permission changes take effect in server-side authorization.
- Student Profile is separate from User, uses `user_id`, and enforces unique
  MSSV; normal User edits never change MSSV.
- A Student account without a configured password cannot log in; setting a new
  password is an explicit edit/reset action.
- Passwords are never stored or rendered as plaintext; validation and duplicate
  constraints are enforced on the server.
- Local seed/reset is explicit, authenticated as `ADMIN`, and available from
  the `/seed` page, which calls the CSRF-protected API after confirmation. It
  truncates the fixed application table set before restoring roles,
  permissions, test accounts and the sample Student Profile.
- Relevant tests and `mvn test` pass; unavailable environment checks are
  recorded explicitly.

### Task sequence

| Task | Outcome | Status |
|---|---|---|
| 001_001 | Reconcile the current identity schema, seed data and service contracts with this vision. | in progress |
| 001_002 | Complete login, logout, session and route authorization behavior. | planned |
| 001_003 | Complete role-aware User CRUD, Student Profile/MSSV handling, status management and credential reset. | planned |
| 001_004 | Complete Role and Permission CRUD plus role-permission assignment. | planned |
| 001_005 | Apply permission checks consistently to UI routes and mutations. | planned |
| 001_006 | Verify the full identity-and-access milestone and record evidence. | planned |

The shared admin layout provides reusable success, warning, and error toast
feedback for redirect-based operations. Toasts are dismissible and auto-hide;
validation errors that belong to a form may remain inline.
Each toast displays a seconds countdown, auto-dismisses after its configured
duration, and the client keeps at most three visible notifications.
Student and lecturer account management use separate canonical resource paths
(`/admin/students` and `/admin/lecturers`) while sharing only reusable UI and
domain persistence components.

The Student and Lecturer directories use shared server-side list behavior:
search filtering, sortable displayed columns, and pagination must preserve the
current query state across navigation. Search inputs should submit through the
shared UI with a Search button and a short client-side debounce.
Credential setup is a separate action from account creation and normal profile
editing: the directories expose a reusable Set password modal that validates
and hashes a new password through a protected password action.

### Administration UI standard

All current and future administration pages should follow the Roles &
permissions page as the visual/layout reference: use the full available main
content width with responsive internal padding, a consistent page heading and
action area, compact Flowbite-style cards/panels, and the shared spacing,
borders, typography, buttons, tables, modals, and toast patterns. Do not add a
`max-w-*` wrapper to a page unless the page genuinely needs a constrained
reading/form width and the task explicitly justifies it.

## Later product roadmap

These are product directions only. They must not become phase tasks until the
active milestone is complete and the user chooses the next scope.

### Future task inventory

| Order when selected | Capability bundle migrated from the former phase plan |
|---|---|
| Next | Department management; registration-period model and time gates; lecturer topic proposal and supervisor data; faculty review/publication; announcement management and public view. |
| Later | Student-group and membership model; leader/membership transaction rules; leader topic registration; faculty registration approval; student status/detail views. |
| Later | Report metadata and safe storage; leader-only upload/access; supervisor assignment; reviewer/grader assignment and conflict rules. |
| Later | Evaluation board/member management; board-topic assignment; score entry with conflict/deadline checks; aggregation; result publication and student result privacy. |
| After core flow | Cross-role authorization audit; end-to-end verification; documentation/handoff; search/filter/pagination; rejection/deadline presentation; report resubmission policy; score locking; responsive/client validation. |
| Optional | Dashboard statistics, activity log, email notification, AJAX search and report-version history. |

When one of these bundles is selected, refine it into small tasks in this file
first, set it as the active milestone, then add the matching task notes to the
current phase. Do not revive the deleted historical phase files.

## Task quality gate

Every new task must be small, independently verifiable and state:

- Vision alignment and milestone.
- Intended role and user-facing outcome.
- In-scope and out-of-scope boundaries.
- Authorization, validation, state and persistence implications.
- Acceptance criteria and focused verification.

If a request is outside the active milestone, record it in the later roadmap
or ask the user to explicitly change the vision; do not silently create a new
phase.
