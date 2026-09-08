---
phase: 004
title: Reports, Evaluation and Results
status: completed
source: GitHub Project #5 Kanban
task_count: 5
done_count: 5
---

# Phase 004 Summary

Scanned from the Project #5 Kanban on 2026-09-08. The phase prefix is
004_xxx and all five cards are Done.

## Goal

Handle report submission/access, evaluator assignment, scoring, average
calculation, result publication and student result visibility.

## Ticket index

### 004_001 — Report Upload & Metadata

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/12

- Only the leader of an approved registration can upload.
- Registration, group and period relationships are validated.
- File type and size use configured allowlists/limits.
- Stored metadata includes original/stored names, content type, size, uploader
  and submission time.
- Storage failures do not leave database metadata or unsafe physical filenames.

### 004_002 — Report Access, Deadline & Resubmission

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/13

- Group members, supervisor, assigned evaluator and scoped Faculty Head see
  only reports they are allowed to access.
- Metadata and download endpoints enforce authorization server-side.
- Cross-group privacy, REPORT_VIEW, relationship and unauthorized download
  cases are tested.

### 004_003 — Evaluator Assignment

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/14

- Faculty Head assigns valid Lecturer/Faculty Head evaluators to approved
  registrations.
- A topic supervisor cannot evaluate that topic.
- Duplicate assignment and unauthorized assignment are rejected.

### 004_004 — Scoring & Average Calculation

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/15

- Only an assigned evaluator can enter score/comment.
- Score range, deadline and publication locks are enforced server-side.
- Duplicate scoring is rejected and valid evaluations produce the average.

### 004_005 — Result Publication & Student View

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/16

- Faculty Head publishes complete results.
- Published results are immutable without an audited action.
- Students see only their own group's published result.
- Missing data, privacy and evaluator role/deactivation history are covered.

## Verification

- ReportControllerTest
- EvaluatorAssignmentControllerTest
- EvaluationScoringControllerTest
- ResultPublicationControllerTest
- ReportServiceStorageFailureTest
- RevisedSchemaPersistenceTest
