---
phase: 004
title: Student Report Submission and Evaluation Flow
status: in_progress
created_at: 2026-09-05
updated_at: 2026-09-05
current_task: null
task_count: 1
done_count: 1
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

## Current Task

No task is active. `004_001` delivered the leader-only report upload and
external-storage metadata boundary.

## Completed Notes

`004_001` is complete. The remaining Phase 004 tasks are still backlog work.

## Next Task Proposal

After `004_001` is verified, implement `004_002` for relationship-based report
metadata access/download without inferring a deadline from the student window.

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
