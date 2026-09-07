# [005_004] Announcement seed and authorization tests

- Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/20
- Status: Implemented locally; GitHub issue remains In Progress until user review.
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
- Full `mvn test`, package, `git diff --check` and browser smoke are the final
  combined verification run before moving the tickets to Done.
