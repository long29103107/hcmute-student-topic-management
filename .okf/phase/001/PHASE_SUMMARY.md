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

## Verification

- DepartmentControllerTest
- RegistrationPeriodControllerTest
- SecurityConfigTest
- DatabaseSchemaServiceTest
- All three Kanban cards are Done.
