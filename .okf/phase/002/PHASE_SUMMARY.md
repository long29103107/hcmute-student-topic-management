---
phase: 002
title: Academic Workflow — Topic Proposal and Supervisor Foundation
status: in_progress
created_at: 2026-09-04
updated_at: 2026-09-04
current_task: 002_002
task_count: 2
done_count: 2
depends_on: [001]
---

# Phase 002 Summary

## Vision alignment

This phase advances the academic-workflow roadmap in `PRODUCT_VISION.md` by
explicit user selection after completion of Phase 001. The selected Project #5
tasks establish the topic-proposal and supervisor-assignment foundation needed
before review and publication.

## Phase Goal

Give Lecturers and Faculty Heads with the Lecturer proposal permission a
server-authorized way to create, view and edit their own topic proposals, then
let Admin and Faculty Head users assign valid supervisors within their scope.

## Scope

In: own-proposal list, create/edit form, supervisor assignment, validation,
department and lecturer registration-period checks, SSR and REST adapters,
permission/seed updates and focused authorization tests.

Out: registration-period administration, topic review, approval, publication,
groups, registrations, reports and evaluations.

## Task Index

| Task | Title | Status | Done At |
|---|---|---|---|
| 002_001 | Topic Proposal CRUD | completed | 2026-09-04 |
| 002_002 | Topic Supervisor Assignment | completed | 2026-09-04 |

## Verification Evidence

- `mvn -Dtest=TopicProposalControllerTest test` — pass, 6 tests.
- `mvn -Dtest=TopicSupervisorControllerTest test` — pass, 6 tests.
- `mvn test` — pass, 87 tests, 0 failures, 0 errors, 0 skipped.
- `mvn package -DskipTests` — pass; executable Spring Boot JAR created.
- `git diff --check` — pass; only normal Git LF/CRLF conversion warnings were
  emitted by the status/diff inspection.

## Next Task Proposal

`002_003 — Topic Review, Approve and Reject` (#6), subject to explicit task
selection after the supervisor-assignment ticket is closed.
