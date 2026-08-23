# Coding Style Standard

## Java

- Follow the Java version declared by `pom.xml`; do not silently upgrade it.
- Use Spring constructor injection; avoid field injection.
- Use descriptive classes such as `RegistrationPeriodService`,
  `TopicRegistrationDao`, `TopicController` and `TopicRestController`.
- Prefer immutable DTOs/records where the selected Java version supports them;
  use ordinary beans where JSP binding requires it.
- Keep methods small and make invalid states explicit with domain exceptions.
- Do not place SQL strings in Servlet classes.
- Use `try-with-resources` for JDBC resources and preserve the original cause
  when wrapping exceptions.
- Keep comments for non-obvious business constraints only.

## Spring MVC/JSP

- Use one focused `@Controller`/`@RestController` per coherent resource or
  action, with explicit mappings and HTTP method handling.
- Use POST/redirect/GET after successful mutations.
- Store only minimal safe identity data in session; do not store passwords.
- JSP pages use JSTL/EL and escaped output. Avoid scriptlets.
- Keep form field names and validation messages stable; document them in the
  relevant task note when they become a contract.
- REST endpoints use request/response DTOs and stable JSON error shapes; never
  expose JDBC/domain entities directly.

## Database and security

- Use named constants/enums for role and status values rather than scattering
  string literals.
- Hash passwords with a vetted password-hashing library (BCrypt is the default
  implementation choice unless the project selects another approved algorithm).
- Validate upload extension, MIME/type signature where practical, size and
  generated storage name; never use the original filename as a storage path.
- Escape/encode user-controlled values in JSP and JavaScript contexts.

## Documentation

- Update `README.md` for run/build/configuration changes.
- Update the applicable `docs/` contract when a route, status, field, role or
  unresolved business rule changes.
- Keep `.okf` task and phase notes synchronized after verification.
