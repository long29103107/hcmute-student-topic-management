---
phase: 002
title: Topic Management
status: completed
source: GitHub Project #5 Kanban
task_count: 4
done_count: 4
---

# Phase 002 Summary

Scanned from the Project #5 Kanban on 2026-09-08. The phase prefix is
002_xxx and all four cards are Done.

## Goal

Manage lecturer topic proposals through supervisor assignment, faculty review
and publication.

## Ticket index

### 002_001 — Topic Proposal CRUD

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/4

- Lecturer creates topics for a valid department/period and open lecturer
  registration window.
- A lecturer edits only their own proposal while its status allows editing.
- Title, description, department and period are validated.
- Unauthorized and period-boundary requests are rejected.

### 002_002 — Topic Supervisor Assignment

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/5

- A topic has one or two valid Lecturer/Faculty Head supervisors.
- Duplicate supervisors and assignments beyond two are rejected.
- Only an authorized actor can assign or replace supervisors.
- Count, duplicate, invalid-user and unauthorized cases are covered.

### 002_003 — Topic Review Approve & Reject

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/6

- Faculty Head/Admin with TOPIC_REVIEW sees pending proposals.
- Only PENDING_APPROVAL proposals can be approved or rejected.
- Approval moves a topic to APPROVED; rejection moves it to REJECTED.
- Lecturer self-approval and invalid server-side transitions are rejected.

### 002_004 — Topic Publication & Published Query

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/7

- Only approved topics can be published.
- Published proposals cannot be edited through the proposal workflow.
- Published queries filter by topic status and valid period.
- Students never receive draft or rejected topics.
- Publish transition, visibility, period filtering and unauthorized cases are
  covered.

## Verification

- TopicProposalControllerTest
- TopicSupervisorControllerTest
- TopicReviewControllerTest
- TopicPublicationControllerTest
- All four Kanban cards are Done.
