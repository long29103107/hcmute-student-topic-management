# Database design proposal

This is a code-generation starting point derived from section 9 of
`REQUEST.md`. Names may change during implementation, but relationships and
invariants must remain equivalent.

## MVP tables

Start with the smallest schema that can demonstrate the complete course-project
workflow:

| Table | Main columns/relationships |
|---|---|
| `users` | id, login identifier, full name, email/code, password_hash, role, active |
| `departments` | id, code/name, active |
| `registration_periods` | id, name, type, lecturer_start/end, student_start/end |
| `topics` | id, period_id, department_id, title, description, status, supervisor_id or a small supervisor relation |
| `student_groups` | id, name, created_by, leader_id, member ids, status |
| `topic_registrations` | id, group_id, topic_id, submitted_by, submitted_at, status |
| `reports` | id, registration_id, stored_name, original_name, content_type, size, uploader_id, submitted_at |
| `evaluations` | id, registration_id/topic_id, lecturer_id, score, comment, average_score, status, published_at |

`announcements` can be a small optional table or seeded static data for the
MVP. Do not create separate review-board, board-member, reviewer-assignment,
score-component and final-result tables until the extended Should Have model is
selected.

## Optional Should Have tables

Add these only when the rubric or a selected task requires a full review flow:

| Table | Purpose |
|---|---|
| `topic_supervisors` | Multiple GVHD per topic. |
| `group_members` | Normalized membership history and invitations. |
| `reviewer_assignments` | Multiple reviewer roles and assignment history. |
| `review_boards` / `review_board_members` | Board with 3–5 lecturers, chair and secretary. |
| `board_topic_assignments` | Assign topics to a board. |
| `scores` / `final_results` | Multiple component scores and separate aggregation/publication. |

## Required uniqueness and integrity candidates

- `users.login_identifier` and `users.email/code` as appropriate for the chosen
  login contract.
- A topic must have at least one assigned supervisor; a maximum of two is a
  Service rule when the small supervisor relation is used.
- A student’s active group membership must be checked transactionally; the
  exact historical/archival interpretation remains open.
- `topic_registrations` unique for one group per registration context.
- Exactly one group leader and no more than three members per group.
- Prevent duplicate evaluation submission for the same registration and lecturer.
- If optional board tables are selected, add their uniqueness and chair/secretary
  constraints in that task rather than pre-building them.

## Transaction boundaries

Use a transaction for:

- create group + first leader membership;
- join/leave/leader changes;
- topic + supervisor link;
- topic registration submission and any status update with related data;
- report metadata and file move coordination;
- evaluation submission and result publication.

Board/member/topic-assignment transactions belong to the optional extended
model.

The file move cannot be rolled back by MySQL. Use a temporary upload name and a
cleanup strategy documented in the implementation task.

## Deliberate omissions

Do not add tables for email delivery, audit logs, dashboards, API tokens,
analytics, tenants or external integrations in the first version.
