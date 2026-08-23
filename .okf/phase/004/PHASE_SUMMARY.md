---
phase: 004
title: Reports and Lecturer Assignments
status: planned
created_at: 2026-08-20
updated_at: 2026-08-20
current_task: null
task_count: 5
done_count: 0
depends_on: [003]
---

# Phase 004 Summary

## Phase Goal

Nhóm trưởng của registration đã được chấp thuận nộp report an toàn; Faculty
Head quản lý GVHD/GVPB/người chấm và các role liên quan xem report đúng quyền.

## Phase Done Criteria

- Only approved group leader can submit.
- Report metadata/bytes are persisted with safe generated storage path.
- File type/size come from config and invalid upload is rejected.
- Members, supervisors and assigned graders can view/download; unauthorized
  users cannot.
- A lecturer is never assigned to grade a topic they supervise.

## Scope

In: reports, configured upload boundary, supervisor/reviewer assignments.

Out: review board composition and score calculation (Phase 005).

## Task Index

| Task | Title | Status | Done At |
|---|---|---|---|
| 004_001 | Report metadata and safe storage boundary | planned | |
| 004_002 | Leader-only upload and report access | planned | |
| 004_003 | Supervisor assignment management | planned | |
| 004_004 | Reviewer/grader assignment and conflict rule | planned | |
| 004_005 | Phase verification and closure | planned | |

## Current Task

No task is active. Next proposal: `004_001`.

## Completed Notes

No phase tasks are complete yet.

## Next Task Proposal

Start by resolving metadata/storage and configured upload policy; the exact
extension/size remains an open question and must not be guessed.

## Task Notes

### 004_001 - Report metadata and safe storage boundary

#### Step Goal

Implement report table/DAO, upload config and non-public generated storage path
with cleanup-aware transaction coordination.

#### Dependency

- Phase 003 approved registration.

#### Scope

In: metadata mapping, size/type allowlists from config, temporary name and
storage service seam.

Out: choosing unconfirmed file policy, public static file serving.

#### Acceptance Criteria

- Original filename is display metadata only, never a path.
- Missing upload config fails closed with a useful admin/developer error.
- SQL metadata uses PreparedStatement and transaction-safe cleanup.

#### Affected Files

- `Report` model/DAO, `FileStorageService`, config, SQL, tests.

#### Verification

- File validation/path traversal tests, DAO tests, `mvn test/package`.

#### Foundation for Next Step

Leader upload Controller/REST adapter can call one safe report service.

#### Done Notes

Not started.

### 004_002 - Leader-only upload and report access

#### Step Goal

Implement report submission and view/download routes with approved registration
and resource-level authorization.

#### Dependency

- `004_001`.

#### Scope

In: leader-only POST, multipart parsing, permission checks for members/GVHD/
assigned graders, download response headers.

Out: report re-submit before deadline unless confirmed as Should Have.

#### Acceptance Criteria

- Non-leader, unapproved group and anonymous requests fail.
- Stored bytes are not directly reachable from public web root.
- Download does not leak filesystem path or unsafe content disposition.

#### Affected Files

- report Service/Controller/REST/filter/JSP, tests, `docs/ui-route-map.md`.

#### Verification

- Authorization/file upload tests and Tomcat smoke; `mvn test/package`.

#### Foundation for Next Step

Academic roles can inspect the same report through a controlled service.

#### Done Notes

Not started.

### 004_003 - Supervisor assignment management

#### Step Goal

Faculty Head can review/set 1–2 GVHD for each topic while preserving topic
supervisor invariants.

#### Dependency

- Phase 002 topic supervisors.

#### Scope

In: assignment UI, transaction update, lecturer eligibility and report access
relationship.

Out: reviewer board membership.

#### Acceptance Criteria

- Topic always has 1–2 valid supervisors after mutation.
- Faculty Head only mutation path.
- Supervisor assignment cannot be used to bypass score conflict rule.

#### Affected Files

- assignment DAO/Service/Controller/JSP/REST/tests.

#### Verification

- Cardinality/authorization tests; `mvn test/package`.

#### Foundation for Next Step

Reviewer assignment can query supervisors for conflict detection.

#### Done Notes

Not started.

### 004_004 - Reviewer/grader assignment and conflict rule

#### Step Goal

Faculty Head assigns GVPB and graders to topics and blocks any supervisor from
being a grader of that topic.

#### Dependency

- `004_003`.

#### Scope

In: assignment table/DAO/Service/Controller/JSP/REST, multiple-topic assignments,
conflict validation and assignment listing.

Out: board composition and final score.

#### Acceptance Criteria

- One lecturer may be assigned to many topics.
- Supervisor conflict fails on both UI and direct POST.
- Assigned lecturer can see only assigned grading work.

#### Affected Files

- `ReviewerAssignment` model/DAO/Service/Controller/JSP/REST/tests.

#### Verification

- Conflict/multi-assignment tests and Tomcat smoke; `mvn test/package`.

#### Foundation for Next Step

Phase 005 can compose boards from conflict-safe assignment data.

#### Done Notes

Not started.

### 004_005 - Phase verification and closure

#### Step Goal

Verify report and assignment requirements and close Phase 004.

#### Dependency

- `004_001` through `004_004`.

#### Scope

In: full tests, upload/access/conflict smoke and docs.

Out: score entry and result publication.

#### Acceptance Criteria

- File policy uncertainty is not silently replaced by a hard-coded allowlist.
- Report access and supervisor conflict are covered by evidence.

#### Affected Files

- tests, docs and this summary.

#### Verification

- `mvn test`, `mvn package`, MySQL/Tomcat smoke when available.

#### Foundation for Next Step

Review boards and score workflows have authorized reports and assignments.

#### Done Notes

Not started.
