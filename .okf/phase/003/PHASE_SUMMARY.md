---
phase: 003
title: Student Groups and Topic Registration
status: completed
source: GitHub Project #5 Kanban
task_count: 4
done_count: 4
---

# Phase 003 Summary

Scanned from the Project #5 Kanban on 2026-09-08. The phase prefix is
003_xxx and all four cards are Done.

## Goal

Let students form groups, enforce leader and membership rules, submit topic
registrations and let Faculty Head review them.

## Ticket index

### 003_001 — Student Group Creation & Membership

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/8

- Students create, join and leave valid groups.
- Groups are capped at three members and reject duplicate membership.
- Students see only groups related to them.
- Create, join, leave, capacity, duplicate and unauthorized cases are tested.

### 003_002 — Group Leader & Membership Rules

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/9

- An active group has exactly one leader who is also a member.
- Leadership transfers only to a valid member in the same period.
- The last leader cannot leave before transferring leadership.
- A student can belong to only one active group per period.
- Completed/inactive groups reject membership mutations.
- Transaction and concurrency tests protect the invariants.

### 003_003 — Topic Registration Submission

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/10

- Only the group leader submits.
- The group must be active and the topic published.
- Topic and selected period must match.
- Submission is limited to the student registration window.
- A group cannot have multiple current registrations in one period.
- Submitter and submission time are retained.

### 003_004 — Topic Registration Review

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/11

- Faculty Head reviews registrations in scope and approves valid ones.
- Rejection requires a reason.
- Approved/rejected registrations cannot be mutated through the review flow.
- Registration history is retained and approved registrations are available to
  report/evaluation read-only queries.

### User-selected extension — Faculty Student Group Directory

Vision alignment: the user explicitly selected the later student-group scope
while the identity milestone remains active. This slice is deliberately
read-only and does not expand student membership or registration mutations.

- `GROUP_READ` is a separate seeded permission from student `GROUP_MANAGE`;
  Admin and Faculty Head receive it, while Lecturer and Student do not.
- `GET /faculty/groups` is server-rendered. Admin sees every group; a Faculty
  Head sees groups having at least one member in the Faculty Head's department;
  an unassigned Faculty Head gets an empty scoped directory.
- Search matches group, registration period, leader and member names/logins.
  Sort is allowlisted as `group|period|leader|members|status|created`, and
  pagination clamps page size to the existing 5–100 server-side range while
  preserving search/sort/direction in navigation links.
- The sidebar entry is permission-guarded and the directory is read-only;
  group creation, membership changes and leadership transitions remain under
  the existing Student `GROUP_MANAGE` flow.

## Verification

- StudentGroupControllerTest
- FacultyStudentGroupControllerTest (faculty scope, Admin scope, permission
  denial, search, sort and pagination)
- TopicRegistrationControllerTest
- TopicRegistrationReviewControllerTest
- DatabaseSeedControllerTest (27 seeded permissions and Faculty Head
  `GROUP_READ` assignment)
- `mvn '-Dtest=FacultyStudentGroupControllerTest' test`
- `mvn '-Dtest=FacultyStudentGroupControllerTest,StudentGroupControllerTest,DatabaseSeedControllerTest' test`
- `npm run build:css`
- A full `mvn test` attempt reached 173 tests but had 12 existing H2 shared-
  database initialization errors in `RegistrationPeriodControllerTest` and
  `ReviewBoardControllerTest` (`USERS`, `REGISTRATION_PERIODS` or
  `ANNOUNCEMENTS` not found); the focused group/seed suite remains green.
- Permissions, lifecycle, privacy, deadlines, leadership transfer,
  one-group-per-period and concurrency are covered by the existing group tests.
