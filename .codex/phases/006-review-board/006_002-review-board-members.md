# Phase 006.002 - Review Board Member Assignment & Validation

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/22

## Objective

Validate board composition, scope and member history.

## Acceptance mapping

- A board has 3-5 unique active Lecturer or Faculty Head members.
- Exactly one CHAIR and one SECRETARY are required.
- Students, inactive users, cross-department users and topic supervisors are rejected.
- Admin manages all departments; Faculty Head manages only the topic department.
- Removed assignments become inactive historical rows with endedAt.

## Implementation

- Added active/endedAt fields to ReviewBoardMemberEntity and DDL.
- Added candidate queries and service-side composition validation.
- Added REVIEW_BOARD_VIEW and REVIEW_BOARD_MANAGE seed permissions.
- Seeded CNTT and CNPM boards with realistic members.

## Verification

- Focused controller integration test covers invalid composition and cross-department denial.
- Seed reset reports 6 active board members across 2 boards.
- No external GitHub state beyond moving the issue to In Progress was changed.
