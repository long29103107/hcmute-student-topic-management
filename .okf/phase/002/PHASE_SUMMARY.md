---
phase: 002
title: Academic Workflow — Topic Proposal Foundation
status: complete
created_at: 2026-09-04
updated_at: 2026-09-04
current_task: 002_001
task_count: 1
done_count: 1
depends_on: [001]
---

# Phase 002 Summary

## Vision alignment

This phase advances the academic-workflow roadmap in `PRODUCT_VISION.md` by
explicit user selection after completion of Phase 001. The selected Project #5
task is the topic-proposal foundation needed before supervisor assignment,
review and publication.

## Phase Goal

Give Lecturers and Faculty Heads with the Lecturer proposal permission a
server-authorized way to create, view and edit their own topic proposals.

## Scope

In: own-proposal list, create/edit form, validation, department and lecturer
registration-period checks, SSR and REST adapters, focused authorization tests.

Out: registration-period administration, supervisor assignment, review,
approval, publication, groups, registrations, reports and evaluations.

## Task Index

| Task | Title | Status | Done At |
|---|---|---|---|
| 002_001 | Topic Proposal CRUD | completed | 2026-09-04 |

## Verification Evidence

- `mvn -Dtest=TopicProposalControllerTest test` — pass, 6 tests.
- `mvn test` — pass, 64 tests, 0 failures, 0 errors, 0 skipped.
- `git diff --check` — pass; only normal Git LF/CRLF conversion warnings were
  emitted by the status/diff inspection.
