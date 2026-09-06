---
phase: 004
task: 004_003
title: Evaluator Assignment
status: completed
completed_at: 2026-09-05
---

# 004_003 — Evaluator Assignment

## Vision alignment

This task implements the MVP evaluation handoff after a Faculty Head approves
a student topic registration. It creates the evaluator relationship required
before score entry while leaving scoring, aggregation and result publication to
the later Phase 004 tasks.

## Dependencies

- `004_002` report access and the approved-registration relationship contract.
- Existing `REGISTRATION_REVIEW` permission and Faculty Head/Admin department
  authorization.
- Revised `evaluations` table and lecturer-level evaluation entity.

## Scope

In:

- Faculty Head/Admin evaluator queue for approved registrations.
- Assign/change one evaluator through the `evaluations` table.
- Active Lecturer/Faculty Head candidate validation scoped to the topic's
  department.
- Supervisor conflict, approved-only, department scope and duplicate-row
  protection.
- SSR modal UI, REST list/update API, sidebar entry and request-level tests.

Out:

- Score/comment entry, score deadlines, average calculation and result
  publication.
- Full 3–5 member review-board workflow, chair/secretary roles and board
  scheduling.

## Acceptance mapping

- Faculty Head can assign a valid evaluator in their department scope; Admin
  can operate across departments.
- Evaluators must be active Lecturer or Faculty Head accounts from the same
  department as the topic and cannot be a supervisor for the same topic.
- Only approved registrations are eligible.
- Reassignment updates the current evaluation row; repeated assignment does
  not create duplicate rows.
- SSR and REST use the same Service authorization and validation rules.

## Affected files

- `src/main/java/com/hcmute/topicmanagement/service/EvaluatorAssignmentService.java`
- `src/main/java/com/hcmute/topicmanagement/repository/EvaluationRepository.java`
- `src/main/java/com/hcmute/topicmanagement/web/controller/EvaluatorAssignmentController.java`
- `src/main/java/com/hcmute/topicmanagement/web/controller/EvaluatorAssignmentRestController.java`
- `src/main/resources/templates/faculty/evaluator-assignments.html`
- `src/main/resources/templates/fragments/sidebar.html`
- `src/test/java/com/hcmute/topicmanagement/EvaluatorAssignmentControllerTest.java`

## Verification

- Focused `mvn -q -Dtest=EvaluatorAssignmentControllerTest test` passes, 5 tests.
- Full `mvn -q test` passes, 149 tests, 0 failures, 0 errors.
- `mvn -q package -DskipTests` passes; executable JAR packaged.
- `git diff --check` passes; only normal LF/CRLF conversion warnings.
