# ADR 001: Scope and Stack Boundary

Status: Accepted

## Context

`REQUEST.md` explicitly selects Java + Spring MVC/Jakarta Servlet, JSP/JSTL SSR,
RESTful API trong cùng monolith, JDBC + MySQL, Maven + Tomcat, Tailwind CSS,
jQuery và Java Mail tùy chọn.

Spring Framework Core/Spring MVC is configured directly with Java config and
`DispatcherServlet`; Spring Boot is intentionally not part of the baseline.

## Decision

Use a single Maven WAR application with the dependency direction:

```text
Spring MVC Controller/REST Controller -> Service -> DAO/JDBC -> MySQL
                    \-> JSP/JSTL views and static Tailwind/jQuery assets
```

SPA framework, microservices, Docker, Kubernetes, CI/CD and advanced
infrastructure are outside the first version. Java Mail remains optional and
must call the same Service layer.

## Consequences

- Spring MVC controllers and REST controllers are adapters only; server-side
  validation and authorization in Service are authoritative.
- JSP views are placed under `WEB-INF/views`.
- Every future phase must state why a new dependency is within `REQUEST.md`.
- Client-side validation/AJAX may improve UX later, but may not become a second
  business implementation.
