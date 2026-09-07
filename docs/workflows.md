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
  -> Faculty Head creates a review board and assigns Chair/Secretary/members
  -> Assigned board members enter score and comment
  -> Faculty Head completes the board
  -> Faculty Head publishes the evaluation result
  -> Group members view their result
```

The full board workflow is implemented as Phase 006. The legacy single-evaluator
path remains supported for existing registrations without a board, while board
registrations use active member-linked evaluations.

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
- Lecturer can submit an owned `DRAFT` or `REJECTED` proposal while its
  lecturer registration window is open; submission changes the topic to
  `PENDING_APPROVAL` and makes it read-only until Faculty review decides it.
- Admin/Faculty Head owns approval/rejection; Admin can publish across departments and Faculty Head publication is department-scoped.
- Student catalog/query checks `PUBLISHED`, an `OPEN` period and the inclusive student registration window; registration later rechecks the same gates.
- Rejection reason is optional for Must Have and useful for Should Have; keep
  the data field available without requiring a particular UI until confirmed.

## Topic supervisor assignment gates

- Admin may manage supervisors for every topic.
- Faculty Head may manage supervisors only for topics in the Faculty Head's
  assigned department; an unassigned Faculty Head has no manageable topics.
- An assignment must contain one or two active users with an active
  `LECTURER` or `FACULTY_HEAD` role. Duplicate IDs, inactive users, students
  and empty/over-limit assignments are rejected server-side.
- Every selected supervisor must be a member of the topic's department;
  cross-department assignments are rejected even when the selected user has a
  valid lecturer-capability role.
- `SUPERVISOR_MANAGE` is the permission gate. The UI only filters the list;
  `TopicSupervisorService` rechecks actor scope, candidate roles and the
  transaction before replacing `topic_supervisors`.

## Group registration gates

1. Authenticated student creates or joins a group.
2. Service checks max three members, one leader who is also a member, and no duplicate membership.
3. The current group leader can transfer leadership to another active member;
   the leader cannot leave before transferring leadership. An active
   `GROUP_MANAGE` actor can perform the transfer as an operational action.
4. Only leader can submit a registration.
5. Service checks student window, topic published status, matching period and
   no existing registration for the group.
6. A valid submission is stored as `PENDING` with the submitter and submission
   time; rejected/cancelled rows remain history while `PENDING` and `APPROVED`
   count as the current registration.
7. Faculty Head reviews `PENDING` registrations in the topic's department.
   Approve changes the row to `APPROVED`; reject requires and stores a reason
   up to 500 characters. `APPROVED` and `REJECTED` rows are terminal for this
   workflow and remain in history.
8. Report and evaluation consumers use the read-only
   `findApprovedByIdForReadOnly` repository contract; pending/rejected rows are
   not eligible for downstream work.

The service must perform the check and insert/update in one transaction to
avoid two concurrent membership or registration requests passing the same
uniqueness check. Group mutations lock the target group and registration
period rows before rechecking the rules.

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
  detailed audit history is deferred. `TOPIC_REVIEW` is required on the
  server; Admin reviews all pending proposals, Faculty Head reviews pending
  proposals in the assigned department, and the proposer cannot review their
  own proposal. The review API accepts `APPROVE` or `REJECT` only while the
  current status is `PENDING_APPROVAL`.
- Issue #9 scopes group membership by `student_groups.period_id`. An active
  group has at most three members and exactly one leader who is a member;
  leadership can transfer only to another active member, and a student may
  belong to at most one active group in the same period. Completed/inactive
  groups reject membership changes, while invite/confirm flows are outside
  this ticket.
- Issue #12 allows report upload only by the leader of an approved
  registration. The `reports` table stores metadata while file bytes remain in
  configured external storage; validation and storage policy are configurable,
  and storage must succeed before metadata is committed.
- Issue #13 protects report metadata and downloads with resource authorization
  for group members, supervisors, evaluators and Faculty Head according to the
  relationship policy. `REPORT_VIEW` is seeded for the roles that may reach a
  report resource, while the Service still checks the group/topic/assignment
  relationship. Admin has operational access to all report resources. A report
  deadline is not inferred from the student window because the schema has no
  dedicated field; resubmission rules remain deferred.
- Issue #14 lets an Admin or Faculty Head assign or replace one evaluator on an
  approved registration through the `evaluations` table. The actor is scoped
  to the registration's department unless they are Admin; the evaluator must
  be an active Lecturer or Faculty Head and cannot supervise the same topic.
  Reassignment updates the existing evaluation row, so repeated assignment
  does not create duplicate rows. `REGISTRATION_REVIEW` is the assignment
  permission for the current MVP faculty workflow.

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

Issue #15 implements the MVP score-entry boundary:

- An assigned evaluator submits or updates the existing `evaluations` row with
  a score and optional comment; another lecturer cannot submit against it.
- The default configurable scale is `0`–`10` with up to two decimal places;
  deployments can override `EVALUATION_SCORE_MIN` and `EVALUATION_SCORE_MAX`.
- The `reviewer_score_deadline` is enforced server-side. A published
  `registration_results` row also makes the evaluation read-only.
- The average is calculated from non-null `SUBMITTED`/`PUBLISHED` scores and
  rounded to two decimal places with `HALF_UP`; scores outside the configured
  range or with more than two decimal places are invalid. Score entry does not
  publish or write the final result.

Issue #16 completes the MVP result-release boundary:

- Admin or a Faculty Head publishes an approved registration only when at least
  one evaluation exists and every assigned evaluation has a non-null score with
  `SUBMITTED` or `PUBLISHED` status.
- Publication writes the aggregate `average_score`, `PUBLISHED` status,
  publisher and timestamp audit fields on `registration_results`.
- A published result is immutable through the normal workflow; a repeated
  publish request is rejected because no audited correction action exists.
- Students receive only published results for groups returned by the server's
  group-membership relationship query. Draft, incomplete and other-group
  results are not exposed.

## Extended board and score gates

- The extended board cannot be saved unless it has 3–5 members, exactly one
  chair and one secretary.
- Topic must be assigned to board before scores are accepted.
- A lecturer cannot submit a score if they are a supervisor of the topic.
- Score deadline, if applicable, is checked server-side.
- Each registration/evaluator pair has one evaluation row. The board materializes
  one row per active member and reuses/deactivates rows when membership changes.
- Chair or the configured academic role aggregates only valid submitted scores
  from current active members; inactive historical members remain auditable but
  do not block or change the current average.
- Faculty Head publishes only after every active board member has submitted a
  valid score and the board is `COMPLETED`. Publication writes publisher/time
  audit fields, changes the board to `PUBLISHED`, and locks further score edits.

Issue #25 implements the multi-evaluator boundary: duplicate evaluator rows are
blocked at the database boundary, score range/precision rules are shared by SSR
and REST, and average calculation is deterministic (`HALF_UP`, scale 2).

Issue #26 completes the result/QA boundary: incomplete or malformed board
evaluations cannot publish, student result reads remain group-scoped and
published-only, and the repeatable local seed contains one published board
result plus one active board for interactive scoring.

## Result visibility

Before publication, students do not see final result data. After publication,
only members of the group attached to that topic registration may view it.
Admin, Faculty Head and assigned academic roles may have operational access as
defined in the authorization matrix, but student-facing privacy rules remain
server enforced.


## Review board lifecycle (Issues #21–#24)

```text
DRAFT -> ASSIGNED -> SCHEDULED -> ACTIVE -> COMPLETED -> PUBLISHED -> CLOSED
```

A board belongs to exactly one approved registration. Faculty Head management is
department-scoped; Admin management is cross-department. A valid composition has
3–5 unique active Lecturer/Faculty Head accounts from the topic department, one
`CHAIR`, one `SECRETARY`, and no topic supervisor. Member removal is historical
(`active=false`, `ended_at` set), so evaluation/audit context is not lost.

Board creation materializes one evaluation row per active member and links it to
the board member. Scoring is accepted only for that active member while the board
is `ACTIVE` or `COMPLETED`; after publication or closure the evaluation is read-only.
