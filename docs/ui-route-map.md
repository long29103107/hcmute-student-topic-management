# Spring MVC SSR and REST route map

These are proposed stable route names for code generation. SSR routes map to
Spring MVC `@Controller` methods and Thymeleaf templates under
`src/main/resources/templates`. REST routes map
to `@RestController` methods in the same executable Spring Boot application
and reuse the same Service layer.

For the course-project MVP, implement only routes needed for the core flow:
periods, topics, groups, registrations, reports and simple evaluations/results.
The Announcement bundle is explicitly selected for Phase 005 and is listed
below as an implemented extension.

## Public/authentication

| Method | Route | Controller/view | Access |
|---|---|---|---|
| GET | `/login` | `LoginController` → `login.html`; authenticated users are redirected to `/dashboard` | anonymous page, authenticated redirect |
| POST | `/login` | `AuthController` | anonymous |
| POST | `/logout` | `AuthController` | authenticated |
| GET | `/access-denied` | `AccessDeniedController` → redirect to `/forbidden` | any; compatibility redirect |
| GET | `/error` | `ErrorPageController` (`ErrorViewResolver`) → redirect to a named error path | HTML error dispatch; standalone pages |
| GET | `/bad-request`, `/error/400` | `ErrorPageController` → `error/400.html` | public page; response keeps 400 |
| GET | `/unauthorized`, `/error/401` | `ErrorPageController` → `error/401.html` | public page; response keeps 401 |
| GET | `/forbidden`, `/error/403` | `ErrorPageController` → `error/403.html` | public page; response keeps 403 |
| GET | `/not-found`, `/error/404` | `ErrorPageController` → `error/404.html` | public page; response keeps 404 |
| GET | `/internal-server-error`, `/error/500` | `ErrorPageController` → `error/500.html` | public page; response keeps 500 |
| GET | `/service-unavailable`, `/error/503` | `ErrorPageController` → `error/503.html` | public page; response keeps 503 |

## Shared pages

| Method | Route | Purpose |
|---|---|---|
| GET | `/` | `HomeController` → redirects to the role-aware dashboard |
| GET | `/dashboard` | `HomeController` → `dashboard/admin.html`, `dashboard/faculty-head.html`, `dashboard/lecturer.html` or `dashboard/student.html` based on the authenticated role |
| GET | `/announcements` | `AnnouncementController` → published announcements visible to the authenticated user's school/department scope |
| GET | `/topics` | `TopicCatalogController` → published topic list filtered by keyword, department and valid student registration period |
| GET | `/topics/view?id=...` | topic detail |

## Admin/faculty-head management

| Method | Route | Purpose |
|---|---|---|
| GET | `/admin/students` | `StudentManagementController` → student directory, search, status summary and department display |
| GET | `/admin/lecturers` | `LecturerManagementController` → lecturer-capability directory (`LECTURER` or `FACULTY_HEAD`), search, status summary, department display and sortable department column |
| POST | `/admin/students`, `/admin/students/{id}/edit`, `/admin/students/{id}/status`, `/admin/students/{id}/delete` | `StudentManagementController` → student create, update, lock/unlock and safe delete |
| POST | `/admin/lecturers`, `/admin/lecturers/{id}/edit`, `/admin/lecturers/{id}/status`, `/admin/lecturers/{id}/delete` | `LecturerManagementController` → lecturer create, update, lock/unlock and safe delete |
| GET | `/admin/users` | `UserManagementController` → legacy combined account directory |
| GET/POST | `/admin/users/new` | `UserManagementController` → create account and assign system roles |
| GET/POST | `/admin/users/{id}/edit` | `UserManagementController` → update account details and role assignments |
| POST | `/admin/users/{id}/status` | `UserManagementController` → lock/unlock account with self/last-admin safeguards |
| GET | `/seed` | `DatabaseSeedPageController` → admin page for invoking the seed API |
| GET | `/admin/roles?roleId=...` | `RoleManagementController` → combined system-role directory and permission editor (ADMIN hidden) |
| GET/POST | `/admin/roles/{id}/permissions` | `RoleManagementController` → select/toggle permissions for a system role only; GET redirects to the combined editor |
| GET/POST | `/admin/departments`, `/admin/departments/{id}/edit`, `/admin/departments/{id}/status`, `/admin/departments/{id}/delete` | `DepartmentController` → Admin-only department CRUD, search (`search`), pagination (`page`, `size`), column sort (`sort=code|name|status`, `direction=asc|desc`), activate/deactivate and safe delete; delete succeeds only when no users or topics reference the department |
| GET | `/faculty/departments` | `FacultyDepartmentController` → Faculty Head read-only view of their assigned department members |
| GET/POST | `/faculty/periods`, `/faculty/periods/{id}/edit` | `RegistrationPeriodController` → Faculty Head/Admin registration-period CRUD with lecturer/student windows, status and creator tracking |
| GET | `/faculty/topics/supervisors` | `TopicSupervisorController` → Admin/Faculty Head topic list scoped by authorization, with search (`search`), pagination (`page`, `size`), column sort (`sort=topic|department|period|status|proposer|supervisors`, `direction=asc|desc`) and supervisor-assignment modals (`SUPERVISOR_MANAGE`) |
| POST | `/faculty/topics/{id}/supervisors` | `TopicSupervisorController` → replace one or two active Lecturer/Faculty Head supervisors through the shared Service validation |
| GET/POST | `/faculty/topics/review` | `TopicReviewController` → list pending proposals and approve/reject |
| GET/POST | `/faculty/topics/publish` | `TopicPublicationController` → list approved topics in the Faculty Head's department and publish them |
| GET/POST | `/faculty/registrations/review` | `TopicRegistrationReviewController` → Faculty Head/Admin pending-registration queue, approve/reject with required rejection reason |
| GET/POST | `/faculty/registrations/evaluators`, `/faculty/registrations/{id}/evaluator` | `EvaluatorAssignmentController` → approved-registration evaluator queue and modal assignment/change flow |
| GET/POST | `/faculty/boards` | `ReviewBoardController` → full board/member/topic assignment (Should Have) |
| GET/POST | `/faculty/results`, `/faculty/results/{id}/publish` | `ResultController` → scoped result publication queue and final publish action |
| GET | `/announcements/manage` | `AnnouncementController` → management queue and create/edit forms |
| POST | `/announcements/manage`, `/announcements/manage/update` | `AnnouncementController` → create draft or update an announcement through PRG |
| POST | `/announcements/manage/publish`, `/announcements/manage/hide` | `AnnouncementController` → confirmed publish/hide transitions with CSRF |

## Lecturer

| Method | Route | Purpose |
|---|---|---|
| GET | `/lecturer/topics` | `TopicProposalController` → list the authenticated Lecturer's own proposals and render compact `max-w-2xl` create/edit modals; Faculty Head access is limited to the same own-proposal capability |
| POST | `/lecturer/topics`, `/lecturer/topics/{id}/edit`, `/lecturer/topics/{id}/submit` | `TopicProposalController` → create, update or submit an owned proposal through PRG; submit transitions `DRAFT`/`REJECTED` to `PENDING_APPROVAL` while the lecturer registration window is open |
| GET | `/lecturer/assignments` | `AssignmentController` → supervised/reviewer assignments |
| GET/POST | `/lecturer/scoring`, `/lecturer/scoring/{id}` | `EvaluationScoringController` → assigned evaluation queue and score/comment modal; score changes use PRG |
| GET | `/reports/view?id=...` | `ReportViewController` → relationship-authorized report download |

## Student/group

| Method | Route | Purpose |
|---|---|---|
| GET | `/student/groups` | `StudentGroupController` → related group list with search (`search`), pagination (`page`, `size`), column sort (`sort=group|period|leader|members|status|created`, `direction=asc|desc`), create form and join form |
| POST | `/student/groups`, `/student/groups/join`, `/student/groups/{id}/join`, `/student/groups/{id}/leave`, `/student/groups/{id}/leader` | `StudentGroupController` → create, join by shared group ID, join/leave an active group and transfer leadership to another member; server enforces Student role, one active group per period, max three members and leader membership |
| GET/POST | `/student/groups/register-topic` | `TopicRegistrationController` → leader registration form and submit |
| GET | `/student/registrations` | `TopicRegistrationController` → own group registration history/status |
| GET | `/student/results` | `ResultController` → published results for the student's own groups only |

## RESTful API in the same monolith

The REST surface is an adapter for AJAX, testing and future consumers. It does
not replace the Thymeleaf flow or duplicate Service rules.

| Method | Endpoint | Purpose | Access |
|---|---|---|---|
| POST | `/api/auth/login` | authenticate and create session | anonymous |
| POST | `/api/auth/logout` | invalidate session | authenticated |
| POST | `/api/seed/ddl` | drop the 18 revised-schema tables, then recreate the MySQL schema from `database/1.ddl.sql` | ADMIN only + CSRF by default; anonymous local bootstrap with `SEED_PUBLIC_ENABLED=true`; destructive local operation |
| POST | `/api/seed/permissions` | create or update local permission fixtures | ADMIN only + CSRF by default; anonymous local bootstrap with `SEED_PUBLIC_ENABLED=true` |
| POST | `/api/seed/roles` | create or update local role fixtures | ADMIN only + CSRF by default; anonymous local bootstrap with `SEED_PUBLIC_ENABLED=true` |
| POST | `/api/seed/role-permissions` | recreate role-permission fixture assignments | ADMIN only + CSRF by default; anonymous local bootstrap with `SEED_PUBLIC_ENABLED=true` |
| POST | `/api/seed/users` | create or update local test accounts and role assignments | ADMIN only + CSRF by default; anonymous local bootstrap with `SEED_PUBLIC_ENABLED=true` |
| POST | `/api/seed/registration-periods` | create or update the local open registration-period fixture | ADMIN only + CSRF by default; anonymous local bootstrap with `SEED_PUBLIC_ENABLED=true` |
| POST | `/api/seed/student-groups` | create or update four local student-group fixtures with seeded memberships | ADMIN only + CSRF by default; anonymous local bootstrap with `SEED_PUBLIC_ENABLED=true` |
| POST | `/api/seed/topics` | create or update eight local topic fixtures and their one-to-two supervisor assignments | ADMIN only + CSRF by default; anonymous local bootstrap with `SEED_PUBLIC_ENABLED=true` |
| POST | `/api/seed/announcements` | create or update the school-wide and CNTT published announcement fixtures | ADMIN only + CSRF by default; anonymous local bootstrap with `SEED_PUBLIC_ENABLED=true` |
| POST | `/api/admin/seed` | truncate all 18 revised-schema tables and recreate local identity, academic topic, supervisor, student-group and announcement fixtures; response includes `tablesReset`, `roles`, `permissions`, `users`, `registrationPeriods`, `studentGroups`, `topics`, `topicSupervisors` and `announcements` | ADMIN only + CSRF; local destructive operation |
| GET | `/api/announcements` | published announcements | authenticated |
| GET | `/api/announcements/manage` | management-visible announcements in the actor's scope | `ANNOUNCEMENT_MANAGE` |
| POST | `/api/announcements/manage` | create a draft from { title, content, scope, departmentId } | `ANNOUNCEMENT_MANAGE` + CSRF |
| PUT | `/api/announcements/manage/{id}` | update title/content/scope while preserving lifecycle status | `ANNOUNCEMENT_MANAGE` + CSRF |
| POST | `/api/announcements/manage/{id}/publish` | publish a draft or hidden announcement | `ANNOUNCEMENT_MANAGE` + CSRF |
| POST | `/api/announcements/manage/{id}/hide` | hide a published announcement | `ANNOUNCEMENT_MANAGE` + CSRF |
| GET | `/api/topics` | published topics filtered by keyword, period or department; the server only returns PUBLISHED topics in an OPEN period with an active student window | `TOPIC_VIEW` |
| POST | `/api/faculty/topics/{id}/publish`, `/api/topics/{id}/publish` | publish an APPROVED topic; Admin can publish across departments and Faculty Head is scoped to their department | `TOPIC_REVIEW` + Admin/Faculty Head role + CSRF |
| GET/POST | `/api/faculty/periods` | planned REST adapter; current registration-period CRUD is available through `/faculty/periods` | Faculty Head/Admin |
| GET | `/api/faculty/topics/supervisors` | list manageable topics with search, pagination metadata, column sort (`sort=topic|department|period|status|proposer|supervisors`, `direction=asc|desc`) and active lecturer-capability supervisor options filtered to each topic's department | `SUPERVISOR_MANAGE` |
| PUT | `/api/faculty/topics/{id}/supervisors` | replace a topic's supervisor IDs; accepts one or two IDs and returns the updated assignment | `SUPERVISOR_MANAGE` + CSRF |
| GET/POST | `/api/lecturer/topics` | list or create the authenticated user's own topic proposals; create validates the active department and inclusive lecturer registration window | `TOPIC_PROPOSE` |
| PUT | `/api/lecturer/topics/{id}` | update an owned `DRAFT`/`REJECTED` proposal; a rejected proposal returns to `DRAFT` after a valid update | `TOPIC_PROPOSE` |
| POST | `/api/lecturer/topics/{id}/submit` | submit an owned `DRAFT`/`REJECTED` proposal for Faculty review while its lecturer registration window is open; transitions it to `PENDING_APPROVAL` | `TOPIC_PROPOSE` + CSRF |
| GET | `/api/faculty/topics/review`, `/api/topics/review` | list pending topic proposals in the actor's review scope | `TOPIC_REVIEW` |
| POST | `/api/topics/{id}/approve` | approve/reject a `PENDING_APPROVAL` topic with `{ "decision": "APPROVE" | "REJECT" }` | `TOPIC_REVIEW` + CSRF |
| GET | `/api/student/groups` | related groups with search, pagination metadata and column sort (`sort=group|period|leader|members|status|created`, `direction=asc|desc`) | `GROUP_MANAGE` + Student |
| POST | `/api/student/groups` | create a group; creator becomes the leader and first member | `GROUP_MANAGE` + Student + CSRF |
| POST | `/api/student/groups/{groupId}/members` | join an active group | `GROUP_MANAGE` + Student + CSRF |
| DELETE | `/api/student/groups/{groupId}/members/me` | leave an active group as a non-leader member | `GROUP_MANAGE` + Student + CSRF |
| PUT | `/api/student/groups/{groupId}/leader` | transfer leadership to an active student who is already in the group; Admin/another `GROUP_MANAGE` actor may perform the operation, while Student is limited to the current group leader | `GROUP_MANAGE` + CSRF |
| GET | `/api/student/registrations` | current student's group registration history/status | Student + `REGISTRATION_SUBMIT` |
| POST | `/api/student/groups/{groupId}/registrations` | leader submits `{ topicId, periodId? }`; returns the pending registration | Student + `REGISTRATION_SUBMIT` + CSRF + current group leader |
| POST | `/student/registrations/{registrationId}/report` | group leader uploads a report through the SSR form with `groupId`, `periodId` and multipart `file` | Student + `REPORT_SUBMIT` + CSRF + current group leader of an approved registration |
| POST | `/api/student/groups/{groupId}/registrations/{registrationId}/reports` | group leader uploads multipart `file` with `periodId`; returns report metadata after external storage succeeds | Student + `REPORT_SUBMIT` + CSRF + current group leader of an approved registration |
| GET | `/api/faculty/registrations/review`, `/api/registrations/review` | pending topic registrations in the reviewer's department scope | `REGISTRATION_REVIEW` + Admin/Faculty Head |
| POST | `/api/registrations/{id}/review`, `/api/faculty/registrations/{id}/review` | review `{ decision: "APPROVE" | "REJECT", rejectionReason? }`; reject requires a non-blank reason and only `PENDING` rows can transition | `REGISTRATION_REVIEW` + Admin/Faculty Head + CSRF |
| GET | `/api/faculty/registrations/evaluators` | approved registrations with current evaluator and valid evaluator options, scoped by department | `REGISTRATION_REVIEW` + Admin/Faculty Head |
| PUT | `/api/faculty/registrations/{id}/evaluator` | assign or replace `{ evaluatorId }`; only approved registrations, active Lecturer/Faculty Head candidates and non-supervisors are accepted | `REGISTRATION_REVIEW` + Admin/Faculty Head + CSRF |
| GET | `/api/reports/{id}` | relationship-authorized report metadata | `REPORT_VIEW` + resource relationship |
| GET | `/api/reports/{id}/download` | relationship-authorized report file download | `REPORT_VIEW` + resource relationship |
| GET | `/api/lecturer/scoring`, `/api/faculty/scores` | assigned evaluator queue, configured score range and calculated averages | `EVALUATION_SUBMIT` + assigned evaluator |
| PUT | `/api/lecturer/scoring/{evaluationId}`, `/api/faculty/scores/{evaluationId}` | update the existing evaluation score/comment; returns the calculated average | `EVALUATION_SUBMIT` + assigned evaluator + CSRF |
| GET | `/api/faculty/results` | scoped approved-registration result publication queue | `REGISTRATION_REVIEW` + Admin/Faculty Head |
| POST | `/api/faculty/results/{id}/publish` | publish final result after all assigned evaluations have scores | `REGISTRATION_REVIEW` + Admin/Faculty Head + CSRF |
| GET | `/api/student/results` | published results for groups containing the authenticated student | `RESULT_VIEW` + Student |

REST request/response DTOs, status codes and field errors must be documented in
the owning task before implementation. `@RestController` methods must never
calculate final grades or bypass resource authorization.

## Error pages

The 400, 401, 403, 404, 500 and 503 pages are standalone Thymeleaf documents under
`src/main/resources/templates/error/`. They intentionally do not decorate the
main layout and do not include the application sidebar, header or footer. The
Spring Boot error controller selects the matching status template for error
dispatches; Spring Security's access-denied flow redirects browser requests to
the semantic `/forbidden` path. API requests retain their normal status/JSON
response instead of redirecting to HTML. The legacy `/error/{status}` aliases
remain available. Their shared visual styling is isolated in
`src/main/resources/static/css/error.css` so the pages do not depend on the
generated Tailwind utility bundle.

## Implementation rules

### Account directory UI contract

- The canonical Student and Lecturer directories keep the account name and
  login email together in the `Account` cell: the name is shown first and the
  email is shown below it without a leading `@`.
- The directories do not render a separate `Email` column. `Department` is a
  separate column showing the department code and name, or `Not assigned` for
  legacy accounts without a department.
- Admin create/edit account modals expose an active-department selector. The
  selected `departmentId` value is persisted through `users.department_id`;
  unassigned legacy accounts remain supported.

- Use POST/redirect/GET for all state changes.
- `id` values are parsed and validated before Service call; Service rechecks
  ownership and role.
- Do not expose direct template paths; all protected views remain under
  `src/main/resources/templates`.
- Any SSR or REST route added for a Must Have feature must be added here and to
  the phase task acceptance criteria.
