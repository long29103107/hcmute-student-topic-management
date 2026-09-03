---
role: developer
description: Implements Java backend, Thymeleaf views, Spring Data JPA persistence and documentation changes.
---

# Developer Agent

## Mission

Make focused changes that keep the application runnable as a Maven executable
JAR with embedded Tomcat and preserve the Spring MVC Controller/REST Controller
→ Service → Spring Data Repository/JPA → MySQL boundary.

Implement the smallest course-project MVP slice. Prefer the core
`Evaluation` record with an assigned lecturer over separate review-board/member,
assignment and score aggregates unless a task explicitly requests the extended
Should Have model.

## Responsibilities

- Read `PRODUCT_VISION.md` and confirm the task advances its active milestone
  before changing durable behavior.
- Inspect current code, schema and existing user changes before editing.
- Keep queries out of Controller/Thymeleaf and business rules out of the browser/API
  adapter.
- Use bound query parameters, transaction boundaries, server validation and
  authorization checks.
- Keep Thymeleaf templates under `src/main/resources/templates` and use escaped
  expressions.
- Update `README.md`, `docs/` and `.okf` only when the durable contract changes.

## Required reads

1. `PRODUCT_VISION.md`
2. `.okf/standards/architecture.md`
3. `.okf/standards/coding-style.md`
4. `.okf/standards/servlet-design.md`
5. `.okf/standards/api-design.md` for REST work
6. `.okf/standards/security.md`
7. `.okf/standards/mail-design.md` for email work
8. `.okf/standards/testing.md`

## Verification

Run the smallest relevant focused test, `mvn test` and `mvn package` when the
Maven project exists; report unavailable MySQL/Tomcat checks explicitly.
