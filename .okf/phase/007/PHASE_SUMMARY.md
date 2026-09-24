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
- The `/student/groups` search form is right-aligned within its card header,
  matching the directory filter layout while keeping the input and Search
  button together.
- Verification: browser smoke confirmed the form sits at the right edge;
  Maven compile and `git diff --check` passed.
- `/announcements/manage` now renders announcements as a flat sortable table
  with Announcement, Department, Author, Updated, Status, and Actions columns.
  The filter bar keeps only status, department, search, and Search; pagination
  preserves all active filters and sort parameters and remains visible with
  disabled controls on a single page.
- Verification: browser smoke confirmed the flat table, sortable headers, four
  filter controls, and pagination; `AnnouncementServiceTest` passed 6/6,
  Maven compile passed, and `git diff --check` passed.
- The Create announcement modal now shows required markers for Title, Content,
  and Audience; the Department marker follows the selected Audience and is
  shown only when Department is selected, matching the existing validation.
- Verification: browser smoke confirmed the markers for both School-wide and
  Department audiences; Maven compile and `git diff --check` passed.
- Announcement Edit actions now open a dedicated modal matching the Create
  announcement form, with prefilled title/content/audience/department fields,
  required markers, and Save changes/Cancel actions.
- Verification: browser smoke opened the Edit announcement popup and confirmed
  the prefilled fields and responsive form layout; Maven compile and
  `git diff --check` passed.
- `/topics` now always renders its pagination controls when topics exist,
  including disabled Previous/Next controls on a single page, while retaining
  the existing page slicing and sort/filter query parameters.
- Verification: browser smoke confirmed the filtered `/topics` page shows
  pagination and `page=1&size=5` renders `Showing 6-10 of 10` with filters and
  sorting preserved; `TopicPublicationControllerTest` passed 5/5, Maven
  compile passed, and `git diff --check` passed.
- `/student/groups` now always renders its pagination controls when groups
  exist, including disabled Previous/Next controls on a single page, while
  preserving search and sort parameters across page links.
- Verification: Maven compile passed, `StudentGroupControllerTest` passed
  11/11, and `git diff --check` passed. Browser smoke was blocked by the
  currently signed-in account lacking access to Student workspace (`403`).
- `/lecturer/topics` now always renders its topic proposal pagination controls
  when proposals exist, including disabled Previous/Next controls on a single
  page; page links retain the requested page size.
- Verification: Maven compile passed, `TopicProposalControllerTest` passed
  7/7, and `git diff --check` passed.
- The `/student/groups` Create group modal now visibly marks Group name and
  Registration period as required fields while retaining the existing HTML
  `required` validation.
- Verification: browser smoke confirmed both required markers; Maven compile
  and `git diff --check` passed.
- The `/student/groups` Join group modal now visibly marks Group ID as a
  required field while retaining the existing HTML `required` validation.
- Verification: the Join group form markup was checked; Maven compile and
  `git diff --check` passed.
- `/lecturer/topics` now includes a right-aligned Search form that filters the
  lecturer's own proposals by topic, description, department, period or status;
  the normalized keyword is preserved across pagination links.
- Verification: Maven compile passed, `TopicProposalControllerTest` passed
  7/7 including the search case, and `git diff --check` passed. Browser smoke
  could not confirm the authenticated page because the current browser session
  redirected to the app's internal-server-error page; an anonymous HTTP check
  correctly redirected to `/login`.
- `/lecturer/topics` now supports server-side sorting by topic, department,
  registration period, status and last updated; sort direction, search keyword
  and page size are preserved across table headers and pagination links.
- Verification: Maven compile passed, `TopicProposalControllerTest` passed
  8/8 including the sort case, and `git diff --check` passed.
- `/faculty/registrations/evaluators` now exposes sort links for Topic,
  Department, Registration period and Evaluator; the existing server-side sort
  contract is now reachable from every main table column and preserves search
  and pagination parameters.
- Verification: `EvaluatorAssignmentControllerTest` passed 5/5 and
  `git diff --check` passed.
- `/faculty/topics/publish` now shows sort indicators and toggle links for
  Topic, Registration period and Proposed by, exposing the existing server-side
  sort behavior while preserving search, page size and pagination state.
- Verification: `TopicPublicationControllerTest` passed 5/5 and
  `git diff --check` passed.
- `/lecturer/scoring` now supports server-side search, sorting by group/topic,
  period, score, average and status, plus pagination with state preserved in
  sort/search links; the REST list contract remains available through the
  existing unpaged service method.
- Verification: `EvaluationScoringControllerTest` passed 5/5, including the
  search/sort/pagination scenario, and `git diff --check` passed.
- 008_003 (#29): `/lecturer/topics` follows the admin student directory
  pattern: search (title, description, department, registration period,
  status), a Department dropdown of active departments and a Status dropdown
  with All combine with AND inside the lecturer's own-proposal scope. Sortable
  Topic/Department/Registration period/Status/Last updated headers reset to
  page 1 (`sort=title` is accepted as an alias of `topic`); pagination clamps
  invalid pages and every link keeps `search`, `departmentId`, `status`,
  `sort`, `direction` and `size`. The search input reuses the shared
  `data-user-search` debounce; a no-match result shows the shared `No data`
  state plus a clear link, with no row actions or edit modals. The default page
  size stays 5 (Phase 7 seed decision) rather than the issue's example of 20.
- Verification: changed Java sources and tests pass a JDK parse check and a
  dependency-free javac pass shows no errors outside the missing Spring/JUnit
  libraries; every new Tailwind utility exists in the built `tailwind.css`.
  Maven Central was blocked in the authoring environment, so `mvn test` for
  `TopicProposalControllerTest` still needs to be run locally.
- 008_005 (#31): `/faculty/periods` adds a Status dropdown (All plus
  DRAFT/OPEN/CLOSED/ARCHIVED) beside the search box. Status and search combine
  with AND on the server before sorting and pagination; every sort/page link
  keeps `search`, `status`, `sort`, `direction` and `size` (`sort=name` is
  accepted for the Period column; the default size stays 10). Unknown status
  values fall back to All like the Faculty student groups directory. Empty
  results now use the shared `fragments/no-data` state plus a Clear search and
  filters link, and no edit modals are rendered for hidden periods.
- Verification: changed Java sources and tests pass a JDK parse check and a
  dependency-free javac pass shows no errors outside the missing Spring/JUnit
  libraries; every new Tailwind utility exists in the built `tailwind.css`.
  Maven Central was blocked in the authoring environment, so `mvn test` for
  `RegistrationPeriodControllerTest` still needs to be run locally.
- 008_008 (#34): `/faculty/topics/review` adds Department and Registration
  period filters to the existing search toolbar, right-aligned in a full-width
  `justify-end` row like Manage students. Options and rows are derived after
  the existing reviewer scope (Admin: all departments, Faculty Head: own
  department), so a foreign `departmentId` returns no rows; links preserve
  both filters and the empty state distinguishes no pending proposals from no
  matches.
- Verification: changed Java sources and tests pass a JDK parse check and a
  dependency-free javac pass shows no errors outside the missing Spring/JUnit
  libraries; every new Tailwind utility exists in the built `tailwind.css`.
  Maven Central was blocked in the authoring environment, so `mvn test` for
  `TopicReviewControllerTest` still needs to be run locally.
- 008_011 (#37): `/faculty/registrations/evaluators` and its REST list add a
  scoped Department filter; sorting covers the Group, Topic, Department,
  Registration period and Evaluator columns in both directions. The toolbar
  now uses the shared management-page control styling (full-width
  right-aligned row, `p-2.5` fields, `sm:w-80` search) and resets to page 1 on
  submit. The filter runs before evaluator-option lookups, the page response
  exposes `departmentId` and `departmentOptions`, and links keep department,
  search, sort and size.
- Verification: changed Java sources and tests pass a JDK parse check and a
  dependency-free javac pass shows no errors outside the missing Spring/JUnit
  libraries; every new Tailwind utility exists in the built `tailwind.css`.
  Maven Central was blocked in the authoring environment, so `mvn test` for
  `EvaluatorAssignmentControllerTest` still needs to be run locally.
- Seed data now adds 20 idempotent topic proposals owned by
  `hoang.thai.xuan.khoa`,
  cycling DRAFT, PENDING_APPROVAL, REJECTED and APPROVED statuses so
  `/lecturer/topics` has enough rows to exercise pagination. Seed assertions
  were updated for 110 topics and 115 supervisor assignments.
- Verification: `mvn.cmd -B -Dtest=DatabaseSeedControllerTest test` passed
  (3 tests, 0 failures).
- Registration-period seed now keeps the existing open period and adds 20
  idempotent sample periods across all lifecycle statuses and period types;
  the full seed result reports 21 registration periods.
- Topic-review seed now adds 20 idempotent `PENDING_APPROVAL` topic fixtures
  in department `KT`, using lecturer proposers from that department so the
  Faculty Head account `hoang.thai.xuan.khoa` can see and review all 20 rows;
  the review queue has 35 rows for `/faculty/topics/review` pagination.
- Evaluator-assignment seed now adds 20 published KT topics with 20 approved
  registrations, so `hoang.thai.xuan.khoa` sees 20 manageable items at
  `/faculty/registrations/evaluators`.
- Latest verification: `mvn.cmd -B -Dtest=DatabaseSeedControllerTest test`
  passed (3 tests, 0 failures).
- Announcement seed now adds 20 realistic school-wide fixtures and 15
  department-scoped KT fixtures authored by `hoang.thai.xuan.khoa`, with
  Published/Draft/Hidden lifecycle coverage for the manage-page filters.
- Latest verification: `mvn.cmd -B -Dtest=DatabaseSeedControllerTest test`
  passed (3 tests, 0 failures), and `git diff --check` reported no whitespace
  errors.
- Announcement management now gives the department filter a fixed `w-80`
  width with `flex-none` so long department labels do not collapse into the
  select arrow.
- Verification: `git diff --check` passed. `AnnouncementControllerTest` still
  has two existing SSR expectation failures for the removed actions dropdown
  and outdated pagination aria label; the template width change itself does
  not affect those assertions.
- Topic publication seed now adds 20 realistic `APPROVED` accounting topics
  in department `KT`, with Hoàng Thái Xuân Khoa as an eligible supervisor, so
  his Faculty Head scope has 20 items on `/faculty/topics/publish`.
- Latest verification: `mvn.cmd -B -Dtest=DatabaseSeedControllerTest test`
  passed (3 tests, 0 failures), including the Faculty Head page visibility
  assertion; `git diff --check` also passed.
- Student-group seed now adds 20 realistic groups across five sample
  registration periods, with mixed Active/Completed/Inactive statuses and
  department-specific student members; total seeded groups is 40.
- Latest verification: `mvn.cmd -B -Dtest=DatabaseSeedControllerTest test`
  passed (3 tests, 0 failures), and `git diff --check` passed.
- Registration-review seed now adds 20 realistic pending KT registrations for
  Hoàng Thái Xuân Khoa's Faculty Head scope, distributed across the existing
  KT groups `Nhóm InfoSec`, `Nhóm SysNet`, and `Nhóm AI Lab`. The related topic
  proposals now use accounting-focused titles and descriptions, and the full
  seeded registration count is 66.
- Latest verification: `mvn.cmd -B -Dtest=DatabaseSeedControllerTest test`
  passed (3 tests, 0 failures), including Faculty Head visibility for a seeded
  registration-review item.
- Review-board seed now adds 20 realistic KT boards for
  `hoang.thai.xuan.khoa`, using the approved accounting topics, KT student
  groups, and three active KT lecturers per board. The boards cover DRAFT,
  ASSIGNED, SCHEDULED, ACTIVE, and COMPLETED statuses; seeded totals are 86
  registrations and 22 boards.
- Latest verification: `mvn.cmd -B -Dtest=DatabaseSeedControllerTest test`
  passed (3 tests, 0 failures), including Faculty Head visibility on
  `/faculty/boards` for a seeded accounting board.
- Result-publication seed now adds 15 KT result records linked to the seeded
  accounting boards, with realistic evaluation scores and DRAFT, FINALIZED,
  and PUBLISHED result states. Hoàng Thái Xuân Khoa can see the seeded rows on
  `/faculty/results`; the total seeded result count is now 16 including the
  original published result.
- Latest verification: `mvn.cmd -B -Dtest=DatabaseSeedControllerTest test`
  passed (3 tests, 0 failures), including Faculty Head visibility on
  `/faculty/results` for a seeded accounting result.
- Lecturer topic seed now adds 15 realistic KT proposals owned by
  `hoang.thai.xuan.khoa`, distributed across DRAFT, PENDING_APPROVAL, REJECTED,
  and APPROVED states so the lecturer workspace can exercise editing,
  submission, filtering, and pagination. Seed totals are now 185 topics and
  190 supervisor assignments.
- Latest verification: `mvn.cmd -B -Dtest=DatabaseSeedControllerTest test`
  passed (3 tests, 0 failures), including the seeded owner's visibility on
  `/lecturer/topics`.
- Lecturer scoring seed now adds 15 blank, editable DRAFT evaluations for
  `hoang.thai.xuan.khoa` on approved KT registrations without published
  results or review-board locks, so the Faculty Head can enter scores at
  `/lecturer/scoring`.
- Latest verification: `mvn.cmd -B -Dtest=DatabaseSeedControllerTest test`
  passed (3 tests, 0 failures), including the scoring-page visibility check
  and an exact assertion for 15 Hoàng DRAFT assignments.
- Test suite maintenance now follows the current production templates and
  service rules: stale announcement, supervisor, user-management, and topic-
  publication assertions were updated; pagination fixtures use the current
  page size; topic-registration fixtures assign matching student/topic
  departments; schema SQL assertions normalize line endings; and test H2
  databases are unique per Spring context to prevent `create-drop` collisions.
- Latest verification: `mvn.cmd -B test` passed (195 tests, 0 failures, 0
  errors), and `git diff --check` passed.
- Faculty registration review now gates approval behind a shared confirmation
  modal. The final confirmation submits the existing CSRF-protected approval
  form, while cancel, close, and Escape leave the registration unchanged.
- Latest verification: `mvn.cmd -B -Dtest=TopicRegistrationReviewControllerTest
  test` passed (6 tests, 0 failures), `mvn.cmd -B test` passed (195 tests, 0
  failures, 0 errors), and `git diff --check` passed.
- Review-board action menus use Flowbite's `bottom-end` placement and allow
  overflow on desktop table layouts, so the menu opens below the action button
  without being clipped by the table container; narrow screens retain
  horizontal table scrolling.
- Latest verification: `mvn.cmd -B -Dtest=ReviewBoardControllerTest test`
  passed (11 tests, 0 failures), `node --check src/main/resources/static/js/app.js`
  passed, and `git diff --check` passed.
- Final UI verification after removing the desktop table overflow conflict:
  the Review boards action menu is configured to open below the button with
  all available actions, while the focused controller test still passes (11
  tests, 0 failures).
- Student registration summaries now expose whether the associated result is
  published. The student registration page hides the report-upload button and
  modal after Faculty Head publication, while the report service rejects
  direct upload attempts after publication as well.
- Latest verification: `mvn.cmd -B -Dtest=ReportServiceStorageFailureTest,ReportControllerTest,TopicRegistrationControllerTest test`
  passed (18 tests, 0 failures), and `mvn.cmd -B test` passed (197 tests, 0
  failures, 0 errors).
- Review-board department filter now has a fixed `w-56 flex-none` width so the
  `All departments` label and longer department options do not collapse into
  the adjacent status filter.
- Latest verification: `mvn.cmd -B -Dtest=ReviewBoardControllerTest test`
  passed (11 tests, 0 failures), and `git diff --check` passed.
- Follow-up UI adjustment changes the review-board three-dot action menu
  placement from `top-end` to `bottom-end`, so it opens below the clicked
  button while retaining right-edge alignment.
- Latest verification: `mvn.cmd -B -Dtest=ReviewBoardControllerTest test`
  passed (11 tests, 0 failures), and `git diff --check` passed.
