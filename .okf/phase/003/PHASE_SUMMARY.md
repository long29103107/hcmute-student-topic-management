---
phase: 003
title: Student Groups and Topic Registration
status: planned
created_at: 2026-08-20
updated_at: 2026-08-20
current_task: null
task_count: 6
done_count: 0
depends_on: [002]
---

# Phase 003 Summary

## Phase Goal

Sinh viên tạo/tham gia nhóm, hệ thống giữ đúng giới hạn thành viên/leader và
group leader đăng ký duy nhất một topic published; Faculty Head duyệt đăng ký.

## Phase Done Criteria

- Group tối đa 3 sinh viên và đúng 1 leader.
- Student không tham gia trùng group theo rule hiện hành.
- Chỉ leader gửi registration trong student window.
- Topic phải published và thuộc đúng period.
- Faculty Head approve/reject; trạng thái hiển thị đúng cho group.

## Scope

In: groups, members, leader, topic registrations, approval and student SSR.

Out: report uploads, review boards, scores and final results.

## Task Index

| Task | Title | Status | Done At |
|---|---|---|---|
| 003_001 | Group and membership model | planned | |
| 003_002 | Leader and membership transaction rules | planned | |
| 003_003 | Leader topic registration | planned | |
| 003_004 | Faculty registration approval | planned | |
| 003_005 | Student status/detail views | planned | |
| 003_006 | Phase verification and closure | planned | |

## Current Task

No task is active. Next proposal: `003_001`.

## Completed Notes

No phase tasks are complete yet.

## Next Task Proposal

Start with group/member persistence and a transaction seam; registration depends
on trustworthy leader and membership state.

## Task Notes

### 003_001 - Group and membership model

#### Step Goal

Create group/member schema, DAO, Service and student SSR screen for creating a
group and viewing current membership.

#### Dependency

- Phase 002 published topics/periods and Phase 001 student identities.

#### Scope

In: group creation with creator as leader, member list and safe display DTOs.

Out: invitation policy, topic registration and report submission.

#### Acceptance Criteria

- Creator is the initial/only leader in a valid group.
- No password or unrelated user data is exposed in member view.
- Direct URL access is limited to group members/authorized academic roles.

#### Affected Files

- `StudentGroup`, `GroupMember`, DAO/Service, group Controller/JSP, REST adapter,
  schema/tests.

#### Verification

- Service/DAO tests and `mvn test/package`.

#### Foundation for Next Step

Leader changes and membership actions operate on one aggregate transaction.

#### Done Notes

Not started.

### 003_002 - Leader and membership transaction rules

#### Step Goal

Enforce max three members, exactly one leader and no duplicate student
membership, with race-safe transaction handling.

#### Dependency

- `003_001`.

#### Scope

In: join/leave/leader change commands as allowed by chosen interim membership
policy, row locking/unique checks and rollback tests.

Out: unresolved invite/confirmation behavior becoming mandatory.

#### Acceptance Criteria

- A fourth member is rejected.
- A group cannot be saved without exactly one leader.
- A student cannot be active member of two groups.
- Failed multi-write operations leave no partial membership.

#### Affected Files

- group Service/DAO/transaction helper, config/docs and tests.

#### Verification

- Boundary/concurrency-oriented service tests and MySQL transaction test when
  available; `mvn test/package`.

#### Foundation for Next Step

Leader-only topic registration has a reliable authorization precondition.

#### Done Notes

Not started.

### 003_003 - Leader topic registration

#### Step Goal

Allow only a group leader to submit one registration for one published topic in
the current period and student window.

#### Dependency

- `003_002` and `002_004`.

#### Scope

In: topic selection screen, registration command, matching period/status/date
checks, unique registration transaction.

Out: approval/rejection management.

#### Acceptance Criteria

- Non-leader direct POST fails.
- Unpublished/wrong-period topic fails.
- Out-of-window request fails.
- Duplicate registration for a group fails atomically.

#### Affected Files

- `TopicRegistration` model/DAO/Service, student Controller/JSP, REST adapter,
  tests.

#### Verification

- Service authorization/time/status tests, MySQL unique test, `mvn test/package`.

#### Foundation for Next Step

Faculty Head receives a queue of valid pending registrations.

#### Done Notes

Not started.

### 003_004 - Faculty registration approval

#### Step Goal

Faculty Head approves/rejects pending group registrations and records safe
status/reason information.

#### Dependency

- `003_003`.

#### Scope

In: review list/detail, state transitions, optional rejection reason and PRG.

Out: multiple groups per topic decision beyond the open policy.

#### Acceptance Criteria

- Only Faculty Head can decide.
- Only pending registration can transition.
- Group sees the decision and cannot submit a second registration.

#### Affected Files

- registration review Service/Controller/JSP/REST/DAO/tests.

#### Verification

- State/authorization tests and Tomcat smoke; `mvn test/package`.

#### Foundation for Next Step

Approved registration is the gate for report submission in Phase 004.

#### Done Notes

Not started.

### 003_005 - Student status/detail views

#### Step Goal

Provide a student-facing view of group membership, selected topic and
registration status without leaking other groups’ data.

#### Dependency

- `003_004`.

#### Scope

In: view models, safe filters/labels, status history if available.

Out: AJAX search and advanced pagination.

#### Acceptance Criteria

- Student sees only own group/registration.
- Published topic list and registration state are consistent after refresh.
- JSP output is escaped.

#### Affected Files

- student Controllers/JSP, REST view DTOs, tests and route docs.

#### Verification

- Ownership tests and Tomcat smoke; `mvn test/package`.

#### Foundation for Next Step

Members and approved registration are discoverable for report flow.

#### Done Notes

Not started.

### 003_006 - Phase verification and closure

#### Step Goal

Verify all group/registration acceptance criteria and close Phase 003.

#### Dependency

- `003_001` through `003_005`.

#### Scope

In: full test suite, role/direct URL/time-window smoke and docs.

Out: report/board/score features.

#### Acceptance Criteria

- No duplicate registration or membership loophole remains in tested paths.
- Open member-policy and multiple-group questions remain explicit.

#### Affected Files

- tests, docs and this summary.

#### Verification

- `mvn test`, `mvn package`, MySQL/Tomcat smoke when available.

#### Foundation for Next Step

Phase 004 can safely authorize report upload and academic assignments.

#### Done Notes

Not started.
