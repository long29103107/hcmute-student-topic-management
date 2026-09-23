---
phase: 007
title: Result Publication and Integration QA
status: completed
source: GitHub Project #5 Kanban
task_count: 2
done_count: 2
---

# Phase 007 Summary

Scanned from the Project #5 Kanban on 2026-09-08. The phase prefix is
007_xxx and both cards are Done.

## Goal

Make multi-evaluator scoring deterministic, gate publication on complete board
data, add repeatable fixtures and verify student privacy.

## Ticket index

### 007_001 — Multi-Evaluator Scoring & Average Calculation

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/25

- Each registration/evaluator pair has at most one evaluation and active member
  pair; JPA/DDL unique constraints enforce this.
- SSR/REST share score range, status and precision validation.
- Valid submitted evaluations produce a two-decimal HALF_UP average.
- Deadline, board status and published-result locks reject later score changes.

### 007_002 — Result Publication, Seed & End-to-End QA

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/26

- A board result requires every active member to submit a valid score and the
  board to be COMPLETED.
- Publication records publisher/time, sets the board to PUBLISHED and blocks
  further scoring.
- Students see only their own group's published result.
- Reset/reseed is repeatable and creates two boards, six members/evaluations,
  one published result at average 8.50 and one active board with drafts.
- The authenticated login-to-/student/results journey and manual workflow
  documentation are covered.
- `docs/AI_WORKFLOW_GUIDE.md` provides the teammate/AI handoff: end-to-end
  overview, roles and permissions, state gates, step-by-step main flows,
  verification checklist and a reusable task prompt.
- `docs/HUMAN_WORKFLOW.md` provides the browser-based manual runbook: exact
  seed accounts, password preparation, login/logout order, page actions,
  expected state changes, negative checks and the complete topic-to-result flow.
- `Dockerfile` and `docker-compose.yaml` provide a local Spring Boot + MySQL
  runtime with first-start DDL initialization, health-gated app startup and
  persistent report/database volumes. Compose follows the application
  defaults (port 5000, root with empty local password and public seed enabled)
  and only changes the datasource host to the Docker service name `mysql`.

### Docker runtime correction

- Restored the `mysql` Compose service after the app-only compose definition
  caused the app container to fall back to `127.0.0.1:3306` and fail Hibernate
  JDBC metadata detection.
- The browser URL remains `http://localhost:5000`; only the container-to-
  container datasource URL uses `mysql:3306`.

## Verification

- ResultPublicationControllerTest
- DatabaseSeedControllerTest
- ReviewBoardControllerTest
- `SETUP_GUIDE.md` reconciled with the current 18-table schema, 12-step seed
  pipeline, 28 permissions, 71 users, group/topic/registration fixtures,
  review-board result fixtures, announcement states and seed security modes.
- Full Maven/package/static checks were recorded during implementation.
- Browser automation was unavailable; MockMvc covers the authenticated
  SSR/REST journey and negative authorization paths.
- Docker verification: `docker compose config --quiet` passed; the image
  rebuilt successfully; MySQL became healthy; the app started with
  `Database dialect: MySQLDialect`; `GET http://localhost:5000/login` returned
  HTTP 200.
- `mvn -B -DskipTests package` passed. A full `mvn -B test` run still reports
  12 pre-existing H2 schema setup errors in `RegistrationPeriodControllerTest`
  and `ReviewBoardControllerTest`; the container configuration does not change
  those test fixtures.

## Phase 8 directory UX follow-up

- `/faculty/topics/supervisors` now combines server-side search, department,
  registration-period and topic-status filters before sorting and pagination.
  Filter options remain scoped to the manager's authorized topic set; Admin
  can work across departments while Faculty Head remains department-scoped.
- Sort and pagination links preserve all directory parameters, and empty
  filtered results use the shared `fragments/no-data` state without topic
  action menus or assignment modals.
- The shared admin user directory keeps department, role and status filters
  readable with a minimum filter width, and renders the shared `No data` state
  only when the current lecturer/student result set is empty.
- Verification: browser smoke on `/admin/lecturers` confirmed normal results
  show the table without `No data`, while a non-matching status/search query
  shows only the empty state. Maven package verification was unavailable
  because Maven Central access is blocked in the environment.
- The lecturer directory uses a distinct broad capability scope for its
  default view (`LECTURER_DIRECTORY` = `LECTURER` or `FACULTY_HEAD`); explicit
  `LECTURER` and `FACULTY_HEAD` filters are exact-role filters.
- `/faculty/groups` now supplies period/status filter options from the server,
  applies both filters within the authorized Admin/Faculty Head scope, keeps
  them across sorting, pagination and group edits, and defaults the faculty
  directory to five rows per page so pagination is visible for larger lists.
- The repeatable database seed now creates 20 student groups total, using
  existing student fixtures with a mix of ACTIVE, COMPLETED and INACTIVE
  statuses for directory pagination and status-filter verification.
- `/faculty/results` now supplies Department filter options from active
  departments. Admin receives all active departments and may filter the
  approved publication queue by department; Faculty Head receives only their
  assigned department and the service keeps the result scope restricted to it.
- Verification: `mvn.cmd -B -DskipTests compile` passed; browser smoke confirmed
  the Admin dropdown contains the active departments and filtering to CNTT
  leaves only CNTT results.
- `/faculty/results` now paginates the filtered publication queue with a default
  page size of five, preserving department, status, search and sort parameters
  across page links. The filter/search controls use one fixed-width horizontal
  row with overflow handling so the fields do not overlap.
- Verification: browser smoke confirmed the one-row controls and showed `1–5 of
  11` on page one, then `6–10 of 11` after navigating to page two; compile and
  `git diff --check` passed.
- The repeatable topic seed now adds ten deterministic DRAFT proposals owned by
  `nguyen.van.khang`, so the Faculty Head's own-topic directory has two pages at
  the default page size of five. The `/api/seed/topics` step upserts these
  fixtures without resetting the database.
- Verification: `/api/seed/topics` returned `count: 90`; browser smoke showed
  `Showing 1–5 of 10` and `Showing 6–10 of 10` for the Faculty Head account;
  `DatabaseSeedControllerTest` passed 3/3 and compile passed.
- `/lecturer/topics` now exposes server-side page/size parameters with a default
  of five proposals per page, renders the current range and Previous/Next/page
  links, and keeps the existing owner-only proposal scope and empty state.
- Verification: `TopicProposalControllerTest` passed all 7 tests; Maven compile
  and `git diff --check` passed. Browser smoke reached the authenticated page;
  the seeded Faculty Head account had no own proposals, so it correctly showed
  the existing `No data` state rather than pagination controls.
- `/admin/departments` gives the status filter a fixed wider width (`w-44`) and
  prevents flex shrinking so `All statuses` and its native arrow remain readable.
- Verification: browser smoke confirmed the widened filter on the department
  management page; `mvn.cmd -B -DskipTests compile` and `git diff --check` passed.
- `/faculty/boards` now renders the department/status/sort/direction filter form
  inside the review-board card header, with the controls aligned to the right
  beside the card content instead of in a separate panel.
- Verification: browser smoke confirmed the filters appear inside the Review
  boards card and remain right-aligned; `mvn.cmd -B -DskipTests compile` and
  `git diff --check` passed.
- `/faculty/boards` now uses a flat, sortable directory table for groups,
  topics, department/period, scheduled time and status. The filter row is
  limited to department, status, keyword search and Search; search matches the
  board's group/topic/department/period/status and active member details.
- Board pagination and sort links preserve all active filters, while the
  existing Admin/Faculty Head department scope and board action controls remain
  server-authorized.
- Verification: `ReviewBoardControllerTest` passed 11/11, Maven compile passed,
  browser smoke confirmed the flat table, header sort links and compact filter
  row, and `git diff --check` passed.
- The Review board edit modal footer now spans the full form width so Cancel and
  Save changes are aligned to the right edge of the modal.
- Verification: browser smoke opened the Edit review board modal and confirmed
  the right-aligned actions; `mvn.cmd -B -DskipTests compile` and
  `git diff --check` passed.
- The Review boards status select now uses a fixed wider, non-shrinking width
  (`w-44`) so `All statuses` and its native arrow remain readable.
- Verification: browser smoke confirmed the wider status filter; Maven compile
  and `git diff --check` passed.
- The Review board Edit form now closes the Chair/Secretary field group before
  its full-width footer, matching the Create form with normal-height actions
  aligned to the right.
- Verification: the template structure was rechecked, Maven compile passed,
  and `git diff --check` passed.
- Follow-up fix: replaced the unsupported `col-span-full` utility with the
  generated `sm:col-span-2` utility, so the Edit footer spans both form columns
  at the same breakpoint as the form grid.
- Verification: browser smoke confirmed the divider spans the full modal and
  the actions match the Create form; Maven compile and `git diff --check` passed.
- `/faculty/topics/supervisors` now renders the shared `No data` fragment only
  when the filtered topic list is empty; populated results render the table and
  pagination without the empty-state message.
- Verification: the focused `TopicSupervisorControllerTest` data and empty
  state cases passed 2/2; the full class still has one pre-existing assertion
  expecting the removed `6 topics` header, unrelated to this empty-state fix.
- The Supervisor assignments filter form now uses fixed-width flex controls so
  department, period, status, search and Search stay on one right-aligned row
  on desktop, while still wrapping on narrow screens.
- Verification: browser smoke confirmed the five controls render on one row;
  Maven compile and `git diff --check` passed.
