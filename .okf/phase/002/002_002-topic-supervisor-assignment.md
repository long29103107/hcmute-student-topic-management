# Task Note

## Vision alignment

- Product Vision milestone: academic workflow roadmap, explicitly selected by
  the user after Phase 001 identity and access completion.
- User-facing outcome: Admin and Faculty Head can assign one or two valid
  lecturer-capability supervisors to a topic through a server-authorized UI
  and REST adapter.
- Explicitly out of scope: topic proposal CRUD, topic review/approval,
  publication, groups, registrations, reports and evaluation scoring.

## Step Goal

Implement topic supervisor assignment against the revised
`topic_supervisors(topic_id, lecturer_id)` join table. Admin has full scope;
Faculty Head can only manage topics belonging to the Faculty Head's own
department.

## Dependency

- `TopicEntity` and its existing `topic_supervisors` JPA mapping.
- `DepartmentEntity` and `UserEntity` department/role assignments.
- Completed Topic Proposal CRUD (`002_001`) and seeded departments/users.

## Scope

In: supervisor assignment service, permission/role seed, Faculty Head/Admin
topic assignment page, REST adapter, server-side validation and authorization
tests.

Out: supervisor invitation/confirmation, assignment history, topic review,
publication, board membership and evaluation assignment.

## Business rules

- A topic must have between one and two supervisors after an assignment.
- A supervisor must be active and have an active `LECTURER` or
  `FACULTY_HEAD` role; Faculty Head does not need a second `LECTURER` role.
- Duplicate supervisors are rejected before persistence.
- Admin can manage every topic.
- Faculty Head can only manage topics in the Faculty Head's assigned
  department. An unassigned Faculty Head has no manageable topics.
- The Service owns all checks and the transaction; UI filtering is not a
  security boundary.

## HTTP contract

- `GET /faculty/topics/supervisors` renders the manageable topic list and
  assignment modals.
- `POST /faculty/topics/{id}/supervisors` replaces the topic's supervisors
  using repeated `lecturerIds` form parameters and redirects on success.
- `GET /api/faculty/topics/supervisors` returns the manageable topics and valid
  supervisor options.
- `PUT /api/faculty/topics/{id}/supervisors` accepts
  `{ "lecturerIds": [1, 2] }` and returns the updated assignment.
- Mutating browser/API requests require CSRF according to the existing
  security configuration.

## Acceptance checklist

- [ ] `SUPERVISOR_MANAGE` is present in the seed permission catalog.
- [ ] Admin receives the permission through the full permission bundle.
- [ ] Faculty Head receives the permission; Lecturer and Student do not.
- [ ] The UI lists only topics inside the actor's allowed scope.
- [ ] Valid one- and two-supervisor assignments persist to
  `topic_supervisors`.
- [ ] Duplicate, zero, over-two, inactive and non-lecturer assignments are
  rejected server-side.
- [ ] Faculty Head cannot assign a topic from another department.
- [ ] Admin can assign topics across departments.
- [ ] REST and SSR use the same Service rules.
- [ ] Tests cover permission, department scope, valid assignments and all
  validation boundaries.

## Verification plan

- Focused `TopicSupervisorControllerTest` for SSR/REST and persistence.
- Seed assertions for permission count and Admin/Faculty Head mappings.
- Full `mvn test`, `mvn package -DskipTests` and `git diff --check`.
