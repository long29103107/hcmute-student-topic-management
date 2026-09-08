# Decision 004 - Course project MVP scope

## Status

Accepted for the current môn học đồ án unless the lecturer's rubric explicitly
requires a broader capability.

## Decision

The implementation targets a demonstrable MVC monolith, not a production-grade
academic management platform. The core workflow is:

```text
sign in → registration period → topic → student group
→ topic registration → report → evaluation/score → published result
```

The core conceptual entities are:

- `User`
- `Department`
- `RegistrationPeriod`
- `Topic`
- `StudentGroup`
- `TopicRegistration`
- `Report`
- `Evaluation`

`Evaluation` stores one lecturer's score/comment. The revised schema maps
`ReviewBoard`, `ReviewBoardMember` and `RegistrationResult` as persistence
extension points; their full UI/workflow remains scope-gated by the MVP
decision.

## Priority boundary

- **Must Have:** authentication/roles, periods, topics, groups, registration,
  report submission, simple lecturer assignment, scoring and result publication.
- **Should Have:** full 3–5 member review board, chair/secretary, multiple
  reviewer roles, filters, pagination, rejection reasons, score locking,
  responsive polish and advanced announcement management.
- **Nice to Have:** dashboards, activity logs, email, AJAX search and multiple
  report versions.

Detailed capabilities remain documented in `.okf/README.md` and `docs/`; this decision controls
implementation depth and prevents scope expansion during planning/review.
