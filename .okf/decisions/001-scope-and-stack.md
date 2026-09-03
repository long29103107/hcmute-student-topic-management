# ADR 001: Scope and Stack Boundary

Status: Accepted

## Context

The implemented project uses Java 21 + Spring Boot 4.1.1/Spring MVC on Jakarta
Servlet, Thymeleaf SSR, RESTful API in the same monolith, Spring Data
JPA/Hibernate + MySQL, Maven + embedded Tomcat, Tailwind CSS/Flowbite and
optional Java Mail.

Spring Framework Core/Spring MVC is configured directly with Java config and
`DispatcherServlet`; Spring Boot owns application bootstrap and embeds Tomcat.

## Decision

Use a single Maven executable Spring Boot application with the dependency direction:

```text
Spring MVC Controller/REST Controller -> Service -> Spring Data Repository/JPA -> MySQL
                    \-> Thymeleaf views and static Tailwind/Flowbite assets
```

SPA framework, microservices, Docker, Kubernetes, CI/CD and advanced
infrastructure are outside the first version. Java Mail remains optional and
must call the same Service layer.

## Consequences

- Spring MVC controllers and REST controllers are adapters only; server-side
  validation and authorization in Service are authoritative.
- Thymeleaf views are placed under `src/main/resources/templates`.
- Every future phase must state why a new dependency is within `REQUEST.md`.
- Client-side validation/AJAX may improve UX later, but may not become a second
  business implementation.
