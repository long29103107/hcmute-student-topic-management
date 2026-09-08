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
- Publish/hide actions have confirmation UI and CSRF protection.
- Reader SSR/REST and dashboards use the same scoped service rules.

### 005_004 — Announcement Seed & Authorization Tests

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/20

- Seed is repeatable without duplicate announcements.
- Admin/Faculty Head and cross-department boundaries are tested.
- Published notices render on dashboards/list pages.
- Verification and route documentation are updated.

## Verification

- AnnouncementServiceTest
- AnnouncementControllerTest
- DatabaseSeedControllerTest
- DatabaseSchemaServiceTest
