# Task Note

## Vision alignment

- Product Vision milestone: academic workflow roadmap, explicitly selected by
  the user after Phase 001 identity and access completion.
- User-facing outcome: a Lecturer can manage their own topic proposals from a
  server-rendered page and the same rules are available through REST.
- Explicitly out of scope: period CRUD, supervisor assignment, review,
  approval, publication, groups, registrations, reports and evaluations.

## Step Goal

Implement Topic Proposal CRUD for the authenticated proposer, including
validation, ownership checks, lecturer registration-window enforcement and
focused SSR/REST tests.

## Dependency

- Department CRUD and seeded departments/users from the completed identity and
  administration work.
- Existing `TopicEntity`, `RegistrationPeriodEntity` and permission catalog.

## Scope

In:

- Own-proposal list and create/edit modals at `/lecturer/topics`.
- REST list/create/update endpoints under `/api/lecturer/topics`.
- Active department and `OPEN` lecturer registration-window validation.
- Ownership checks and editable-state rules for `DRAFT` and `REJECTED`.

Out:

- Topic deletion or submission to review.
- Supervisor assignment, topic review, approval or publication.
- Registration-period management and downstream student workflow.

## Relevant Standards

- `.okf/standards/architecture.md`
- `.okf/standards/coding-style.md`
- `.okf/standards/servlet-design.md`
- `.okf/standards/api-design.md`
- `.okf/standards/security.md`
- `.okf/standards/testing.md`
- `PRODUCT_VISION.md`
- `REQUEST.md`

## Affected Files

- `TopicProposalService`, `TopicRepository`
- `TopicProposalController`, `TopicProposalRestController`
- `TopicProposalForm`, `TopicProposalRequest`
- `lecturer/topics.html`, `fragments/topic-form.html`, lecturer sidebar
- `docs/ui-route-map.md`, `docs/domain-model.md`, `AGENT_MEMORY.md`

## Acceptance Criteria

- [x] Lecturer can create a topic for an active department and valid period.
- [x] Creation is limited to the inclusive lecturer registration window.
- [x] A Lecturer can edit only their own `DRAFT`/`REJECTED` proposal.
- [x] Title, description, department and period are validated server-side.
- [x] Users without `TOPIC_PROPOSE` are rejected by SSR and REST routes.
- [x] Tests cover success, invalid data, unauthorized access and period rules.

## Foundation for Next Step

The proposal service and DTOs provide an ownership-safe topic read/write
contract. Later supervisor-assignment and review tasks can consume the topic
status and proposer/department/period relationships without duplicating the
lecturer proposal rules.

## Verification

- Focused `mvn -Dtest=TopicProposalControllerTest test` — pass, 6 tests.
- Full `mvn test` — pass, 64 tests, 0 failures, 0 errors, 0 skipped.
- `git diff --check` — pass.

## Done Notes

Implementation and verification complete. All six GitHub acceptance criteria
are checked and the Project #5 item is now Done.
