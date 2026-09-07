# [005_003] Announcement UI and REST adapters

- Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/19
- Status: Implemented locally; GitHub issue remains In Progress until user review.
- Phase: 005 — Announcements

## Vision alignment

This is an explicitly selected Phase 005 extension of the academic workflow.
It keeps the existing Spring MVC/Thymeleaf monolith and exposes the same
announcement service through a reader page, a management page and REST
adapters.

## Acceptance criteria

- [x] Authorized Admin and Faculty Head managers can create and edit notices.
- [x] Publish and hide actions use a visible confirmation modal and CSRF.
- [x] Published reader pages and REST responses apply school/department scope.
- [x] Draft and hidden notices are excluded from end-user views.
- [x] SSR and REST operations delegate to the same transactional service.

## Implementation

- Added `AnnouncementController` for `/announcements` and
  `/announcements/manage` PRG actions.
- Added `AnnouncementRestController` for reader and management endpoints,
  stable validation/not-found/forbidden error payloads and CSRF-protected
  mutations.
- Added Thymeleaf reader/manager pages, shared dashboard announcement panel,
  sidebar navigation, scope-aware forms and publish/hide confirmation modal.
- Added real scoped announcement data to all four role dashboards.

## Verification

- `AnnouncementControllerTest`: 4 tests passed.
- `AnnouncementServiceTest`: existing lifecycle/scope coverage remains green.
- Seed regression suite passed before and after the adapter changes.
- `mvn -q -DskipTests compile`: passed.
- Browser/Tomcat smoke remains the user-facing follow-up checklist.
