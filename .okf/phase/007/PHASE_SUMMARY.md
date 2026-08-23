---
phase: 007
title: Should Have Usability Improvements
status: planned
created_at: 2026-08-20
updated_at: 2026-08-20
current_task: null
task_count: 6
done_count: 0
depends_on: [006]
---

# Phase 007 Summary

## Phase Goal

Chỉ sau khi Phase 006 hoàn tất, bổ sung các tiện ích Should Have mà không tạo
business path thứ hai hoặc thay đổi Must Have rules.

## Phase Done Criteria

- Search/filter, pagination, rejection reasons and deadline display work through
  the same Service contracts.
- Report re-submit, score lock, responsive layout and client validation are
  enabled only where policy allows.
- Server-side validation remains authoritative.

## Scope

In: Should Have list from `REQUEST.md`.

Out: dashboard, audit log, email, AJAX search and multiple report versions
unless separately approved in Phase 008.

## Task Index

| Task | Title | Status | Done At |
|---|---|---|---|
| 007_001 | Search/filter and pagination | planned | |
| 007_002 | Rejection reasons and deadline presentation | planned | |
| 007_003 | Report resubmission policy | planned | |
| 007_004 | Score locking after publication | planned | |
| 007_005 | Responsive UI and client validation | planned | |
| 007_006 | Phase verification | planned | |

## Current Task

No task is active. Next proposal: `007_001` only after Must Have closure.

## Completed Notes

No phase tasks are complete yet.

## Next Task Proposal

Start with server-side search/filter/pagination reuse; it is the least
policy-sensitive improvement.

## Task Notes

### 007_001 - Search/filter and pagination

#### Step Goal

Add search/filter by period, type, department and status plus pagination to
existing list services and JSPs.

#### Dependency

- Phase 006 complete.

#### Scope

In: allowlisted filter fields, DAO pagination, query DTOs and accessible UI.

Out: AJAX-only implementation or new search index.

#### Acceptance Criteria

- Filters use PreparedStatement and allowlists.
- Pagination does not bypass authorization or status visibility.
- Existing SSR route remains functional.

#### Affected Files

- list DTO/DAO/Service/Controller/JSP/REST/tests/docs.

#### Verification

- Query/authorization tests, `mvn test/package`.

#### Foundation for Next Step

All list screens share predictable filter/pagination contracts.

#### Done Notes

Not started.

### 007_002 - Rejection reasons and deadline presentation

#### Step Goal

Show rejection reasons and clear registration/scoring deadlines where data is
already available.

#### Dependency

- `007_001`.

#### Scope

In: view models, Vietnamese labels, safe reason rendering and date formatting.

Out: changing approval policy.

#### Acceptance Criteria

- Reasons are escaped and visible only to permitted users.
- Deadline display uses server/configured timezone consistently.

#### Affected Files

- view DTOs, JSP fragments, tests/docs.

#### Verification

- View/security tests and Tomcat smoke; `mvn test/package`.

#### Foundation for Next Step

Users understand why a flow is blocked before optional resubmit behavior.

#### Done Notes

Not started.

### 007_003 - Report resubmission policy

#### Step Goal

If lecturer confirms it, allow leader to replace a report before the permitted
deadline while preserving file cleanup rules.

#### Dependency

- `007_002` and open questions 5.

#### Scope

In: explicit config/state, transaction/file cleanup and leader/deadline checks.

Out: multiple historical versions unless separately selected.

#### Acceptance Criteria

- Feature remains disabled or fails closed when policy is absent.
- Non-leader/late resubmission is rejected.

#### Affected Files

- report Service/DAO/storage/Controller/JSP/REST/tests/docs.

#### Verification

- File replacement/rollback tests and Tomcat smoke; `mvn test/package`.

#### Foundation for Next Step

Report lifecycle has an explicit policy boundary.

#### Done Notes

Not started.

### 007_004 - Score locking after publication

#### Step Goal

Prevent score/result edits after final publication through Service and DAO
state gates.

#### Dependency

- Phase 005 publication.

#### Scope

In: state gate, UI read-only state and regression tests.

Out: audit log.

#### Acceptance Criteria

- Direct POST cannot edit a published result.
- Before publication, authorized editing still works.

#### Affected Files

- score/result Service/DAO/Controller/JSP/REST/tests.

#### Verification

- State transition tests; `mvn test/package`.

#### Foundation for Next Step

UI polish can reflect immutable published state.

#### Done Notes

Not started.

### 007_005 - Responsive UI and client validation

#### Step Goal

Improve desktop/mobile layout and add client-side validation without removing
server checks.

#### Dependency

- `007_004`.

#### Scope

In: Tailwind responsive utilities, jQuery validation helpers and shared form
messages.

Out: SPA migration or client-owned authorization/business rules.

#### Acceptance Criteria

- Core flows are usable on desktop/mobile widths.
- Forged requests still fail server-side.

#### Affected Files

- JSP fragments, Tailwind assets, jQuery scripts and browser checks.

#### Verification

- `mvn package`, Tomcat smoke and manual responsive check.

#### Foundation for Next Step

All Should Have UI changes keep one SSR contract.

#### Done Notes

Not started.

### 007_006 - Phase verification

#### Step Goal

Verify selected Should Have tasks without weakening Must Have behavior.

#### Dependency

- Selected tasks `007_001` through `007_005`.

#### Scope

In: regression tests and docs.

Out: Nice to Have backlog.

#### Acceptance Criteria

- Each selected task has evidence; unselected tasks remain planned.

#### Affected Files

- tests/docs/this summary.

#### Verification

- `mvn test`, `mvn package`, relevant Tomcat smoke.

#### Foundation for Next Step

Optional Phase 008 can be considered independently.

#### Done Notes

Not started.
