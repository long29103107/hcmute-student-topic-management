---
phase: 006
title: Review Board
status: completed
source: GitHub Project #5 Kanban
task_count: 4
done_count: 4
---

# Phase 006 Summary

Scanned from the Project #5 Kanban on 2026-09-08. The phase prefix is
006_xxx and all four cards are Done.

## Goal

Create scoped review boards for approved registrations, assign members, link
evaluations and expose board management through SSR/REST.

## Ticket index

### 006_001 — Review Board Domain & Lifecycle

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/21

- Boards are created only for approved registrations and cannot duplicate one
  registration.
- Lifecycle transitions are forward-only and validated server-side.
- Board/member/evaluation persistence is transactional.

### 006_002 — Review Board Member Assignment & Validation

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/22

- A board has 3–5 unique active Lecturer/Faculty Head members.
- Exactly one CHAIR and one SECRETARY are required.
- Students, inactive users, cross-department users and topic supervisors are
  rejected.
- Admin manages all departments; Faculty Head is limited to the topic
  department.
- Removed assignments remain as inactive historical rows.

### 006_003 — Review Board UI & REST API

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/23

- /faculty/boards and /api/faculty/boards expose scoped board management.
- SSR and REST share server-side validation and authorization.
- Create/edit uses a full-width popup; Board lecturers uses the same checkbox
  selector pattern as Assign supervisors.
- Navigation and route/seed documentation are updated.

### 006_004 — Review Board Evaluation Integration

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/24

- Board creation creates or links one evaluation per active board member.
- Only the assigned active member may score.
- A topic supervisor cannot score their own topic.
- Reassignment preserves historical member/evaluation links.

## Verification

- ReviewBoardControllerTest
- EvaluatorAssignmentControllerTest
- EvaluationScoringControllerTest
- DatabaseSeedControllerTest
