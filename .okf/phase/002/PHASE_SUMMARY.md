---
phase: 002
title: Registration Periods, Topics and Announcements
status: planned
created_at: 2026-08-20
updated_at: 2026-08-20
current_task: null
task_count: 6
done_count: 0
depends_on: [001]
---

# Phase 002 Summary

## Phase Goal

Cho Faculty Head tạo đợt với hai cửa sổ thời gian, lecturer đề xuất topic,
Faculty Head duyệt/công bố, và người dùng xem announcements đã công bố.

## Phase Done Criteria

- Period lưu đủ loại và các mốc theo `REQUEST.md`.
- Service chặn đăng ký ngoài thời gian và validate field applicability.
- Topic có đúng một department, 1–2 supervisors và chỉ topic published mới
  xuất hiện trong luồng student.
- Faculty Head duyệt/từ chối/công bố được topic.
- Announcement draft/hidden/published hoạt động cơ bản.

## Scope

In: `registration_periods`, topics, supervisors, announcements, time/status
rules and SSR screens.

Out: student groups, registrations, reports, boards, scoring.

## Task Index

| Task | Title | Status | Done At |
|---|---|---|---|
| 002_001 | Period model/schema/form | planned | |
| 002_002 | Period time gates and applicability | planned | |
| 002_003 | Lecturer topic proposal and supervisors | planned | |
| 002_004 | Faculty approval and publication | planned | |
| 002_005 | Announcement management and public view | planned | |
| 002_006 | Phase verification and closure | planned | |

## Current Task

No task is active. Next proposal: `002_001` after Phase 001 closes.

## Completed Notes

No phase tasks are complete yet.

## Next Task Proposal

Start with period schema and forms; all later topic gates depend on one period
service rather than duplicated date checks.

## Task Notes

### 002_001 - Period model/schema/form

#### Step Goal

Implement period entity/DAO/service and Faculty Head SSR create/edit/list flow
with all required date fields and type enum.

#### Dependency

- Phase 001 foundation and departments.

#### Scope

In: type allowlist, lecturer/student windows, nullable reviewer deadline and
council date, form validation and PRG.

Out: topic and group registration behavior.

#### Acceptance Criteria

- Invalid start/end order is rejected.
- Reviewer deadline is only accepted for TLCN/KLTN.
- Council date is only accepted for KLTN.
- Only Faculty Head can mutate periods.

#### Affected Files

- `RegistrationPeriod` model/DAO/Service/Controller/JSP, REST adapter, SQL and tests.

#### Verification

- Service boundary tests and MySQL DAO tests; `mvn test/package`.

#### Foundation for Next Step

One reusable `RegistrationWindowService` can gate all later actions.

#### Done Notes

Not started.

### 002_002 - Period time gates and applicability

#### Step Goal

Centralize server-side checks for lecturer/student windows and period state.

#### Dependency

- `002_001`.

#### Scope

In: injected clock, inclusive boundary behavior, inactive/closed periods,
reviewer deadline lookup and tests.

Out: client-only countdowns, background schedulers.

#### Acceptance Criteria

- Every service action uses server time, not browser-submitted state.
- Boundary and timezone behavior is covered by tests.
- The open policy for council/report behavior is visible in config/docs.

#### Affected Files

- period policy service, config, tests, `docs/workflows.md`.

#### Verification

- Clock-based unit tests, `mvn test`.

#### Foundation for Next Step

Topic proposal and student registration use the same time policy.

#### Done Notes

Not started.

### 002_003 - Lecturer topic proposal and supervisors

#### Step Goal

Lecturer creates topic in an active lecturer window and links 1–2 lecturers as
supervisors.

#### Dependency

- `002_002`.

#### Scope

In: topic/subject fields, department/period lookup, supervisor link transaction,
draft/pending statuses and validation.

Out: approval/publication UI.

#### Acceptance Criteria

- Topic has exactly one department and matching period.
- Service enforces 1–2 supervisors and lecturer eligibility.
- Non-lecturer or out-of-window requests fail server-side.

#### Affected Files

- topic/supervisor model, DAO, Service, lecturer Controller/JSP, REST adapter and tests.

#### Verification

- Transaction and cardinality tests; `mvn test/package`.

#### Foundation for Next Step

Faculty Head can review a stable topic aggregate.

#### Done Notes

Not started.

### 002_004 - Faculty approval and publication

#### Step Goal

Faculty Head reviews pending topics, approves/rejects them and publishes the
approved list.

#### Dependency

- `002_003`.

#### Scope

In: state transitions, rejection reason storage, publication visibility and
direct URL authorization.

Out: student registration mutation.

#### Acceptance Criteria

- Invalid transitions are rejected.
- Students see only published topics in the matching period.
- Rejected/draft topics cannot be selected.

#### Affected Files

- topic review Service/Controller/JSP, REST adapter, DAO, tests and route docs.

#### Verification

- State transition/visibility tests and Tomcat smoke; `mvn test/package`.

#### Foundation for Next Step

Phase 003 can treat published topic as the only selectable topic source.

#### Done Notes

Not started.

### 002_005 - Announcement management and public view

#### Step Goal

Admin/Faculty Head create, edit, hide and publish announcements; authenticated
users view only published items.

#### Dependency

- `001_003` and account authorization.

#### Scope

In: announcement schema/DAO/Service/Controller/JSP, optional REST adapter, scope field, status and
escaped rendering.

Out: email delivery and audit log.

#### Acceptance Criteria

- Unauthorized users cannot mutate announcements.
- Hidden/draft content is not shown on public list.
- Content is escaped in JSP.

#### Affected Files

- announcement model/DAO/Service/Controller/JSP, REST adapter and tests.

#### Verification

- Authorization/XSS rendering tests and Tomcat smoke; `mvn test/package`.

#### Foundation for Next Step

All roles have a stable announcement surface for later phases.

#### Done Notes

Not started.

### 002_006 - Phase verification and closure

#### Step Goal

Verify period/topic/announcement flows and close Phase 002.

#### Dependency

- `002_001` through `002_005`.

#### Scope

In: full tests, role smoke, time gate evidence and phase docs.

Out: new feature work.

#### Acceptance Criteria

- Done criteria have repeatable evidence.
- Open questions remain documented and no unsupported rule is claimed.

#### Affected Files

- tests, docs and this summary.

#### Verification

- `mvn test`, `mvn package`, MySQL/Tomcat smoke when available.

#### Foundation for Next Step

Published topics and periods are ready for student groups/registrations.

#### Done Notes

Not started.
