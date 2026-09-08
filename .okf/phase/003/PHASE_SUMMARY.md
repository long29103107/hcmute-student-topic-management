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

## Verification

- StudentGroupControllerTest
- TopicRegistrationControllerTest
- TopicRegistrationReviewControllerTest
- Permissions, lifecycle, privacy, deadlines, leadership transfer,
  one-group-per-period and concurrency are covered.
