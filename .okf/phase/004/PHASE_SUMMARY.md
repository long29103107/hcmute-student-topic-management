---
phase: 004
title: Student Report Submission and Evaluation Flow
status: completed
created_at: 2026-09-05
updated_at: 2026-09-05
current_task: null
task_count: 5
done_count: 5
depends_on: [002, 003]
---

# Phase 004 Summary

## Vision alignment

This phase was explicitly selected by the user from the GitHub Project #5
backlog. It advances the later Product Vision roadmap bundle for report
metadata/safe storage and the evaluation flow after the topic registration
workflow. The missing local Phase 003 record is not recreated here; the
selected task keeps its GitHub prefix and is recorded under Phase 004.

## Phase Goal

Give an approved student topic registration a server-authorized report
submission path, then build the relationship-scoped report and evaluation
steps on top of the stored report metadata.

## Phase Done Criteria

- Group leaders can submit a report for an approved registration with metadata
  persisted only after external storage succeeds.
- Report access, evaluator assignment, scoring/aggregation and published-result
  visibility are implemented in their selected task boundaries.
- Each task has focused authorization, validation, persistence and UI tests,
  plus a recorded full regression result before the phase is closed.

## Scope

In:

- Report upload and metadata persistence (`004_001`).
- Relationship-based report access/download (`004_002`).
- Evaluator assignment (`004_003`).
- Score entry and average calculation (`004_004`).
- Result publication and student result view (`004_005`).

Out:

- Download authorization, deadlines and resubmission policy in `004_001`;
  those remain in the later report-access task unless explicitly selected.
- Full 3–5 member review boards, email, audit logs and report version history.

## Task Index

| Task | Title | Status | Done At |
|---|---|---|---|
| 004_001 | Report Upload & Metadata | completed | 2026-09-05 |
| 004_002 | Report Access, Deadline & Resubmission | completed | 2026-09-05 |
| 004_003 | Evaluator Assignment | completed | 2026-09-05 |
| 004_004 | Scoring & Average Calculation | completed | 2026-09-05 |
| 004_005 | Result Publication & Student View | completed | 2026-09-05 |

## Current Task

No task is active. `004_001` delivered the leader-only report upload and
external-storage metadata boundary; `004_002` delivered relationship-scoped
report metadata and download access; `004_003` delivered evaluator assignment;
`004_004` delivered evaluator score entry and average calculation; `004_005`
delivered final result publication and the privacy-scoped student result view.

## Completed Notes

`004_001`, `004_002`, `004_003`, `004_004` and `004_005` are complete. The
selected Phase 004 backlog is complete.

## Next Task Proposal

The selected Phase 004 backlog is complete. Any further work should be a new
user-selected task or a follow-up hardening issue.

## Task Notes

### 004_001 - Report Upload & Metadata

#### Step Goal

Allow the current leader of an approved topic registration to upload a report
through SSR or REST, validate the configured file policy, store bytes outside
the database and commit only the revised `reports` metadata after storage
succeeds.

#### Dependency

- `003_004` approved topic-registration workflow and read-only approved
  registration repository contract.
- Existing `REPORT_SUBMIT` permission in the Student seed bundle.

#### Scope

In:

- Leader/approved-registration/group/period relationship checks.
- Configurable content-type and byte-size policy.
- Local external storage adapter with generated storage keys and cleanup.
- SSR upload form on the student registration history and multipart REST API.
- Report metadata persistence and focused tests.

Out:

- Report download authorization, deadlines/resubmission, evaluator assignment
  and scoring.

#### Acceptance Criteria

- Only the current leader of an approved registration can upload.
- Registration, group and period identifiers must match on the server.
- File policy is configurable and invalid type/size is rejected before storage.
- Original name, generated stored name, content type, size, uploader and time
  are persisted in `reports` only after storage succeeds.
- Storage failure leaves no metadata row and cleans temporary/final bytes when
  possible; user filenames never become physical paths.
- SSR/REST/CSRF/relationship/storage failure tests pass.

#### Relevant Standards

- `.okf/standards/architecture.md`
- `.okf/standards/coding-style.md`
- `.okf/standards/servlet-design.md`
- `.okf/standards/api-design.md`
- `.okf/standards/security.md`
- `.okf/standards/testing.md`
- `PRODUCT_VISION.md`
- `REQUEST.md`

#### Affected Files

- `src/main/java/com/hcmute/topicmanagement/config/ReportUploadProperties.java`
- `src/main/java/com/hcmute/topicmanagement/service/ReportStorage.java`
- `src/main/java/com/hcmute/topicmanagement/service/LocalReportStorage.java`
- `src/main/java/com/hcmute/topicmanagement/service/ReportService.java`
- `src/main/java/com/hcmute/topicmanagement/web/controller/ReportController.java`
- `src/main/java/com/hcmute/topicmanagement/web/controller/ReportRestController.java`
- `src/main/resources/templates/student/registrations.html`
- `src/main/resources/application.properties`
- `docs/ui-route-map.md`, `docs/verification.md`

#### Verification

- `mvn -q -Dtest=ReportControllerTest,ReportServiceStorageFailureTest test`
  — passed after implementation.
- `mvn -q test` — passed, 133 tests, 0 failures, 0 errors.
- `mvn -q package -DskipTests` — passed; executable JAR packaged.
- `git diff --check` — passed; only normal LF/CRLF conversion warnings.

#### Foundation for Next Step

The `ReportEntity`, `ReportRepository`, approved-registration query and
generated storage-key boundary are reusable by `004_002` without exposing a
public filesystem path or changing the revised schema.

#### Done Notes

Completed on 2026-09-05 after focused tests, full regression and package
verification.

### 004_002 - Report Access, Deadline & Resubmission

#### Step Goal

Protect report metadata and file downloads with relationship-based resource
authorization for group members, supervisors, evaluators and Faculty Heads.

#### Dependency

- `004_001` report metadata and generated storage-key boundary.
- Existing topic supervisor and review/evaluation relationship mappings.

#### Scope

In:

- Seeded `REPORT_VIEW` permission and role-permission assignments.
- Group-member, topic-supervisor, assigned-evaluator, Faculty Head department
  and Admin resource checks in the Service.
- SSR download plus REST metadata and download endpoints.
- Cross-group, cross-department, missing-permission and download privacy tests.

Out:

- Report deadline and resubmission/version policy; the issue explicitly
  defers both because no dedicated deadline field or replacement rule exists.
- Evaluator assignment, scoring, result aggregation and publication.

#### Acceptance Criteria

- Only related group members, supervisors, assigned evaluators, scoped
  Faculty Heads and Admin can read report metadata or download bytes.
- The same Service authorization protects REST metadata, REST download and SSR
  download; hidden links are not used as the security boundary.
- Physical storage keys are not returned in the access metadata DTO.
- `REPORT_VIEW` is restored by the seed pipeline for the permitted role
  bundles.

#### Verification

- `mvn -q -Dtest=ReportControllerTest,ReportServiceStorageFailureTest test` —
  passed after implementation.
- `mvn -q test` — passed, 135 tests, 0 failures, 0 errors.
- `mvn -q package -DskipTests` — passed; executable JAR packaged.
- `git diff --check` — passed; only normal LF/CRLF conversion warnings.

#### Done Notes

Completed on 2026-09-05 after focused relationship, permission, metadata and
download tests, full regression, package and diff verification.

### 004_003 - Evaluator Assignment

#### Step Goal

Let an Admin or Faculty Head assign or change one evaluator for an approved
topic registration without bypassing department, role or supervisor-conflict
rules.

#### Dependency

- `004_002` approved-registration/report relationship boundary.
- Existing `REGISTRATION_REVIEW` permission and lecturer-capability role seed.
- Revised `evaluations` table with lecturer-level rows.

#### Scope

In:

- Approved-registration evaluator queue with search, sorting and pagination.
- SSR modal and REST API for assigning/changing one evaluator.
- Active Lecturer/Faculty Head candidate validation, department scope,
  supervisor conflict and duplicate-row protection.
- Faculty workflow sidebar entry and request-level tests.

Out:

- Score/comment entry, score deadlines, average calculation, result
  publication and full review-board workflow.

#### Acceptance Criteria

- Faculty Head assigns only within the assigned department; Admin can assign
  across departments.
- Candidate evaluator has an active Lecturer or Faculty Head role and is not a
  supervisor of the same topic.
- Only approved registrations are eligible.
- Reassignment updates the existing evaluation row and repeated assignment does
  not create duplicate rows.
- SSR and REST use the same Service authorization and validation rules.

#### Verification

- `mvn -q -Dtest=EvaluatorAssignmentControllerTest test` — passed, 4 tests.

### 004_004 - Scoring & Average Calculation

#### Step Goal

Allow assigned evaluators to enter/update score and comment for approved
registrations, calculate the valid evaluation average and enforce score and
publication gates.

#### Acceptance Criteria

- Only the assigned evaluator can submit the target evaluation.
- Scores are validated against the configurable default `0`–`10` scale.
- The reviewer deadline and published-result state make scoring read-only.
- Updating a score reuses the existing evaluation row and average calculation
  includes only non-null submitted/published scores.
- SSR and REST routes use the same Service rules and permission.

#### Verification

- `mvn -q -Dtest=EvaluationScoringControllerTest test` — passed, 4 tests.
- Full `mvn -q test` — passed, 143 tests, 0 failures, 0 errors.
- `mvn -q package -DskipTests` — passed; executable JAR packaged.
- `git diff --check` — passed with only normal LF/CRLF conversion warnings.

### 004_005 - Result Publication & Student View

#### Step Goal

Publish a complete approved registration result for the Faculty Head/Admin
scope and expose only published results for the authenticated student's groups.

#### Acceptance Criteria

- Faculty Head publishes only in their department; Admin can publish across
  departments.
- Publication requires at least one assigned evaluator and a submitted score
  for every assigned evaluation.
- Publication persists average score, `PUBLISHED` status, publisher and time.
- Published results cannot be changed through the normal workflow.
- Students see only published results for groups to which they belong.
- Evaluator history remains valid when the evaluator is deactivated.

#### Verification

- `mvn -q -Dtest=ResultPublicationControllerTest test` — passed, 5 tests.
- Full `mvn -q test` — passed, 148 tests, 0 failures, 0 errors.
- `mvn -q package -DskipTests` — passed; executable JAR packaged.
- `git diff --check` — passed; only normal LF/CRLF conversion warnings.

#### Done Notes

Completed on 2026-09-05 after focused publication, privacy, completeness,
scope, immutability, evaluator-history and SSR/API tests, full regression,
package and diff verification.
