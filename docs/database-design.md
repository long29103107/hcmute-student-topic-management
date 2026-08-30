# Database design proposal

This is a code-generation starting point derived from section 9 of
`REQUEST.md`. Names may change during implementation, but relationships and
invariants must remain equivalent.

## MVP tables

Start with the smallest schema that can demonstrate the complete course-project
workflow:

| Table | Main columns/relationships |
|---|---|
| `users` | id, internal login identifier, full name, login email, nullable password_hash, active |
| `departments` | id, code/name, active |
| `registration_periods` | id, name, type, lecturer_start/end, student_start/end |
| `topics` | id, period_id, department_id, title, description, status, supervisor_id or a small supervisor relation |
| `student_groups` | id, name, created_by, leader_id, member ids, status |
| `topic_registrations` | id, group_id, topic_id, submitted_by, submitted_at, status |
| `reports` | id, registration_id, stored_name, original_name, content_type, size, uploader_id, submitted_at |
| `evaluations` | id, registration_id/topic_id, lecturer_id, score, comment, average_score, status, published_at |

## Authentication and authorization tables

These tables support the login and role-management UI without expanding the
course-project business workflow:

| Table | Main columns/relationships |
|---|---|
| `roles` | id, code, name, description, system_role, active |
| `permissions` | id, code, name, permission_group, description, active |
| `user_roles` | user_id, role_id, assigned_at, active; unique per pair |
| `role_permissions` | role_id, permission_id, assigned_at, active; unique per pair |

`user_roles` references `users` and `roles`. `role_permissions` references
`roles` and `permissions`. The four default role codes are `ADMIN`,
`FACULTY_HEAD`, `LECTURER` and `STUDENT`; `GROUP_LEADER` remains a
group-membership attribute.

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

- `users.login_identifier` is an immutable internal identifier and
  `users.email_or_code` is the email used for password login.
- Student `login_identifier` equals the unique MSSV; Student creation generates
  `email_or_code` as `<MSSV>@student.hcmute.edu.vn` and may leave
  `password_hash` null until an administrator performs a password set/reset
  action. Authentication must reject accounts without a configured password.
- Lecturer creation also leaves `password_hash` null; its manually entered email
  is used for login and an administrator must set/reset the password before
  login.
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
