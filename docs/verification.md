# Verification runbook

For the course-project MVP, verify the core path first: authentication, period,
topic, group, registration, report and simple evaluation/result publication.
Full review-board, dashboard, email, audit-log and report-version checks are
optional and should only be run when that scope is selected.

## Local Maven checks

Run from repository root after `pom.xml` exists:

```powershell
mvn test
mvn package
```

Build stylesheet trước khi deploy Thymeleaf templates:

```powershell
npm install
npm run build:assets
```

Use a focused test class while iterating, then rerun the full suite before
closing the task. Package output should be an executable Spring Boot JAR with
embedded Tomcat. The repository's automated H2 test context does not replace
the required MySQL smoke check.

## MySQL checks

Use a dedicated test schema and connection values supplied through Maven
profile, system properties or environment variables. Never commit credentials.

Verify at least:

- schema can be created from the repository resources;
- the Admin seed pipeline drops all 17 revised-schema tables, recreates them
  from the DDL, and restores the local fixtures;
- foreign keys/unique constraints exist;
- transaction rollback leaves no partial group, registration or evaluation;
- if the extended board is selected, also verify no partial board/member/topic
  assignment;
- prepared statements handle quotes and malicious-looking input as data.

## Spring MVC/Tomcat smoke checklist

With the executable JAR running:

1. Anonymous user is redirected to login for protected pages.
2. Invalid login does not create an authenticated session.
3. Each role sees only its permitted actions and direct URL/REST access is checked.
4. Faculty Head creates a period with valid windows and invalid windows fail.
5. Lecturer proposes/Faculty Head publishes topic; student cannot select an
   unpublished topic.
6. Student group rules, leader transfer/last-leader protection, concurrent
   one-group-per-period membership, leader-only registration and duplicate
   registration are enforced.
7. Leader report upload validates permission and configured file policy.
8. Board cardinality/roles and supervisor scoring restriction are enforced when
   the extended board workflow is enabled.
9. Final average/publication/result visibility follow the configured policy.
10. Published announcements are visible; hidden announcements are not.

## Registration Period CRUD checklist (Issue #2)

The `RegistrationPeriodControllerTest` suite maps the issue checklist to
request-level assertions:

- DATN/course and KLTN period types can be created.
- Lecturer and student registration windows are persisted on create and update.
- The current status and creator are persisted and rendered in the management list.
- Invalid window ordering and inapplicable reviewer/council milestones are rejected.
- Users without `PERIOD_MANAGE` cannot view or modify periods.
- Mutating requests without a CSRF token are rejected.

## Timeline Rules & Permission Tests checklist (Issue #3)

The period service and controller tests also cover the follow-up rules:

- Reviewer and council milestones are rejected for non-KLTN periods.
- Missing required fields and reversed windows are rejected server-side.
- Status transitions follow `DRAFT -> OPEN -> CLOSED -> ARCHIVED`; backwards
  and skipped transitions are rejected.
- `RegistrationPeriodService.inspect` and its `requireOpenFor*` methods provide
  an inclusive boundary contract with stable error codes for downstream modules.
- Department and period direct requests enforce their existing permissions and
  CSRF protection.

## REST smoke checklist

## Topic Review, Approve and Reject checklist (Issue #6 / 002_003)

The `TopicReviewControllerTest` suite maps the issue checklist to SSR/REST
request-level assertions:

- Faculty Head/Admin users with `TOPIC_REVIEW` can view pending proposals;
  Faculty Head results are limited to the assigned department and Admin sees
  pending proposals across departments.
- Only `PENDING_APPROVAL` proposals are reviewable.
- Approve persists `APPROVED` and reject persists `REJECTED` through the same
  Service used by SSR and REST.
- The proposer cannot approve or reject their own proposal.
- Invalid decisions, invalid status transitions and missing proposals return
  stable REST error codes without changing the topic.
- Users without `TOPIC_REVIEW` are rejected and review mutations require CSRF.
- The seed pipeline restores `TOPIC_REVIEW` for Admin/Faculty Head, excludes it
  from Lecturer, and restores three pending review fixtures; the authorized
  sidebar entry is rendered by the review page test.
- `DatabaseSchemaServiceTest` verifies that the pipeline drops all 17 project
  tables before executing the DDL.

## Topic Publication and Published Query checklist (Issue #7 / 002_004)

The `TopicPublicationControllerTest` suite maps the publication contract to
SSR/REST request-level assertions:

- Only an approved topic can transition to `PUBLISHED`; repeated publish and
  non-approved transitions are rejected without changing state.
- Publication is restricted to an active Admin or Faculty Head with
  `TOPIC_REVIEW`; Admin can publish across departments while Faculty Head is
  scoped to their department, and Lecturer publish attempts are rejected.
- `/api/topics` and `/topics` expose only published topics whose period is
  `OPEN` and whose inclusive student registration window contains now.
- Published proposals are rejected by the existing proposal update flow, and
  publication mutations require CSRF.

## Topic Supervisor Assignment checklist (Issue #5 / 002_002)

The `TopicSupervisorControllerTest` suite maps the task acceptance criteria to
SSR and REST request-level assertions:

- `SUPERVISOR_MANAGE` is seeded; Admin receives all permissions, Faculty Head
  receives the supervisor permission, and Lecturer/Student do not.
- The admin seed pipeline creates eight realistic topic fixtures and thirteen
  `topic_supervisors` rows after users, departments and the open period.
- Admin can view and assign one or two supervisors across departments.
- Faculty Head sees and manages only topics in their own department.
- Active Lecturer and Faculty Head users are valid candidates; students and
  inactive users are rejected.
- Cross-department lecturer-capability users are rejected and are not listed
  as options for the topic.
- Empty, duplicate and over-two assignments are rejected server-side.
- Valid assignments persist to `topic_supervisors`.
- SSR and REST use the same Service rules; unauthorized access and missing
  CSRF on mutations are rejected.
- The supervisor directory searches topic, department, period, status, proposer
  and assigned-supervisor text within the actor's scope.
- Topic and Department/Period/Status/Supervisors column links toggle the
  whitelisted ascending/descending sort while preserving the current search
  and page size.
- SSR and REST pagination return the requested page size, total item/page
  metadata and clamp out-of-range page/size values safely.

When REST endpoints are implemented, verify at least:

- login/logout session behavior through `/api/auth/*`;
- JSON validation and error status for malformed requests;
- the same role/resource authorization as the SSR route;
- no REST endpoint exposes domain entities, passwords, stack traces or SQL;
- repeated calls do not create duplicate registrations or scores.
