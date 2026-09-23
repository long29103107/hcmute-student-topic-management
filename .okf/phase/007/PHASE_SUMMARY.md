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
