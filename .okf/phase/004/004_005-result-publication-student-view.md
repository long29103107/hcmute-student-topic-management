---
phase: 004
task: 004_005
title: Result Publication & Student View
status: completed
completed_at: 2026-09-05
---

# 004_005 — Result Publication & Student View

## Goal

Let Admin/Faculty Head publish a complete registration result and let a student
view only published results belonging to the student's own groups.

## Scope delivered

- Faculty publication queue at `/faculty/results` and `GET /api/faculty/results`.
- Publish action at `/faculty/results/{id}/publish` and
  `POST /api/faculty/results/{id}/publish`.
- Approved-registration, department-scope, evaluator-completeness and
  submitted-score checks before publication.
- Aggregate average persistence with `PUBLISHED` status, publisher and
  timestamp audit fields.
- Immutable normal workflow after publication; repeated publish is rejected.
- Student result view at `/student/results` and `GET /api/student/results`.
- Privacy enforced through server-side group membership and published-status
  filtering; student does not receive other groups' or unpublished results.
- Existing evaluator history remains usable even if the evaluator is later
  deactivated.

## Out of scope

- Audited correction/reversal workflow for an already published result.
- Full review board, final-comment editing workflow and notifications.

## Affected files

- `src/main/java/com/hcmute/topicmanagement/service/ResultPublicationService.java`
- `src/main/java/com/hcmute/topicmanagement/web/controller/ResultController.java`
- `src/main/java/com/hcmute/topicmanagement/web/controller/ResultRestController.java`
- `src/main/resources/templates/faculty/results.html`
- `src/main/resources/templates/student/results.html`
- `src/main/resources/templates/fragments/sidebar.html`
- `src/test/java/com/hcmute/topicmanagement/ResultPublicationControllerTest.java`

## Verification

- Focused `mvn -q -Dtest=ResultPublicationControllerTest test` passes, 5 tests.
- Full `mvn -q test` passes, 148 tests, 0 failures, 0 errors.
- `mvn -q package -DskipTests` passes; executable JAR packaged.
- `git diff --check` passes with only normal LF/CRLF conversion warnings.
