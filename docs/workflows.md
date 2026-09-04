# Business workflows and state gates

## Main sequence

```text
Faculty Head/Admin creates and opens a period
  -> Lecturer submits topic
  -> Faculty Head approves and publishes topics
  -> Student creates group
  -> Group leader submits one topic registration
  -> Faculty Head approves registration
  -> Group leader submits report
  -> Faculty Head assigns a lecturer for the MVP evaluation
  -> Assigned lecturer enters score and comment
  -> Faculty Head publishes the evaluation result
  -> Group members view their result
```

The full board workflow (3–5 lecturers, chair, secretary and multiple
assignments) is a Should Have extension. It must not block the MVP workflow.

## Registration period status contract

Registration periods use a forward-only lifecycle. Keeping the same status is
allowed; backward transitions and skipped states are rejected server-side:

```text
DRAFT -> OPEN -> CLOSED -> ARCHIVED
```

The read-only contract exposed by `RegistrationPeriodService` is used by topic
and topic-registration modules:

- `inspect(id, now)` returns status, both windows and inclusive booleans
  `lecturerRegistrationOpen` / `studentRegistrationOpen`.
- `requireOpenForLecturer(id, now)` returns the period or throws
  `REGISTRATION_PERIOD_NOT_FOUND`, `REGISTRATION_PERIOD_NOT_OPEN` or
  `LECTURER_REGISTRATION_WINDOW_CLOSED`.
- `requireOpenForStudent(id, now)` returns the period or throws
  `REGISTRATION_PERIOD_NOT_FOUND`, `REGISTRATION_PERIOD_NOT_OPEN` or
  `STUDENT_REGISTRATION_WINDOW_CLOSED`.

Reviewer score deadline and council report date are only valid for KLTN
(`GRADUATION_THESIS`) periods.

## Time gates

For every create/update action involving a period, Service compares the current
server time against the period window. Browser controls are only UX.

| Action | Required window |
|---|---|
| Lecturer proposes topic | lecturer registration start ≤ now ≤ end |
| Student leader registers topic | student registration start ≤ now ≤ end |
| Reviewer submits score | before reviewer deadline when that deadline applies |
| Council report date | informational/scheduling field for KLTN until lecturer confirms behavior |

The inclusive boundary (`≤`) is the implementation default for “trong thời
gian”; if the lecturer specifies another interpretation, update the contract.

## Topic state gates

```text
DRAFT -> PENDING_APPROVAL -> APPROVED -> PUBLISHED
                         \-> REJECTED
```

- Lecturer owns creation while the lecturer window is open.
- Faculty Head owns approval/rejection and publication.
- Student registration checks `PUBLISHED` plus matching period.
- Rejection reason is optional for Must Have and useful for Should Have; keep
  the data field available without requiring a particular UI until confirmed.

## Group registration gates

1. Authenticated student creates or joins a group.
2. Service checks max three members, one leader and no duplicate membership.
3. Only leader can submit a registration.
4. Service checks student window, topic published status, matching period and
   no existing registration for the group.
5. Faculty Head approves/rejects; reject reason may be stored.

The service must perform the check and insert/update in one transaction to
avoid two concurrent submissions passing the same uniqueness check.

## Report gates

- Registration must be approved.
- Request user must be the current group leader.
- File is validated before metadata/bytes are committed.
- Re-submission before deadline is a Should Have and must not be implemented as
  a mandatory behavior until selected.

## Backlog contract alignment (Issues #6, #9, #12, #13)

The backlog tickets are aligned with the revised schema and the current MVP
boundary as follows:

- Issue #6 uses `topics.status` as the review decision contract:
  `DRAFT -> PENDING_APPROVAL -> APPROVED` or `REJECTED`. The revised `topics`
  table has no approval actor/timestamp/reason columns or review-history table;
  detailed audit history is deferred.
- Issue #9 scopes group membership by `student_groups.period_id`. An active
  group has at most three members and exactly one leader, and a student may
  belong to at most one active group in the same period. Invite/confirm flows
  are outside this ticket.
- Issue #12 allows report upload only by the leader of an approved
  registration. The `reports` table stores metadata while file bytes remain in
  configured external storage; validation and storage policy are configurable,
  and storage must succeed before metadata is committed.
- Issue #13 protects report metadata and downloads with resource authorization
  for group members, supervisors, evaluators and Faculty Head according to the
  relationship policy. A report deadline is not inferred from the student
  window because the schema has no dedicated field; resubmission rules remain
  deferred. `REPORT_VIEW` is planned for implementation and must be added to
  the permission catalog/seed then.

## Evaluation gates

For the MVP:

- An approved registration must have at least one assigned lecturer and an
  evaluation row per assigned lecturer before the result can be finalized.
- The assigned lecturer cannot be a supervisor of the topic.
- The lecturer submits a score/comment before the configured deadline when the
  deadline applies.
- Faculty Head finalizes and publishes a separate `RegistrationResult` only
  after the required scores are present.
- The final score is the average of the configured evaluation score values;
  keep the grading scale and rounding configurable.

## Extended board and score gates

- The extended board cannot be saved unless it has 3–5 members, exactly one
  chair and one secretary.
- Topic must be assigned to board before scores are accepted.
- A lecturer cannot submit a score if they are a supervisor of the topic.
- Score deadline, if applicable, is checked server-side.
- Chair or the configured academic role aggregates evaluation scores using the
  confirmed grading policy; until then, keep scale/rounding configurable.
- Faculty Head publishes only after required scoring/review completion is
  satisfied by the confirmed policy.

## Result visibility

Before publication, students do not see final result data. After publication,
only members of the group attached to that topic registration may view it.
Admin, Faculty Head and assigned academic roles may have operational access as
defined in the authorization matrix, but student-facing privacy rules remain
server enforced.
