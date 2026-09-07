# Phase 006.003 - Review Board UI & REST API

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/23

## Objective

Provide scoped SSR and REST board management for Admin and Faculty Head, with Lecturer visibility for assigned boards.

## Acceptance mapping

- /faculty/boards renders board list, member roles, schedule and lifecycle actions.
- Board create/edit uses approved-registration and lecturer selectors.
- /api/faculty/boards supports list, detail, create, update and status changes.
- Server-side authorization is applied independently from sidebar visibility.
- Seed page exposes topic registrations and review-board seed steps.

## Implementation

- Added ReviewBoardController, ReviewBoardRestController and faculty/boards.html.
- Added sidebar navigation under Faculty Workflow.
- Added /api/seed/topic-registrations and /api/seed/review-boards.
- Added UI/API error mapping and CSRF-protected SSR mutations.

## Verification

- ReviewBoardControllerTest renders SSR and REST responses and verifies department scope.
- Full `mvn -q test` passes across 26 test classes with 0 failures and 0 errors.
- `mvn -q package -DskipTests`, `node --check src/main/resources/static/js/app.js`
  and `git diff --check` pass.
- Browser smoke was not run because the desktop browser automation channel was
  unavailable in this session; controller coverage verifies the SSR/REST paths.
