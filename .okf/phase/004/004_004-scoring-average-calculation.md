---
phase: 004
task: 004_004
title: Scoring & Average Calculation
status: completed
completed_at: 2026-09-05
---

# 004_004 — Scoring & Average Calculation

## Goal

Allow an assigned evaluator to enter or update a score/comment for an approved
topic registration and expose the calculated average without allowing changes
after the reviewer deadline or result publication.

## Scope delivered

- Evaluator scoring queue at `/lecturer/scoring` with score/comment modals.
- REST queue and update routes at `/api/lecturer/scoring` plus the existing
  `/api/faculty/scores` compatibility namespace.
- `EVALUATION_SUBMIT` authorization plus row-level assigned-evaluator checks.
- Configurable inclusive score range, default `0`–`10`, with two-decimal input.
- Server-side reviewer deadline and published-result locking.
- Average calculation from non-null `SUBMITTED`/`PUBLISHED` evaluations,
  rounded to two decimal places with `HALF_UP`.
- Existing evaluation-row update semantics; repeated scoring does not create
  duplicate rows.

## Out of scope

- Final result aggregation/persistence, publication and student result view;
  those remain in `004_005`.

## Affected files

- `src/main/java/com/hcmute/topicmanagement/config/EvaluationScoringProperties.java`
- `src/main/java/com/hcmute/topicmanagement/service/EvaluationScoringService.java`
- `src/main/java/com/hcmute/topicmanagement/repository/EvaluationRepository.java`
- `src/main/java/com/hcmute/topicmanagement/web/controller/EvaluationScoringController.java`
- `src/main/java/com/hcmute/topicmanagement/web/controller/EvaluationScoringRestController.java`
- `src/main/java/com/hcmute/topicmanagement/web/dto/EvaluationScoreRequest.java`
- `src/main/resources/templates/lecturer/scoring.html`
- `src/main/resources/templates/fragments/sidebar.html`
- `src/main/resources/application.properties`
- `src/test/java/com/hcmute/topicmanagement/EvaluationScoringControllerTest.java`

## Verification

- Focused `mvn -q -Dtest=EvaluationScoringControllerTest test` passes, 4 tests.
- Full `mvn -q test` passes, 143 tests, 0 failures, 0 errors.
- `mvn -q package -DskipTests` passes; executable JAR packaged.
- `git diff --check` passes; only normal LF/CRLF conversion warnings.
