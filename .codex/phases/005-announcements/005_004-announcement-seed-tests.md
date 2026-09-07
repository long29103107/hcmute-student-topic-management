# [005_004] Announcement seed and authorization tests

- Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/20
- Status: Implemented and locally verified; GitHub issue remains In Progress until user review.
- Phase: 005 — Announcements

## Vision alignment

The local fixture and regression slice makes the selected announcement
workflow repeatable for Admin, Faculty Head and reader-role testing without
introducing a second authorization path.

## Acceptance criteria

- [x] Local seed creates representative school-wide and CNTT announcements.
- [x] Re-running the fixture pipeline does not duplicate announcements.
- [x] Management and reader role/department boundaries have request coverage.
- [x] SSR and REST controller behavior, CSRF and hidden/draft visibility are tested.
- [x] Four role dashboards render real scoped announcements with an empty state.
- [x] Verification and route documentation are updated.

## Implementation

- Added idempotent announcement seed lookup/update and
  `POST /api/seed/announcements`.
- Extended reset payload with the announcement count and the seed page pipeline.
- Added `AnnouncementControllerTest` for SSR/REST scope, role authorization,
  lifecycle actions, CSRF and confirmation markup.
- Added seed assertions for the two fixtures and updated docs/runbook records.

## Verification

- `DatabaseSeedControllerTest`: 3 tests passed.
- `AnnouncementControllerTest`: 4 tests passed.
- `mvn -q -DskipTests compile`: passed.
- Full `mvn -q test`: passed across 26 test classes with 0 failures and 0 errors.
- `mvn -q package -DskipTests`, `node --check src/main/resources/static/js/app.js`
  and `git diff --check`: passed.
- Browser smoke was not run because the desktop browser automation channel was
  unavailable in this session; SSR/REST behavior is covered by the controller
  tests.
