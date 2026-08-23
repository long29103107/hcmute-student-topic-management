---
phase: 005
title: Review Boards, Scoring and Final Results
status: planned
created_at: 2026-08-20
updated_at: 2026-08-20
current_task: null
task_count: 6
done_count: 0
depends_on: [004]
---

# Phase 005 Summary

## Phase Goal

Faculty Head tạo hội đồng 3–5 lecturer có đúng chair/secretary, gán topic,
lecturer nhập điểm/nhận xét, chair tổng hợp và Faculty công bố kết quả.

## Phase Done Criteria

- Board cardinality and roles are enforced transactionally.
- Topic-board assignment exists before scoring.
- Supervisor cannot score own topic.
- Score input validates the confirmed score policy and deadline.
- Final average is calculated from component scores without hard-coded
  unconfirmed scale/rounding.
- Published results are visible only to the participating student group.

## Scope

In: review boards, members, topic assignments, scores, final results and
publication.

Out: dashboard, email, audit log and advanced analytics.

## Task Index

| Task | Title | Status | Done At |
|---|---|---|---|
| 005_001 | Board and member management | planned | |
| 005_002 | Board-topic assignment | planned | |
| 005_003 | Score entry and conflict/deadline checks | planned | |
| 005_004 | Chair aggregation and average policy | planned | |
| 005_005 | Faculty publication and student result view | planned | |
| 005_006 | Phase verification and closure | planned | |

## Current Task

No task is active. Next proposal: `005_001`.

## Completed Notes

No phase tasks are complete yet.

## Next Task Proposal

Start with board/member aggregate and constraints; score entry must never run
against an invalid board.

## Task Notes

### 005_001 - Board and member management

#### Step Goal

Implement board schema, member roles and Faculty Head SSR create/edit flow.

#### Dependency

- Phase 004 assignments and Phase 001 identities.

#### Scope

In: board/member DAO/Service/Controller/JSP/REST, role selection and transactional
validation for 3–5 members, one chair and one secretary.

Out: topic assignment and scoring.

#### Acceptance Criteria

- Invalid member count or duplicate lecturer is rejected.
- Chair/secretary belong to board and are unique.
- Only Faculty Head mutates board.

#### Affected Files

- board models/DAO/Service/Controller/JSP/REST, SQL and tests.

#### Verification

- Cardinality/role/transaction tests; `mvn test/package`.

#### Foundation for Next Step

A valid board is available for topic assignment.

#### Done Notes

Not started.

### 005_002 - Board-topic assignment

#### Step Goal

Assign approved/eligible topics to a board and expose the assignment to
members/graders.

#### Dependency

- `005_001`.

#### Scope

In: assignment DAO/Service/Controller/JSP/REST, period/topic consistency and duplicate
prevention.

Out: final result publication.

#### Acceptance Criteria

- Only permitted topic/period combinations are assignable.
- Unpublished or unapproved topics cannot enter scoring.
- Board members see assigned topics appropriate to their role.

#### Affected Files

- board-topic assignment code, tests and route docs.

#### Verification

- State/authorization tests and Tomcat smoke; `mvn test/package`.

#### Foundation for Next Step

Score Service can resolve board and topic conflict context.

#### Done Notes

Not started.

### 005_003 - Score entry and conflict/deadline checks

#### Step Goal

Assigned lecturer enters score/comment for an assigned topic with server-side
supervisor conflict and applicable deadline checks.

#### Dependency

- `005_002` and Phase 004 reviewer assignments.

#### Scope

In: score command, component fields, assignment authorization, conflict,
deadline and validation error rendering.

Out: choosing unconfirmed grading scale/rounding.

#### Acceptance Criteria

- Unassigned lecturer cannot submit.
- Supervisor cannot score own topic through UI or direct POST.
- Invalid score values are rejected according to configured policy.

#### Affected Files

- score model/DAO/Service/Controller/JSP/REST/config/tests.

#### Verification

- Conflict/deadline tests, MySQL transaction test and `mvn test/package`.

#### Foundation for Next Step

Chair can aggregate a complete authorized score set.

#### Done Notes

Not started.

### 005_004 - Chair aggregation and average policy

#### Step Goal

Chair reviews score completeness and produces a final result using the
confirmed average policy while keeping scale/rounding configurable.

#### Dependency

- `005_003`.

#### Scope

In: aggregation DAO/Service, chair authorization, average calculation,
incomplete-score handling and result transaction.

Out: email/notifications and analytics.

#### Acceptance Criteria

- Only chair/authorized Faculty Head can aggregate.
- Final score is arithmetic mean of required component scores.
- Missing grading policy does not silently yield a misleading result.

#### Affected Files

- result model/DAO/Service, grading policy config and tests.

#### Verification

- Numeric calculation/rounding-policy tests and rollback test; `mvn test`.

#### Foundation for Next Step

Faculty Head can publish a stable final result.

#### Done Notes

Not started.

### 005_005 - Faculty publication and student result view

#### Step Goal

Faculty Head publishes completed results; participating group members view only
their own topic result.

#### Dependency

- `005_004`.

#### Scope

In: publication state, result visibility Service/Controller/JSP/REST and read-only
state after publication.

Out: dashboard statistics.

#### Acceptance Criteria

- Unpublished results are hidden from students.
- Student direct URL cannot access another group’s result.
- Publication is authorized and transaction-safe.

#### Affected Files

- result publication code, student/faculty JSPs and tests.

#### Verification

- Ownership/publication tests and Tomcat smoke; `mvn test/package`.

#### Foundation for Next Step

All core academic workflow capabilities are available for end-to-end closure.

#### Done Notes

Not started.

### 005_006 - Phase verification and closure

#### Step Goal

Verify board, scoring, aggregation and result privacy criteria.

#### Dependency

- `005_001` through `005_005`.

#### Scope

In: complete tests and multi-role smoke.

Out: Should/Nice to Have polish.

#### Acceptance Criteria

- Board size/roles, supervisor conflict, average and result privacy have
  evidence.
- Unconfirmed grade policy remains visible as a blocker/configuration.

#### Affected Files

- tests, docs and this summary.

#### Verification

- `mvn test`, `mvn package`, MySQL/Tomcat smoke when available.

#### Foundation for Next Step

Phase 006 can run Must Have end-to-end closure and hardening.

#### Done Notes

Not started.
