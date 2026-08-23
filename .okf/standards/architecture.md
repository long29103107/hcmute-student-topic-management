# Architecture Standard

## Repository shape

```text
pom.xml
src/
|-- main/
|   |-- java/<base-package>/
|   |   |-- config/          # DataSource, application configuration
|   |   |-- model/           # Domain entities/value objects/enums
|   |   |-- dto/             # MVC/REST input/output contracts
|   |   |-- dao/             # JDBC interfaces and implementations
|   |   |-- service/         # Business rules and transaction orchestration
|   |   |-- controller/      # Spring MVC SSR controllers
|   |   |-- rest/            # Spring MVC REST controllers under /api
|   |   |-- filter/          # Encoding, authentication and authorization
|   |   |-- notification/    # Optional Java Mail port/adapter
|   |   |-- exception/
|   |   `-- util/
|   |-- resources/           # properties, SQL migration/seed resources
|   `-- webapp/
|       |-- assets/           # CSS, Tailwind output, jQuery and JS
|       |-- WEB-INF/views/    # JSP views; never directly public
|       `-- WEB-INF/web.xml  # only when required by the chosen setup
`-- test/java/<base-package>/
```

The exact base package may follow the course/repository convention; the Maven
configuration is authoritative once scaffolded.

## Layer boundaries

- `model` contains domain state and status transitions without Servlet/JSP or
  JDBC concerns.
- `dao` owns SQL, `Connection`, `PreparedStatement`, `ResultSet`, mapping and
  repository-specific exceptions.
- `service` owns use cases, business rules, authorization decisions and
  transaction boundaries. It must not render JSP or read HTTP parameters.
- `controller` binds form parameters, calls a service, chooses redirect/forward
  and exposes only the data needed by a JSP view.
- `rest` binds JSON DTOs and serializes responses/errors; it calls the same
  Service layer and never owns a second business implementation.
- `notification` is optional infrastructure for Java Mail; it is invoked
  through a Service port and is not required by core transactions.
- `filter` handles UTF-8, authentication/session checks and coarse route
  protection. Spring MVC interceptors may handle cross-cutting concerns;
  fine-grained resource checks remain in Service.
- `WEB-INF/views` contains JSP/JSTL presentation only. No SQL, password logic,
  or business decisions in JSP.

## MVC request flow

```text
Browser
  -> Filter (encoding/session/role)
  -> Spring MVC Controller (form bind + view)
  -> Service (use case + authorization + transaction)
  -> DAO (PreparedStatement/JDBC)
  -> MySQL
  -> Controller (redirect or forward)
  -> JSP/JSTL (escaped SSR)
```

REST adapter flow:

```text
Client/AJAX -> Spring MVC REST Controller (/api)
            -> DTO validation -> Service -> DAO/JDBC -> JSON response
```

## Persistence rules

- Use one configured `DataSource`/connection factory; never hard-code secrets.
- Use `PreparedStatement` for every input-bearing query.
- Keep multi-write operations atomic; pass a transaction context or use a
  service-owned connection/transaction helper consistently.
- Enforce important cardinality and uniqueness rules in both Service and the
  database where practical.
- Use Spring MVC/DI and explicit JDBC DAO code. Do not add JPA/Hibernate,
  Spring Data or another ORM unless `REQUEST.md` is explicitly changed.

## UI boundary

Use normal server-rendered JSP pages and form submissions. Tailwind CSS and
jQuery are UI helpers only; they must not turn the app into an SPA or relocate
business rules to the browser.
