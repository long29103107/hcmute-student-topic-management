# Business workflows and state gates

## Main sequence

```text
Faculty Head creates period
  -> Lecturer submits topic
  -> Faculty Head approves and publishes topics
  -> Student creates group
  -> Group leader submits one topic registration
  -> Faculty Head approves registration
  -> Group leader submits report
  -> Faculty Head creates board and assignments
  -> Assigned lecturers enter scores
  -> Chair aggregates final result
  -> Faculty publishes result
  -> Group members view their result
```

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

## Board and score gates

- Board cannot be saved unless it has 3–5 members, exactly one chair and one
  secretary.
- Topic must be assigned to board before scores are accepted.
- A lecturer cannot submit a score if they are a supervisor of the topic.
- Score deadline, if applicable, is checked server-side.
- Chair aggregates the component scores using the confirmed grading policy;
  until then, keep scale/rounding configurable.
- Faculty Head publishes only after required scoring/review completion is
  satisfied by the confirmed policy.

## Result visibility

Before publication, students do not see final result data. After publication,
only members of the group attached to that topic registration may view it.
Admin, Faculty Head and assigned academic roles may have operational access as
defined in the authorization matrix, but student-facing privacy rules remain
server enforced.
