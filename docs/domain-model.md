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
  optional score deadline/council date, status and creator. The
  `RegistrationPeriodService` validates required/ordered windows, only allows
  reviewer and council milestones for graduation-thesis periods, enforces the
  `DRAFT -> OPEN -> CLOSED -> ARCHIVED` lifecycle, exposes an inclusive,
  read-only window contract for downstream modules, and records the creating user.
- `TopicEntity` belongs to one period and department and references its
  proposer. The topic-proposal Service only exposes the authenticated user's
  own proposals, requires an active department and an `OPEN` period whose
  lecturer window contains the current instant (inclusive), creates proposals
  as `DRAFT`, and permits edits only for owned `DRAFT` or `REJECTED` topics.
  Updating a rejected proposal returns it to `DRAFT`. `topic_supervisors`
  supports multiple lecturers. The `TopicSupervisorService` requires one or
  two active users with an active `LECTURER` or `FACULTY_HEAD` role, rejects
  duplicates, requires each supervisor to belong to the topic's department, and
  scopes management to all topics for Admin or the Faculty Head's own
  department. Topic review currently uses only the `topics.status`
  transition to `APPROVED` or `REJECTED`; the revised schema has no approval
  audit columns/history, so detailed review history is deferred. The review
  Service limits Faculty Heads to their department, gives Admin an all-topic
  operational scope, and rejects self-review and any transition from a status
  other than `PENDING_APPROVAL`.

## Student workflow

- `StudentGroupEntity` is scoped to one `RegistrationPeriodEntity` through the
  required `period_id`, and references its creator and leader.
- `group_members` maps student users to groups. The Service layer enforces at
  most three active members and exactly one leader who is always a member.
  Leadership can be transferred to another active student in the same group;
  a current leader cannot leave before that transfer. A student may belong to
  at most one active group within the same registration period; cross-period
  membership remains a separately configured policy. Invite/confirm workflow
  is not part of the revised MVP contract.
- `TopicRegistrationEntity` links a group, topic, period and submitter. It
  keeps `PENDING`, `APPROVED`, `REJECTED` and `CANCELLED` history, including a
  required rejection reason for a Faculty Head rejection. The revised schema
  deliberately does not use a unique `(group_id, period_id)` key; the Service
  layer prevents more than one active registration. Downstream report and
  evaluation flows should load only approved registrations through the
  repository's read-only approved-registration query.
- `ReportEntity` stores report metadata and uploader information. File bytes
  stay outside the database in configured non-public storage. Upload is allowed
  only for the leader of an approved registration; file policy is configurable
  and storage must succeed before report metadata is committed. Download and
  metadata access are separately protected by resource relationships.

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

## Announcements

- `AnnouncementScope` is either `SCHOOL` or `DEPARTMENT`. A department-scoped
  announcement must reference one department; a school-wide announcement has
  no department restriction.
- `AnnouncementStatus` is `DRAFT`, `PUBLISHED` or `HIDDEN`. New announcements
  start as drafts. Only published announcements are returned by the public
  announcement query; hidden and draft records remain management-only.
- The `ANNOUNCEMENT_MANAGE` permission is assigned to Admin and Faculty Head
  by the seed contract. Admin has all-department management scope; Faculty Head
  management is limited to the user's department. Students and Lecturers have
  no management permission but can read published announcements allowed by
  their school/department visibility.
- The persistent entity and service/repository operations are intentionally
  owned by the next Announcement backend task. This task fixes the shared
  status, scope and authorization vocabulary before that implementation.

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
User 1 ── * Announcement
Department 1 ── * Announcement (department scope only)
```

The relationship diagram does not replace Service authorization. In
particular, only group leaders submit registrations/reports, supervisors may
not evaluate their own topics, deadlines are server-side checks, and results
are visible to students only after publication and only for their own group.
