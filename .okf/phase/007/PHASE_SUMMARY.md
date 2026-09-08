---
phase: 007
title: Result Publication and Integration QA
status: completed
source: GitHub Project #5 Kanban
task_count: 2
done_count: 2
---

# Phase 007 Summary

Scanned from the Project #5 Kanban on 2026-09-08. The phase prefix is
007_xxx and both cards are Done.

## Goal

Make multi-evaluator scoring deterministic, gate publication on complete board
data, add repeatable fixtures and verify student privacy.

## Ticket index

### 007_001 — Multi-Evaluator Scoring & Average Calculation

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/25

- Each registration/evaluator pair has at most one evaluation and active member
  pair; JPA/DDL unique constraints enforce this.
- SSR/REST share score range, status and precision validation.
- Valid submitted evaluations produce a two-decimal HALF_UP average.
- Deadline, board status and published-result locks reject later score changes.

### 007_002 — Result Publication, Seed & End-to-End QA

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/26

- A board result requires every active member to submit a valid score and the
  board to be COMPLETED.
- Publication records publisher/time, sets the board to PUBLISHED and blocks
  further scoring.
- Students see only their own group's published result.
- Reset/reseed is repeatable and creates two boards, six members/evaluations,
  one published result at average 8.50 and one active board with drafts.
- The authenticated login-to-/student/results journey and manual workflow
  documentation are covered.

## Verification

- ResultPublicationControllerTest
- DatabaseSeedControllerTest
- ReviewBoardControllerTest
- Full Maven/package/static checks were recorded during implementation.
- Browser automation was unavailable; MockMvc covers the authenticated
  SSR/REST journey and negative authorization paths.
