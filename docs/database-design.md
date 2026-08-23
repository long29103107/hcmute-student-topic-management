# Database design proposal

This is a code-generation starting point derived from section 9 of
`REQUEST.md`. Names may change during implementation, but relationships and
invariants must remain equivalent.

## Tables

| Table | Main columns/relationships |
|---|---|
| `users` | id, login identifier, full name, email/code, password_hash, role, active |
| `departments` | id, code/name, active |
| `registration_periods` | id, name, type, lecturer_start/end, student_start/end, reviewer_deadline nullable, council_date nullable |
| `topics` | id, period_id, department_id, title, description, status, rejection_reason nullable |
| `topic_supervisors` | topic_id, lecturer_id, supervisor_order/primary marker |
| `student_groups` | id, name, created_by, status |
| `group_members` | group_id, student_id, is_leader, joined_at |
| `topic_registrations` | id, group_id, topic_id, submitted_by, submitted_at, status, rejection_reason nullable |
| `reports` | id, registration_id, stored_name, original_name, content_type, size, uploader_id, submitted_at |
| `reviewer_assignments` | topic_id, lecturer_id, assignment_type, assigned_by |
| `review_boards` | id, period_id, name, status |
| `review_board_members` | board_id, lecturer_id, board_role |
| `board_topic_assignments` | board_id, topic_id |
| `scores` | topic_id, lecturer_id, assignment_id/board_id, component/value/comment, submitted_at |
| `final_results` | topic_id/registration_id, average_score, conclusion, aggregated_by, published_at, status |
| `announcements` | id, title, content, scope, status, published_at, created_by |

## Required uniqueness and integrity candidates

- `users.login_identifier` and `users.email/code` as appropriate for the chosen
  login contract.
- `topic_supervisors(topic_id, lecturer_id)` unique.
- At most two supervisors checked in Service and guarded by transaction.
- `group_members(group_id, student_id)` unique.
- A student’s active group membership must be checked transactionally; the
  exact historical/archival interpretation remains open.
- `topic_registrations` unique for one group per registration context.
- One `is_leader = true` per group, plus Service validation for max three.
- `review_board_members(board_id, lecturer_id)` unique.
- One chair and one secretary per board, enforced by Service and schema strategy.
- `board_topic_assignments(board_id, topic_id)` unique.
- Prevent duplicate score submissions according to the confirmed component
  model.

## Transaction boundaries

Use a transaction for:

- create group + first leader membership;
- join/leave/leader changes;
- topic + supervisor links;
- topic registration submission and any status update with related data;
- report metadata and file move coordination;
- board + members + topic assignments;
- score submission and final-result aggregation/publication.

The file move cannot be rolled back by MySQL. Use a temporary upload name and a
cleanup strategy documented in the implementation task.

## Deliberate omissions

Do not add tables for email delivery, audit logs, dashboards, API tokens,
analytics, tenants or external integrations in the first version.
