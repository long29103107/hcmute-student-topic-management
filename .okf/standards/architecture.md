# Architecture Standard

## Course-project MVP boundary

Use the smallest architecture that demonstrates the core flow in the project
memory in `.okf/README.md`. The revised schema and current JPA model also include
ReviewBoard, ReviewBoardMember and RegistrationResult so the evaluation
extension can be mapped without denormalizing scores.

## Repository shape

```text
pom.xml
src/
|-- main/
|   |-- java/<base-package>/
|   |   |-- config/          # DataSource, application configuration
|   |   |-- model/           # Domain entities/value objects/enums
|   |   |-- dto/             # MVC/REST input/output contracts
|   |   |-- repository/      # Spring Data JPA repository contracts
|   |   |-- service/         # Business rules and transaction orchestration
|   |   |-- controller/      # Spring MVC SSR controllers
|   |   |-- rest/            # Spring MVC REST controllers under /api
|   |   |-- filter/          # Encoding, authentication and authorization
|   |   |-- notification/    # Optional Java Mail port/adapter
|   |   |-- exception/
|   |   `-- util/
|   |-- resources/           # properties, SQL migration/seed resources
|   `-- resources/
|       |-- templates/        # Thymeleaf views and fragments
|       `-- static/           # CSS, Tailwind output and JavaScript
`-- test/java/<base-package>/
```

The exact base package may follow the course/repository convention; the Maven
configuration is authoritative once scaffolded.

## Layer boundaries

- `model` contains domain state and status transitions without Servlet or
  JDBC concerns.
- `repository` owns Spring Data query contracts and entity persistence; custom
  JPQL/native queries must use bound parameters.
- `service` owns use cases, business rules, authorization decisions and
  transaction boundaries. It must not render JSP or read HTTP parameters.
- `controller` binds form parameters, calls a service, chooses redirect/forward
  and exposes only the data needed by a Thymeleaf view.
- REST controllers bind JSON DTOs and serialize responses/errors; they call the
  same Service layer and never own a second business implementation.
- `notification` is optional infrastructure for Java Mail; it is invoked
  through a Service port and is not required by core transactions.
- `filter` handles UTF-8, authentication/session checks and coarse route
  protection. Spring MVC interceptors may handle cross-cutting concerns;
  fine-grained resource checks remain in Service.
- `resources/templates` contains Thymeleaf presentation only. No query,
  password logic or business decisions belong in templates.

## MVC request flow

```text
Browser
  -> Filter (encoding/session/role)
  -> Spring MVC Controller (form bind + view)
  -> Service (use case + authorization + transaction)
  -> Spring Data repository (JPA/Hibernate)
  -> MySQL
  -> Controller (redirect or forward)
  -> Thymeleaf template (escaped SSR)
```

REST adapter flow:

```text
Client/AJAX -> Spring MVC REST Controller (/api)
            -> DTO validation -> Service -> repository/JPA -> JSON response
```

## Persistence rules

- Use one configured `DataSource`; never hard-code secrets.
- Use bound parameters for every input-bearing JPQL/native query.
- Keep multi-write operations atomic with Service-owned `@Transactional`
  boundaries.
- Enforce important cardinality and uniqueness rules in both Service and the
  database where practical.
- Use Spring MVC/DI with Spring Data JPA/Hibernate for the revised schema.

## UI boundary

Use normal server-rendered Thymeleaf pages and form submissions. Tailwind CSS
and JavaScript are UI helpers only; they must not turn the app into an SPA or
relocate business rules to the browser.
