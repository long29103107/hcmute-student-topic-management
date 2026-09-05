---
phase: 002
title: Academic Workflow — Topic Proposal and Supervisor Foundation
status: in_progress
created_at: 2026-09-04
updated_at: 2026-09-05
current_task: 002_003
task_count: 3
done_count: 3
depends_on: [001]
---

# Phase 002 Summary

## Vision alignment

This phase advances the academic-workflow roadmap in `PRODUCT_VISION.md` by
explicit user selection after completion of Phase 001. The selected Project #5
tasks establish the topic-proposal, supervisor-assignment and faculty-review
foundation needed before publication.

## Phase Goal

Give Lecturers and Faculty Heads with the Lecturer proposal permission a
server-authorized way to create, view and edit their own topic proposals, let
Admin and Faculty Head users assign valid supervisors within their scope, and
let authorized reviewers decide pending proposals.

## Scope

In: own-proposal list, create/edit form, supervisor assignment, topic review,
validation, department and lecturer registration-period checks, SSR and REST
adapters, permission/seed updates and focused authorization tests.

Out: registration-period administration, topic publication, groups,
registrations, reports and evaluations.

## Task Index

| Task | Title | Status | Done At |
|---|---|---|---|
| 002_001 | Topic Proposal CRUD | completed | 2026-09-04 |
| 002_002 | Topic Supervisor Assignment | completed | 2026-09-04 |
| 002_003 | Topic Review, Approve and Reject | completed | 2026-09-05 |

## Verification Evidence

- `mvn -Dtest=TopicProposalControllerTest test` — pass, 6 tests.
- `mvn -Dtest=TopicSupervisorControllerTest test` — pass, 6 tests.
- `mvn -Dtest=DatabaseSchemaServiceTest,DatabaseSeedControllerTest,TopicReviewControllerTest` — pass, 9 tests.
- `mvn test` — pass, 92 tests, 0 failures, 0 errors, 0 skipped.
- `mvn package -DskipTests` — pass; executable Spring Boot JAR created.
- `git diff --check` — pass; only normal Git LF/CRLF conversion warnings were
  emitted by the status/diff inspection.

## Delivered outcome

`002_003` adds the pending topic-review queue and server-enforced
`PENDING_APPROVAL -> APPROVED|REJECTED` decision contract for Admin and Faculty
Head users. The local seed pipeline now drops and recreates all 17 revised
schema tables before restoring the permission, account and topic fixtures.
Publication remains a later task.
