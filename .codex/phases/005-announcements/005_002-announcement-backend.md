# [005_002] Announcement Backend Implementation

- Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/18
- Status: Implemented locally; SSR/REST adapters remain for the next task.
- Phase: 005 — Announcements

## Objective

Persist announcements and enforce the draft/publication lifecycle and
school/department visibility rules defined in `docs/announcement-contract.md`.

## Acceptance criteria

- [x] MySQL DDL and JPA mappings for `announcements` are aligned.
- [x] Create/update/publish/hide operations validate lifecycle and scope on the server.
- [x] Published queries return only announcements visible to the current user.
- [x] State-changing service operations are transactional and permission-protected.
- [x] Focused lifecycle, authorization and visibility tests are present.

## Implementation

- Added `AnnouncementEntity` and `AnnouncementRepository`.
- Added transactional `AnnouncementService` with Admin-wide and Faculty Head
  department-scoped management.
- Added the `announcements` DDL table and included it in schema drop/reset order.
- Updated schema documentation and reset-count assertions from 17 to 18 tables.

## Verification

- Focused Announcement, schema and seed tests: passed.
- Full Maven test suite: 155 tests passed, 0 failures.
- `mvn -q -DskipTests package`: passed.
- `git diff --check`: passed; only normal LF/CRLF warnings were reported.

## Follow-up

- Implement controller/API adapters, CSRF request coverage and browser UI in the
  following announcement task.
- Do not close or mark the GitHub issue Done until those remaining task boundaries
  are completed and reviewed.
