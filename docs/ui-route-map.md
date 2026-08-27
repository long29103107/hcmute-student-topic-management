# Spring MVC SSR and REST route map

These are proposed stable route names for code generation. SSR routes map to
Spring MVC `@Controller` methods and JSPs under `WEB-INF/views`. REST routes map
to `@RestController` methods in the same WAR and reuse the same Service layer.

For the course-project MVP, implement only routes needed for the core flow:
periods, topics, groups, registrations, reports and simple evaluations/results.
Board management, advanced announcements and optional polish routes are Should
Have unless explicitly selected.

## Public/authentication

| Method | Route | Controller/view | Access |
|---|---|---|---|
| GET | `/login` | `AuthController` → `auth/login.jsp` | anonymous |
| POST | `/login` | `AuthController` | anonymous |
| POST | `/logout` | `AuthController` | authenticated |
| GET | `/access-denied` | `ErrorController` | any |

## Shared pages

| Method | Route | Purpose |
|---|---|---|
| GET | `/` | `HomeController` → `home.html`, redirects to the dashboard |
| GET | `/dashboard` | `HomeController` → `home.html`, first-look MVP dashboard |
| GET | `/announcements` | published announcements |
| GET | `/topics` | published topic list and filters when available |
| GET | `/topics/view?id=...` | topic detail |

## Admin/faculty-head management

| Method | Route | Purpose |
|---|---|---|
| GET/POST | `/admin/users` | `AdminUserController` → list/create/update/lock |
| GET | `/admin/roles?roleId=...` | `RoleManagementController` → combined system-role directory and permission editor (ADMIN hidden) |
| GET/POST | `/admin/roles/{id}/permissions` | `RoleManagementController` → select/toggle permissions for a system role only; GET redirects to the combined editor |
| GET/POST | `/faculty/departments` | `DepartmentController` → management |
| GET/POST | `/faculty/periods` | `RegistrationPeriodController` → list/create/update |
| GET/POST | `/faculty/topics/review` | `TopicReviewController` → approve/reject/publish |
| GET/POST | `/faculty/registrations/review` | `RegistrationReviewController` → approve/reject |
| GET/POST | `/faculty/boards` | `ReviewBoardController` → full board/member/topic assignment (Should Have) |
| GET/POST | `/faculty/results` | `ResultController` → aggregate/publish |
| GET/POST | `/announcements/manage` | `AnnouncementController` → advanced create/edit/hide/publish (Should Have) |

## Lecturer

| Method | Route | Purpose |
|---|---|---|
| GET/POST | `/lecturer/topics` | `TopicController` → propose/view own topics |
| GET | `/lecturer/assignments` | `AssignmentController` → supervised/reviewer assignments |
| GET/POST | `/lecturer/scoring` | `ScoreController` → score/comment |
| GET | `/reports/view?id=...` | `ReportController` → permitted download/view |

## Student/group

| Method | Route | Purpose |
|---|---|---|
| GET/POST | `/student/groups` | `StudentGroupController` → create/join/member list |
| POST | `/student/groups/register-topic` | `TopicRegistrationController` → leader submits |
| GET | `/student/registrations` | `TopicRegistrationController` → own status |
| GET/POST | `/student/reports` | `ReportController` → leader submits/member status |
| GET | `/student/results` | `ResultController` → own group result |

## RESTful API in the same monolith

The REST surface is an adapter for AJAX, testing and future consumers. It does
not replace the JSP flow or duplicate Service rules.

| Method | Endpoint | Purpose | Access |
|---|---|---|---|
| POST | `/api/auth/login` | authenticate and create session | anonymous |
| POST | `/api/auth/logout` | invalidate session | authenticated |
| GET | `/api/announcements` | published announcements | authenticated |
| GET | `/api/topics` | published topics with period/department/status filters | authenticated |
| GET/POST | `/api/faculty/periods` | list/create periods | Faculty Head |
| POST | `/api/topics/{id}/approve` | approve/reject/publish topic action | Faculty Head |
| GET/POST | `/api/student/groups` | group/member operations | Student |
| POST | `/api/student/groups/{groupId}/registrations` | leader submits registration | Group leader |
| POST | `/api/student/reports` | leader uploads report metadata/file | Group leader |
| GET | `/api/reports/{id}` | permitted report metadata/download | permitted user |
| POST | `/api/faculty/scores/{topicId}` | submit score/comment | assigned lecturer |
| POST | `/api/faculty/results/{id}/publish` | publish final result | Faculty Head |

REST request/response DTOs, status codes and field errors must be documented in
the owning task before implementation. `@RestController` methods must never
calculate final grades or bypass resource authorization.

## Implementation rules

- Use POST/redirect/GET for all state changes.
- `id` values are parsed and validated before Service call; Service rechecks
  ownership and role.
- Do not expose direct JSP paths; all protected views remain under
  `WEB-INF/views`.
- Any SSR or REST route added for a Must Have feature must be added here and to
  the phase task acceptance criteria.
