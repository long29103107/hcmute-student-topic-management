# Domain model

The Java model is mapped with Jakarta Persistence to the revised 17-table MySQL
schema. Authentication and academic workflow data share `UserEntity`; the
revised schema does not have a separate Student Profile entity.

## Identity

- `UserEntity` stores the immutable internal `login_identifier`, display name,
  login email/code, optional department, non-null password hash and active
  status. For a Student, `login_identifier` is the unique 8-digit MSSV and
  `email_or_code` contains the generated student email.
- `RoleEntity` and `PermissionEntity` are system-managed catalogs.
  `UserRoleEntity` and `RolePermissionEntity` model their many-to-many
  assignments.

## Academic setup

- `DepartmentEntity` owns department code/name and has many topics and users.
- `RegistrationPeriodEntity` stores separate lecturer and student windows,
  optional score deadline/council date, status and creator.
- `TopicEntity` belongs to one period and department and references its
  proposer. The topic-proposal Service only exposes the authenticated user's
  own proposals, requires an active department and an `OPEN` period whose
  lecturer window contains the current instant (inclusive), creates proposals
  as `DRAFT`, and permits edits only for owned `DRAFT` or `REJECTED` topics.
  Updating a rejected proposal returns it to `DRAFT`. `topic_supervisors`
  supports multiple lecturers; Service rules limit a topic to one or two
  supervisors in the later assignment flow.

## Student workflow

- `StudentGroupEntity` is scoped to one `RegistrationPeriodEntity` through the
  required `period_id`, and references its creator and leader.
- `group_members` maps student users to groups. The Service layer enforces at
  most three active members and exactly one leader, plus the configured rule
  for a student joining groups across periods.
- `TopicRegistrationEntity` links a group, topic, period and submitter. It
  keeps `PENDING`, `APPROVED`, `REJECTED` and `CANCELLED` history. The revised
  schema deliberately does not use a unique `(group_id, period_id)` key; the
  Service layer prevents more than one active registration.
- `ReportEntity` stores report metadata and uploader information. File bytes
  stay outside the database in configured non-public storage.

## Evaluation and result

- `ReviewBoardEntity` is unique per topic registration and references its
  creator, schedule and status.
- `ReviewBoardMemberEntity` links lecturers to a board and stores `MEMBER`,
  `CHAIR` or `SECRETARY`. The database prevents duplicate lecturer membership;
  Service rules enforce 3–5 members and exactly one chair/secretary when the
  extended board flow is enabled.
- `EvaluationEntity` is one lecturer's score/comment. A registration may have
  multiple evaluations, with uniqueness per `(registration_id, lecturer_id)`;
  `board_id` is nullable for the MVP assignment path.
- `RegistrationResultEntity` uses a shared primary key (`registration_id`) and
  stores the aggregate score, final comment and finalization/publication audit
  fields. `average_score` belongs here, not on individual evaluations.

## Relationships

```text
RegistrationPeriod 1 ── * Topic
RegistrationPeriod 1 ── * StudentGroup
Department 1 ── * User
Department 1 ── * Topic
Topic 1 ── * TopicRegistration * ── 1 StudentGroup
Topic * ── * User (topic_supervisors)
StudentGroup * ── * User (group_members)
TopicRegistration 1 ── * Report
TopicRegistration 1 ── 0..1 ReviewBoard 1 ── * ReviewBoardMember * ── 1 User
TopicRegistration 1 ── * Evaluation * ── 1 User
TopicRegistration 1 ── 0..1 RegistrationResult
```

The relationship diagram does not replace Service authorization. In
particular, only group leaders submit registrations/reports, supervisors may
not evaluate their own topics, deadlines are server-side checks, and results
are visible to students only after publication and only for their own group.
