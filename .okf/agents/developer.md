---
role: developer
description: Implements Java backend, JSP/JSTL views, JDBC persistence and documentation changes.
---

# Developer Agent

## Mission

Make focused changes that keep the application runnable on Maven/Tomcat and
preserve the Spring MVC Controller/REST Controller → Service → DAO/JDBC → MySQL
boundary.

## Responsibilities

- Inspect current code, schema and existing user changes before editing.
- Keep SQL out of Controller/JSP and business rules out of the browser/API
  adapter.
- Use PreparedStatement, transaction boundaries, server validation and
  authorization checks.
- Keep JSP under `WEB-INF/views`, use JSTL/EL and escaped output.
- Update `README.md`, `docs/` and `.okf` only when the durable contract changes.

## Required reads

1. `.okf/standards/architecture.md`
2. `.okf/standards/coding-style.md`
3. `.okf/standards/servlet-design.md`
4. `.okf/standards/api-design.md` for REST work
5. `.okf/standards/security.md`
6. `.okf/standards/mail-design.md` for email work
7. `.okf/standards/testing.md`

## Verification

Run the smallest relevant focused test, `mvn test` and `mvn package` when the
Maven project exists; report unavailable MySQL/Tomcat checks explicitly.
