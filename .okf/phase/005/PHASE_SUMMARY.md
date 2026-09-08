---
phase: 005
title: Announcements
status: completed
source: GitHub Project #5 Kanban
task_count: 4
done_count: 4
---

# Phase 005 Summary

Scanned from the Project #5 Kanban on 2026-09-08. The phase prefix is
005_xxx and all four cards are Done.

## Goal

Add scoped announcement lifecycle, management UI/REST, repeatable fixtures and
dashboard visibility.

## Ticket index

### 005_001 — Announcement Contract & Permissions

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/17

- The announcement domain contract is documented and aligned with the project.
- Only authorized managers create, edit, hide and publish.
- Students and lecturers read only published announcements in scope.
- Draft and hidden announcements are never exposed.

### 005_002 — Announcement Backend Implementation

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/18

- MySQL DDL and JPA mappings are aligned.
- Draft/published/hidden transitions, scope and transactions are validated
  server-side.
- Published queries return only announcements visible to the current user.

### 005_003 — Announcement UI & REST API

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/19

- Admin/Faculty Head can manage announcements through SSR.
- The management UI exposes `SCHOOL` scope only to Admin; Faculty Head sees
  and selects only `DEPARTMENT` announcements in their own department.
- Create announcement opens a centered `max-w-4xl` popup; the management queue
  supports server-side search, sorting/direction and pagination while
  preserving scope.
- The management page follows the portal directory layout: the create action
  sits in the page header, the queue remains a single directory card, and
  filters stay hidden for an empty queue and become a responsive grid when
  announcements exist.
- The Faculty workflow sidebar opens automatically on the management page, so
  Faculty Heads can find the announcement entry after their seeded permission
  bundle is loaded.
- Publish/hide actions have confirmation UI and CSRF protection.
- Reader SSR/REST and dashboards use the same scoped service rules.

### 005_004 — Announcement Seed & Authorization Tests

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/20

- Seed is repeatable without duplicate announcements.
- Seed restores two `PUBLISHED` announcements and one `DRAFT` announcement;
  rerunning it resets lifecycle status and clears `publishedAt` for drafts.
- Admin/Faculty Head and cross-department boundaries are tested.
- Published notices render on dashboards/list pages.
- Verification and route documentation are updated.

## Verification

- AnnouncementServiceTest
- AnnouncementControllerTest
- DatabaseSeedControllerTest
- DatabaseSchemaServiceTest
- Focused regression run: `mvn '-Dtest=AnnouncementServiceTest,AnnouncementControllerTest' test`
- The focused run completed with 11 tests passing.
- Tailwind assets rebuilt successfully with `npm run build:css`.
- Seed, announcement-controller, and security regression run:
  `mvn '-Dtest=DatabaseSeedControllerTest,AnnouncementControllerTest,SecurityConfigTest' test`
  completed with 23 tests passing; the seed asserts `ANNOUNCEMENT_MANAGE` for
  `FACULTY_HEAD` and not for `LECTURER` or `STUDENT`.
- Seed fixture verification: `mvn '-Dtest=DatabaseSeedControllerTest' test`
  completed with 3 tests passing.
