# Task Note

## Vision alignment

- Product Vision capability: academic-workflow roadmap, explicitly selected
  by the user after the identity/access milestone and supervisor-assignment
  task.
- User-facing outcome: Admin and Faculty Head users with `TOPIC_REVIEW` can
  inspect pending topic proposals and approve or reject them through SSR and
  REST while the server enforces scope and status rules.
- Explicitly out of scope: topic publication, rejection-reason persistence,
  approval audit history, groups, registrations, reports and evaluation.

## Step goal

Implement the topic review queue and the revised-schema status transitions for
issue #6. The only reviewable source status is `PENDING_APPROVAL`; approve
sets `APPROVED` and reject sets `REJECTED`.

## Authorization and business rules

- `TOPIC_REVIEW` is required for both SSR and REST operations.
- The seed catalog contains `TOPIC_REVIEW`, grants it to Admin and Faculty
  Head, excludes it from Lecturer/Student, and restores pending topic fixtures.
- The Faculty sidebar exposes `Topic review` only to users holding
  `TOPIC_REVIEW`.
- Admin can review pending proposals across departments.
- Faculty Head can review pending proposals in the Faculty Head's assigned
  department; an unassigned Faculty Head has an empty queue.
- An active reviewer must be an Admin or Faculty Head, and the proposer cannot
  review their own proposal.
- Invalid decisions and transitions are rejected before persistence.
- The schema uses only `topics.status`; no approval actor/timestamp/reason or
  review-history columns are added.

## HTTP contract

- `GET /faculty/topics/review` renders the pending review queue.
- `POST /faculty/topics/review` accepts `topicId` and `decision=approve|reject`
  and redirects after a valid decision; CSRF is required.
- `GET /api/faculty/topics/review` and `GET /api/topics/review` return the
  reviewer's pending queue.
- `POST /api/topics/{id}/approve` accepts
  `{ "decision": "APPROVE" | "REJECT" }` and returns the updated summary;
  CSRF is required.
- REST errors use `TOPIC_REVIEW_INVALID`, `TOPIC_REVIEW_NOT_FOUND` and
  `TOPIC_REVIEW_FORBIDDEN`.

## Acceptance checklist

- [x] Faculty Head/Admin with `TOPIC_REVIEW` can view proposals needing review.
- [x] Seed restores `TOPIC_REVIEW`, its Admin/Faculty Head mapping and pending
      review fixtures; the sidebar exposes the authorized review route.
- [x] Only `PENDING_APPROVAL` proposals can be approved or rejected.
- [x] Approve changes the topic to `APPROVED`.
- [x] Reject changes the topic to `REJECTED`.
- [x] A proposer cannot approve or reject their own topic.
- [x] Invalid transitions are rejected on the server.
- [x] Status transition, permission, self-review, unauthorized and CSRF cases
      have focused tests.

## Verification evidence

- `mvn -Dtest=DatabaseSchemaServiceTest,DatabaseSeedControllerTest,TopicReviewControllerTest` — pass, 9 tests.
- `mvn test` — pass, 92 tests, 0 failures, 0 errors, 0 skipped.
- `mvn package -DskipTests` — pass; executable Spring Boot JAR created.
- `git diff --check` — pass; only normal Git LF/CRLF conversion warnings.
- The seed pipeline's first step now drops all 17 revised-schema tables,
  recreates the DDL and then restores permissions, Admin/Faculty Head access,
  pending review fixtures and the authorized sidebar route.
