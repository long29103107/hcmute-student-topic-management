# Phase 007.001 - Multi-Evaluator Scoring and Average Calculation

Issue: https://github.com/long29103107/hcmute-student-topic-management/issues/25

## Objective

Make board scoring deterministic and safe for multiple evaluators while keeping
the existing legacy evaluator workflow compatible.

## Acceptance mapping

- One evaluation and one active board-member row exist per registration/evaluator
  pair; database and JPA unique constraints enforce this invariant.
- Score range and precision validation are shared by SSR/REST service paths.
- Averages use valid submitted/published current evaluations only and round to
  two decimals with `HALF_UP`; invalid scores cannot satisfy publication.
- Reviewer deadline, result publication and board status continue to lock score
  edits server-side.

## Implementation

- Added `EvaluationScoreCalculator` and reused it from scoring and publication.
- Added unique constraints to `evaluations` and `review_board_members` in JPA
  and `database/1.ddl.sql`.
- Updated board publication context to require exactly one evaluation for each
  active member while ignoring inactive historical members.

## Verification

- `mvn -q -Dtest=ResultPublicationControllerTest test` passes 10/10.
- `mvn -q -Dtest=DatabaseSeedControllerTest,ReviewBoardControllerTest test`
  passes 6/6.
- Full `mvn -q test` passes 167 tests with 0 failures and 0 errors.
- `mvn -q package -DskipTests`, `node --check src/main/resources/static/js/app.js`
  and `git diff --check` pass.

## Scope boundary

No database migration runner was introduced; production schema remains managed
explicitly through `database/1.ddl.sql` as documented by the repository.
