# Task Note

## Vision alignment

- Product Vision roadmap bundle: report metadata and safe storage, explicitly
  selected by the user from Project #5.
- User-facing outcome: a group leader can upload the report for an approved
  topic registration from the student registration history.
- Explicitly out of scope: report download authorization, deadlines,
  resubmission, evaluator assignment and scoring.

## Step Goal

Deliver leader-only report upload through SSR and multipart REST, with
configurable file validation, external bytes and transactional metadata.

## Dependency

- Approved registration workflow from `003_004` / issue #11.
- Existing `REPORT_SUBMIT` Student permission.

## Scope

In:

- Approved registration/group/period/leader checks.
- Configured content-type and max-size validation.
- Generated storage keys, temporary-file cleanup and metadata persistence.
- Student registration-history upload UI and REST response/error envelope.
- Focused SSR/REST/CSRF/storage-failure tests.

Out:

- Download/report-view authorization, report deadlines/resubmission,
  evaluator assignment, scores and results.

## Relevant Standards

- `.okf/standards/architecture.md`
- `.okf/standards/coding-style.md`
- `.okf/standards/servlet-design.md`
- `.okf/standards/api-design.md`
- `.okf/standards/security.md`
- `.okf/standards/testing.md`
- `PRODUCT_VISION.md`
- `REQUEST.md`

## Affected Files

- Report upload properties, storage port/adapter and service.
- Student SSR and REST controllers plus registration-history template.
- Application upload/storage settings and route/verification documents.
- `ReportControllerTest` and `ReportServiceStorageFailureTest`.

## Acceptance Criteria

- Only the current leader of an approved registration can upload.
- Registration, group and period relationship identifiers are checked.
- Configured type/size policy runs before storage.
- Report metadata is saved after storage succeeds; user filename is never a
  physical path/key.
- Storage failures do not persist a report row and clean temporary bytes.
- SSR, REST and CSRF behavior are covered by tests.

## Foundation for Next Step

The report row and storage key can be consumed by the next relationship-based
metadata/download task without coupling it to the local filesystem adapter.

## Verification

- `mvn -q -DskipTests compile` — passed.
- `mvn -q -Dtest=ReportControllerTest,ReportServiceStorageFailureTest test` —
  passed.
- `mvn -q test` — passed, 133 tests, 0 failures, 0 errors.
- `mvn -q package -DskipTests` — passed; executable JAR packaged.
- `git diff --check` — passed; only normal LF/CRLF conversion warnings.

## Done Notes

Completed on 2026-09-05.
