# Database design

`database/1.ddl.sql` is the repository copy of the revised DrawSQL export
`drawSQL-mysql-export-2026-09-03-revised.sql`. The application uses Spring Data
JPA/Hibernate to map this MySQL schema; Hibernate does not create or alter the
schema (`spring.jpa.hibernate.ddl-auto=none`). Existing databases should be
backed up and recreated from the DDL before starting the application.

## Schema groups

The revised schema contains 17 tables:

| Group | Tables | Purpose |
|---|---|---|
| Identity | `users`, `roles`, `permissions`, `user_roles`, `role_permissions` | Accounts and RBAC |
| Academic setup | `departments`, `registration_periods`, `topics`, `topic_supervisors` | Periods, topics and supervisors |
| Student workflow | `student_groups`, `group_members`, `topic_registrations`, `reports` | Group execution, registration and report metadata |
| Evaluation | `review_boards`, `review_board_members`, `evaluations`, `registration_results` | Board assignment, lecturer scores and final result publication |

## Important revised contracts

- `users` contains `login_identifier`, `full_name`, `email_or_code`,
  `password_hash` and `active`; `phone` and `date_of_birth` are no longer
  persisted. `password_hash` is `NOT NULL`; an empty value represents a new
  account whose password has not been configured and is rejected by login.
- `student_groups.period_id` is required, so group membership is scoped to a
  registration period.
- `topics.status` is the current review decision field. The revised schema has
  no approval actor/timestamp/reason columns or topic review-history table;
  those audit details are deferred.
- `topic_registrations` intentionally has no database unique constraint on
  `(group_id, period_id)`. Rejected/cancelled rows remain history; Service code
  must prevent more than one active registration for a group and period.
- `reports` stores file metadata only; file bytes use configured external
  storage. The schema has no dedicated report deadline field, so report access
  and resubmission policy must not infer one from the student registration
  window.
- `evaluations` are lecturer-level rows. The unique key is
  `(registration_id, lecturer_id)`, `board_id` is nullable, and aggregate data
  belongs in `registration_results`.
- `review_boards.registration_id` is unique: the current schema allows one
  board per topic registration. `review_board_members` prevents duplicate
  lecturer assignment per board.
- `registration_results` uses `registration_id` as both primary key and
  foreign key, with optional finalization/publication metadata.

## JPA mapping

Entities use `snake_case` column names explicitly where Java naming differs:

| Entity | Table | Key relationship |
|---|---|---|
| `UserEntity` | `users` | Immutable login identifier; for students it is the unique MSSV |
| `RegistrationPeriodEntity` | `registration_periods` | Owns period windows and references creator |
| `TopicEntity` | `topics` | References period, department and proposer; many-to-many supervisors |
| `StudentGroupEntity` | `student_groups` | References period, creator and leader; many-to-many members |
| `TopicRegistrationEntity` | `topic_registrations` | References group, topic, period and submitter; owns histories |
| `ReportEntity` | `reports` | Many reports per registration, newest submitted report first |
| `ReviewBoardEntity` / `ReviewBoardMemberEntity` | `review_boards` / `review_board_members` | One board per registration and many lecturer members |
| `EvaluationEntity` | `evaluations` | Many lecturer evaluations per registration |
| `RegistrationResultEntity` | `registration_results` | Shared primary key one-to-one result per registration |

`RoleEntity`, `PermissionEntity`, `UserRoleEntity` and
`RolePermissionEntity` map the identity catalog and assignment tables.

## Integrity and Service rules

Database foreign keys and unique keys protect relationships and duplicate
assignments. Service transactions must additionally enforce the business rules
that are not expressible in this schema:

- one to two supervisors per topic;
- at most three students and exactly one leader per active group;
- a student belongs to at most one active group in a period;
- at most one active topic registration per group and period;
- a full review board has 3–5 lecturers, exactly one `CHAIR` and one
  `SECRETARY`, when that workflow is enabled;
- a supervisor cannot evaluate their own topic;
- score deadlines, result aggregation and publication visibility are checked on
  the server.

## Reproducible setup

Run the DDL on a new MySQL schema:

```powershell
mysql -u root -p < database/1.ddl.sql
```

The local seed pipeline truncates the new tables in dependency order before
recreating identity fixtures. It does not migrate arbitrary legacy data.
