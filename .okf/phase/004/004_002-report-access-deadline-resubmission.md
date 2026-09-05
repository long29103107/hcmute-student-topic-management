# Task Note

## Vision alignment

- Product Vision roadmap bundle: relationship-scoped report access after safe
  report storage, explicitly selected by the user from Project #5.
- User-facing outcome: authorized group members and academic actors can view or
  download a report without exposing it to unrelated users.
- Explicitly out of scope: deadline/resubmission policy, evaluator assignment,
  scoring and result publication.

## Step Goal

Deliver server-enforced report metadata and download authorization for group
members, supervisors, assigned evaluators, Faculty Heads and Admin.

## Dependency

- `004_001` report upload and external storage boundary.
- Existing `topic_supervisors`, `review_boards`, `review_board_members` and
  `evaluations` relationship mappings.

## Scope

In:

- `REPORT_VIEW` permission catalog and seed role bundles.
- Group-member, supervisor, evaluator and Faculty Head resource checks.
- SSR download plus REST metadata/download contracts.
- Privacy and unauthorized-download tests.

Out:

- Report deadline and resubmission/version behavior. The issue defers these
  because the revised schema has no dedicated report deadline field and the
  business rule for replacing reports is not decided.
- Evaluator assignment, score entry, average calculation and result release.

## Acceptance Criteria

- Group members can access only reports attached to their own group.
- Supervisors can access only reports for topics they supervise.
- Evaluators can access only reports for registrations assigned to them.
- Faculty Heads can access only reports in their assigned department; Admin has
  operational access across reports.
- Unrelated users cannot access metadata or download bytes.
- The server authorizes both metadata and download through the same Service;
  the physical stored name is not returned in the access metadata DTO.

## Affected Files

- Report storage/service and access/download controllers.
- Permission seed, README and report authorization documentation.
- `ReportControllerTest` and Phase 004 verification notes.

## Verification

- Focused report upload/access tests cover group-member privacy, supervisor,
  evaluator, Faculty Head scope, missing permission, SSR download and REST
  metadata/download.
- Full `mvn -q test` passed with 135 tests, 0 failures and 0 errors.
- `mvn -q package -DskipTests` passed and `git diff --check` passed; only normal
  LF/CRLF conversion warnings were reported by Git.
