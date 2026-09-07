# Phase 006.004 - Review Board Evaluation Integration

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/24

## Objective

Connect evaluation rows to board members and enforce score-entry gates.

## Acceptance mapping

- Board creation creates or links one evaluation per active board member.
- Only the assigned active board member may score.
- Topic supervisors cannot score their own topic.
- Scoring is blocked before board assignment and after publication/closure.
- Historical member/evaluation links remain available after reassignment.

## Implementation

- Extended EvaluatorAssignmentService to use active board-member assignments.
- Extended EvaluationScoringService visibility, ownership and board-status guards.
- Added board/member role and status data to scoring summaries.
- Seeded linked evaluation rows for CNTT and CNPM boards.

## Verification

- Focused board controller test verifies evaluation linkage for a newly created board.
- Existing evaluator/scoring regression suites remain part of the full Maven test run.
- Result publication remains a separate downstream workflow.
