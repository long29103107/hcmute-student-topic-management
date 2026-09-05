# Agent Memory

Durable project context for future coding-agent sessions. Keep entries concise,
factual and free of secrets. Update this file after every code or UI update.

## 2026-09-05 — Report upload and metadata (Issue #12 / 004_001)

- Started Phase 004 task `[004_001] Report Upload & Metadata` after the user
  explicitly selected it from Project #5; the missing local Phase 003 record
  was not recreated.
- Added leader-only upload for approved registrations through SSR and
  multipart REST. The service checks registration/group/period alignment,
  current leader membership, configured content-type/size policy and the
  existing `REPORT_SUBMIT` permission.
- Added generated-key local external storage with temporary-file cleanup and
  metadata-after-storage persistence into the revised `reports` table.
- Added focused upload, invalid-file, relationship, CSRF and storage-failure
  tests. Verification: focused tests pass; full `mvn test` passes 133/133 and
  `mvn package -DskipTests` succeeds.

## 2026-09-05 — Topic registration submission (Issue #10)

- Implemented leader-only topic registration through a shared transaction-safe
  Service used by `/student/groups/register-topic` and
  `/api/student/groups/{groupId}/registrations`.
- The Service locks the group and registration period before checking active
  group state, leader membership, published topic state, period alignment,
  inclusive student window and current-registration uniqueness. It persists
  `PENDING` rows with submitter and submitted time.
- Added the SSR registration form, `/student/registrations` history page and
  `GET /api/student/registrations` relationship-scoped read endpoint. Faculty
  Head review remains Issue #11 scope.
- Verification: focused `TopicRegistrationControllerTest` passes 8/8;
  full `mvn test` is the remaining regression check after documentation update.

## 2026-09-05 — Topic registration review (Issue #11)

- Implemented Faculty Head/Admin review at `/faculty/registrations/review` and
  `/api/registrations/{id}/review`, using the existing `REGISTRATION_REVIEW`
  permission and department-scoped Faculty Head authorization.
- Only `PENDING` registrations can transition to `APPROVED` or `REJECTED`;
  rejection requires a trimmed reason of at most 500 characters. Approved and
  rejected rows remain immutable history.
- Added pending queue search/sort/pagination, SSR/REST review UI, sidebar link,
  and `findApprovedByIdForReadOnly` for downstream report/evaluation queries.
- Verification: focused `TopicRegistrationReviewControllerTest` passes 6/6;
  full regression test is still required after the combined #10/#11 changes.

## 2026-09-04 — Account directory layout and department display

- Canonical account directories are `/admin/students` and `/admin/lecturers`;
  `/admin/users` remains a legacy compatibility route.
- The `Account` cell displays the user's full name first and `email_or_code`
  below it without a leading `@`. There is no separate `Email` table column.
- Student and Lecturer directories display Department code/name in a separate
  column; missing assignments render as `Not assigned`.
- Admin account create/edit forms support selecting an active department, and
  `UserManagementService` persists the selection to `users.department_id`.
- Existing accounts without a department remain valid; selecting an inactive or
  unknown department is rejected by the service.
- Verification after this update: `mvn test` — 54 tests passed.

## 2026-09-04 — Department delete and action dropdown

- Admin Department actions now use a dropdown matching the Student directory;
  it contains Edit, Activate/Deactivate and Delete.
- `POST /admin/departments/{id}/delete` is protected by Admin plus
  `DEPARTMENT_MANAGE` and CSRF.
- Delete is allowed only when no users and no topics reference the department.
  In-use departments remain available for deactivation instead, with a flash
  error explaining the blocker.
- A dedicated delete confirmation modal is rendered per department.
- Documents updated: `docs/ui-route-map.md` and
  `docs/authorization-matrix.md`.
- Verification: `mvn test` — 56 tests passed.

## 2026-09-04 — Department directory search and sorting

- The admin Department directory supports server-side search across department
  code, name and Active/Inactive status.
- Code, department name and status headers sort ascending/descending and retain
  the current search term.
- Department results are paginated with a default page size of 10; pagination
  links retain the current search and sort query state.
- Invalid sort or direction query values fall back to name ascending.
- Documents updated: `docs/ui-route-map.md`.
- Verification: `mvn test` — 58 tests passed.

## 2026-09-04 — Topic Proposal CRUD

- Project #5 task #4, `[002_001] Topic Proposal CRUD`, was selected from Ready
  and moved to In progress.
- Lecturer-capable users with `TOPIC_PROPOSE` can list, create and edit their
  own proposals at `/lecturer/topics` and `/api/lecturer/topics`.
- Creation/update requires an active department and an `OPEN` registration
  period whose lecturer window includes the current time; only owned `DRAFT`
  and `REJECTED` proposals are editable, and a corrected rejected proposal
  returns to `DRAFT`.
- Documents and phase planning updated: `docs/ui-route-map.md`,
  `docs/domain-model.md`, `.okf/phase/002/PHASE_SUMMARY.md` and
  `.okf/phase/002/002_001-topic-proposal-crud.md`.
- Verification: focused tests pass 6/6; full `mvn test` passes 64/64 after
  updating the security regression expectation for the now-existing protected
  topic route.
- `mvn package -DskipTests` also passes and produces the executable JAR. All
  six GitHub acceptance criteria are checked and the Project #5 item is now
  Done.

## 2026-09-04 — Authenticated login redirect

- `GET /login` now redirects authenticated users to `/dashboard`; anonymous
  users and users arriving after logout still receive the login page.
- Documents updated: `docs/ui-route-map.md`.
- Verification: `mvn -Dtest=SecurityConfigTest test` — 7 tests passed; full
  `mvn test` — 65 tests passed; `mvn package -DskipTests` passed.

## 2026-09-04 — Admin full capability rule

- Admin is the full-capability system role, with all current seeded
  permissions and route gates for Lecturer, Faculty Head and Student
  workspaces.
- The sidebar exposes the available Lecturer and Faculty workflow sections to
  Admin; method/service checks still preserve explicitly documented ownership
  or assigned-resource constraints.
- Documents updated: `AGENT.md`, `docs/authorization-matrix.md`.
- Verification: focused seed/security tests pass 11/11; full `mvn test` passes
  66/66; `mvn package -DskipTests` passes and produces the executable JAR.

## 2026-09-04 — Standalone HTTP error pages

- Added standalone Thymeleaf pages for HTTP 400, 401, 403, 500 and 503 under
  `src/main/resources/templates/error/`; none uses the main layout or includes
  the application sidebar, header or footer.
- Spring Boot's error controller resolves the generic status pages, while the
  Spring Security access-denied flow renders the same 403 template.
- Documents updated: `AGENT.md`, `docs/ui-route-map.md`.
- Verification: focused `SecurityConfigTest` passes 13/13; full `mvn test`
  passes 71/71; `mvn package -DskipTests` passes.

## 2026-09-04 — Error page visual refresh

- Replaced the first error-page styling with a centered responsive card, clear
  status typography, branded identity, contextual copy and usable actions.
- Moved error-page styling to `static/css/error.css` so the standalone pages do
  not break when the generated Tailwind bundle has not been rebuilt.
- Documents updated: `AGENT.md`, `docs/ui-route-map.md`.
- Verification: local 403 page inspected in the browser; focused
  `SecurityConfigTest` passes 13/13; full `mvn test` passes 71/71; package
  passes.

## 2026-09-04 — Error page redirect paths

- Added explicit `/error/400`, `/error/401`, `/error/403`, `/error/500` and
  `/error/503` routes that render the matching standalone page while preserving
  its HTTP status.
- HTML error dispatches and browser access-denied responses redirect to the
  matching error path; API access-denied responses retain their status/JSON
  behavior. `/access-denied` remains a compatibility redirect to `/error/403`.
- Documents updated: `AGENT.md`, `docs/ui-route-map.md`.
- Verification: focused `SecurityConfigTest` passes 14/14; full `mvn test`
  passes 72/72; `mvn package -DskipTests` passes.

## 2026-09-04 — Semantic error URLs

- Canonical error page URLs are now `/bad-request`, `/unauthorized`,
  `/forbidden`, `/not-found`, `/internal-server-error` and
  `/service-unavailable`; the matching 404 page was added.
- HTML error redirects and browser access-denied redirects use the semantic
  paths. Existing `/error/{status}` routes remain available as compatibility
  aliases and preserve the corresponding HTTP status.
- Documents updated: `AGENT.md`, `docs/ui-route-map.md`.
- Verification: focused `SecurityConfigTest` passes 15/15; full `mvn test`
  passes 73/73; `mvn package -DskipTests` passes.

## 2026-09-04 — Compact topic proposal modal

- Reduced the Lecturer topic create/edit modal from an unconstrained visual
  width to the existing generated `max-w-2xl` utility, keeping the form focused
  and preventing it from stretching across the viewport.
- Added a template regression assertion and documented the constrained modal as
  an intentional UI exception.
- Documents updated: `AGENT.md`, `docs/ui-route-map.md`.
- Verification: topic proposal controller test passes with the compact modal
  assertion; full `mvn test` and `mvn package -DskipTests` pass.

## 2026-09-04 — Registration Period CRUD (Issue #2)

- Implemented `/faculty/periods` for Faculty Head/Admin with create and edit
  modals, period type/status, lecturer and student registration windows,
  optional reviewer deadline/council date, and created-by display.
- Server validation enforces ordered windows, only allows reviewer and council
  milestones for graduation thesis periods, and protects mutations with
  permission and CSRF checks.
- Added an open local seed period and `/api/seed/registration-periods` so the
  Lecturer topic proposal dropdown has a usable period after running the seed
  pipeline. Added the Registration periods sidebar link for Admin and Faculty
  Head.
- Documents updated: `AGENT.md`, `docs/ui-route-map.md`,
  `docs/domain-model.md`, `docs/workflows.md`.
- Verification: focused Registration Period/seed tests pass 8/8; full
  `mvn test` passes 78/78; `mvn package -DskipTests` and diff checks pass.

## 2026-09-04 — Registration Period checklist test audit

- Added explicit checklist coverage for DATN/course and KLTN types, both
  registration windows on create/update, rendered status/creator, invalid
  milestone/window rules, unauthorized view/modify and missing CSRF.
- Documented the Issue #2 test mapping in `docs/verification.md`.
- Verification: focused checklist/seed tests pass 8/8; full `mvn test`
  passes 78/78; `mvn package -DskipTests` and diff checks pass.

## 2026-09-04 — Timeline Rules & Permission Tests (Issue #3)

- Added forward-only Registration Period lifecycle validation, inclusive
  lecturer/student window inspection, read-only `requireOpenFor*` handoff
  methods with stable error codes, and routed Topic Proposal period checks
  through that contract.
- Updated the KLTN-only milestone rule, added explicit Department/Period
  permission coverage, and kept the existing `PERIOD_MANAGE` and
  `DEPARTMENT_MANAGE` permissions; no new permission or seed mapping was
  required.
- Documents updated: `REQUEST.md`, `AGENT.md`, `docs/domain-model.md`,
  `docs/workflows.md`, `docs/verification.md`.
- Verification: focused Issue #3 tests pass 26/26; full `mvn test` passes
  81/81; `mvn package -DskipTests` passes.

## 2026-09-04 — Backlog contract alignment (#6, #9, #12, #13)

- Updated GitHub issue bodies for #6, #9, #12 and #13 to match the revised
  database schema and the current MVP boundary. All four issues remain in
  Backlog; issue #5 was intentionally left unchanged for discussion.
- #6 is status-only topic review (`DRAFT -> PENDING_APPROVAL -> APPROVED` or
  `REJECTED`); approval audit columns/history are deferred.
- #9 is period-scoped group membership using `student_groups.period_id`, with
  one active group per student per period, at most three members and exactly
  one leader.
- #12 restricts report upload to the leader of an approved registration and
  keeps report bytes outside the database while persisting metadata only after
  storage succeeds.
- #13 defines relationship-based report access/download. Report deadline and
  resubmission remain deferred because the schema has no dedicated deadline
  field or finalized policy. `REPORT_VIEW` is planned but not yet seeded.
- Documents updated: `REQUEST.md`, `docs/workflows.md`,
  `docs/domain-model.md`, `docs/database-design.md`,
  `docs/authorization-matrix.md` and `AGENT_MEMORY.md`.
- Verification: GitHub issue bodies were confirmed in the browser; no
  application tests were run because this update changes requirements/docs,
  not application code or seed data.

## 2026-09-04 — Phase 001 outcome summary

- Added a `Delivered outcome` section to `.okf/phase/001/PHASE_SUMMARY.md` so
  the completed phase explicitly records the user-facing identity/access
  capabilities and its boundary with later academic-workflow phases.
- Updated the phase summary metadata date to `2026-09-04`; task completion dates
  and verification evidence remain unchanged.
- No application code, permission catalog or seed data changed in this update.
- Verification: `git diff --check` passed; no application tests were run for
  this documentation-only change.

## 2026-09-04 — Topic Supervisor Assignment (002_002)

- Implemented Admin/Faculty Head supervisor management for topics through
  `TopicSupervisorService`, SSR routes under `/faculty/topics/supervisors`
  and REST routes under `/api/faculty/topics/supervisors`.
- Added the `SUPERVISOR_MANAGE` permission to the seed catalog and Faculty
  Head bundle; Admin continues to receive every current permission.
- Enforced one-to-two active Lecturer/Faculty Head supervisors, duplicate and
  invalid-user rejection, Admin all-topic scope and Faculty Head own-
  department scope. Added the Faculty workflow sidebar entry.
- Updated `docs/ui-route-map.md`, `docs/domain-model.md`,
  `docs/authorization-matrix.md`, `docs/workflows.md`,
  `docs/verification.md`, the Phase 002 summary and the task checklist.
- Verification: focused `TopicSupervisorControllerTest` passes 5/5; full
  `mvn test` passes 86/86; `mvn package -DskipTests` succeeds and
  `git diff --check` passes. GitHub issue #5 status update remains the
  external closeout step.

## 2026-09-04 — Topic and supervisor seed fixtures

- Extended the admin reset/step seed pipeline with eight realistic topic
  fixtures across CNTT, KHMT, CNPM and HTTT, plus thirteen valid
  `topic_supervisors` rows.
- Added `POST /api/seed/topics` as the final step in the admin seed page;
  the reset response now reports topic and supervisor counts.
- Updated the seed test, route map, verification checklist and
  `002_002` task note. No additional permission was needed.
- Verification: `DatabaseSeedControllerTest` passes 3/3 and full
  `mvn test` passes 86/86; `mvn package -DskipTests` succeeds and
  `git diff --check` passes.

## 2026-09-04 — Same-department supervisor enforcement

- Tightened `TopicSupervisorService` so every assigned supervisor must have
  an active Lecturer/Faculty Head role and belong to the topic's department.
- SSR modals and REST responses now expose per-topic supervisor options filtered
  to that department; cross-department IDs return validation errors.
- Updated the supervisor assignment tests, task checklist, domain/workflow
  documentation and route/verification notes. No permission or seed fixture
  change was needed because all existing fixtures already match their topic
  departments.
- Verification: focused `TopicSupervisorControllerTest` passes 5/5; full
  `mvn test` passes 86/86; `mvn package -DskipTests` succeeds and
  `git diff --check` passes.

## 2026-09-04 — Same-department seed regression coverage

- Extended `DatabaseSeedControllerTest` to assert every seeded
  `topic_supervisors` row points to a user in the topic's department.
- Verification: focused seed suite passes 3/3 after the assertion; the
  previously rerun full suite passes 86/86, package succeeds and diff checks
  remain clean apart from normal LF/CRLF warnings.

## 2026-09-04 — Supervisor assignment directory controls

- Added scoped search across topic, department, period, status, proposer and
  assigned-supervisor text, whitelisted column sorting and request-level
  pagination to the SSR and REST supervisor-assignment listings.
- Pagination metadata and query-preserving links keep the Admin/Faculty Head
  scope and each topic's same-department supervisor options intact; no new
  permission or seed fixture was needed.
- Updated `docs/ui-route-map.md`, `docs/verification.md`, the `002_002` task
  note and Phase 002 summary.
- Verification: focused `TopicSupervisorControllerTest` passes 6/6; full
  `mvn test` passes 87/87; `mvn package -DskipTests` succeeds and
  `git diff --check` passes with only normal LF/CRLF warnings.

## 2026-09-05 — Topic Review, Approve and Reject (002_003)

- Implemented the Admin/Faculty Head pending topic-review queue at
  `/faculty/topics/review` and matching REST list/action endpoints.
- Enforced `TOPIC_REVIEW`, Admin all-department scope, Faculty Head own-
  department scope, proposer self-review rejection, and the only valid
  `PENDING_APPROVAL -> APPROVED|REJECTED` transitions.
- Added `TopicReviewControllerTest` coverage for queue visibility, approve,
  reject, invalid transitions/decisions, self-review, unauthorized access and
  CSRF; updated route, workflow, domain and verification documents plus the
  Phase 002 task note.
- Verification: focused `TopicReviewControllerTest` passes 5/5; full
  `mvn test` passes 92/92; `mvn package -DskipTests` succeeds and
  `git diff --check` passes with only normal LF/CRLF warnings.
- Seed verification now explicitly covers the `TOPIC_REVIEW` permission,
  Admin/Faculty Head mapping, Lecturer exclusion and three pending review
  fixtures.
- The `/seed` pipeline's `/api/seed/ddl` step now drops all 17 revised-schema
  tables, recreates the DDL and requires an explicit destructive confirmation;
  app startup remains non-destructive with `ddl-auto=none`.

## Entry template

### YYYY-MM-DD — Short update title

- Code/UI change:
- Business or authorization rule:
- Documents updated:
- Verification:
