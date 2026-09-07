# Phase 006.001 - Review Board Domain & Lifecycle

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/21

## Objective

Implement one review board per approved topic registration with a forward-only lifecycle and transactional persistence.

## Acceptance mapping

- ReviewBoardEntity remains one-to-one with TopicRegistrationEntity.
- Lifecycle supports DRAFT, ASSIGNED, SCHEDULED, ACTIVE, COMPLETED, PUBLISHED and CLOSED.
- Create/update is limited to approved registrations and rejects duplicate boards.
- Board changes are transactional and evaluation rows are synchronized for active members.

## Implementation

- Added lifecycle transition rules to ReviewBoardStatus.
- Added ReviewBoardService domain operations and board summary DTOs.
- Linked EvaluationEntity to the board and optional board member.
- Updated DDL/indexes for board-member links and active/history membership.

## Verification

- mvn -q -Dtest=ReviewBoardControllerTest test passes.
- Full `mvn -q test` passes across 26 test classes with 0 failures and 0 errors.
- `mvn -q package -DskipTests`, JavaScript syntax check and `git diff --check` pass.
- Scope is limited to the review-board workflow; commit, push and issue closure were not performed.

## Follow-up

Keep the legacy evaluator path for approved registrations that do not have a board.
