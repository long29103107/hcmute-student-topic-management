---
phase: 001
title: Academic Foundation
status: completed
source: GitHub Project #5 Kanban
task_count: 3
done_count: 3
---

# Phase 001 Summary

Scanned from the Project #5 Kanban on 2026-09-08. The phase prefix is
001_xxx and all three cards are Done.

## Goal

Establish departments, registration periods, timeline rules, state transitions
and permission boundaries used by later academic workflows.

## Ticket index

### 001_001 — Department CRUD

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/1

- Faculty Head can create, edit, view and deactivate departments.
- Department code and name are required, correctly formatted and unique.
- A department referenced by topics cannot be hard-deleted.
- Backend permission and CSRF protect mutations.
- CRUD, duplicate, unauthorized, CSRF and deactivation cases are tested.

### 001_002 — Registration Period CRUD

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/2

- Faculty Head can create and update DATN and KLTN periods.
- Lecturer/student windows, current status and creator are persisted.
- Backend permission and CSRF protect mutations.
- Type, creator, status and unauthorized cases are tested.

### 001_003 — Timeline Rules & Permission Tests

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/3

- Reviewer deadline and council report date apply only to KLTN.
- Invalid timeline ordering, missing fields and invalid state transitions are
  rejected server-side.
- Shared period-open/domain rules are available to later modules.
- DATN/KLTN, timeline, status, permission, CSRF and boundary-time cases are
  covered.

## Identity directory refinement

- The dedicated `/admin/students` and `/admin/lecturers` directories accept
  optional `departmentId` and `status` (`ACTIVE` or `LOCKED`) filters in
  addition to search, sort and pagination.
- The shared account-directory UI renders department/status dropdowns and
  carries both filters through sort and pagination links; filtering is applied
  server-side before summary counts and page slicing.
- The directory filter form uses one responsive desktop row: the department
  and status filters use matching `sm:w-64` controls and sit directly beside
  the search input and Search button, with the whole control group aligned to
  the right edge. The search control is capped at `sm:w-80` (320px); mobile
  layouts stack the controls.
- The student/lecturer directory keeps the page title and action button but
  omits the redundant description, `Directory` eyebrow and `Accounts` panel
  heading so the filter row begins directly in the panel.
- User create/edit labels show required fields with red asterisks. MSSV,
  department and email guidance is exposed through hover/focus info icons on
  the corresponding labels, keeping the form compact.
- Set-password modals follow the same compact pattern: no subtitle, required
  red asterisks, and the 8–72 character requirement in a hover/focus tooltip
  on the New password label.
- Lock/Unlock actions open an explicit confirmation modal with the affected
  account name and a warning explaining the sign-in impact before submitting
  the status change. The modal always renders one visible static submit button
  and changes its label to Lock account or Unlock account from the server-side
  status.
- Delete confirmation keeps only the concise title and irreversible-action
  warning; the redundant account-specific sentence is omitted.
- Reusable `fragments/no-data` renders a generic `No data` empty state for
  directories and other modules.

## Verification

### Lecturer filter sizing — 2026-10-06

- Vision alignment: milestone 001 identity directories; compact role/status
  filters on `/admin/lecturers`, with no filtering or authorization changes.
- Lecturer role/status controls use a shared compact class at 10rem (160px)
  from 640px viewport width; mobile controls remain full width. Other directory
  status controls retain their existing width.
- Verified: `mvnw.cmd -q -DskipTests package` and `git diff --check` pass.
  The focused `UserManagementControllerTest` run did not complete because
  Mockito's dynamic Java-agent attachment failed with a Windows pipe access
  error. Browser verification reached the login page in the tool's session,
  so the authenticated lecturer layout was not visually verified.

- DepartmentControllerTest
- RegistrationPeriodControllerTest
- SecurityConfigTest
- DatabaseSchemaServiceTest
- UserManagementControllerTest (28 tests, including department/status
  filtering and shared directory rendering)
- All three Kanban cards are Done.
