# Course alignment

## Document role

`Chapter 00 - Introduction.pdf` is a course-outline reference. It identifies
technologies taught in the subject; it does not override project-specific
business requirements in `REQUEST.md`.

## Technologies identified in the course outline

- MVC web application development on the Java platform.
- Servlet, JSP, JSTL and JDBC.
- Spring Framework and Spring MVC.
- RESTful API.
- UI library content with Bootstrap and jQuery.
- Java Mail.

## Project decisions derived from the course and REQUEST

| Course topic | Project use |
|---|---|
| Servlet/SSR | Spring MVC server-rendered pages using Thymeleaf templates; embedded Tomcat is provided by Spring Boot. |
| Spring/Spring MVC | Spring Boot 4.1.1 provides the executable application, dependency injection and MVC controller layer. |
| RESTful API | REST controllers under `/api` in the same executable Spring Boot application; they reuse the Service/repository layer and do not become a separate service or SPA backend. |
| JDBC/data access | Spring Data JPA/Hibernate maps the revised MySQL schema; repositories expose persistence contracts and Service owns transactions. |
| Bootstrap + jQuery | Use Tailwind CSS 4 + Flowbite 4 for the UI layer; Bootstrap is not a project dependency. |
| Java Mail | Optional Nice to Have notification adapter; it is not required for the core workflow. |

The implemented baseline is the Spring Boot executable JAR described in
`pom.xml`. The revised schema is managed explicitly through
`database/1.ddl.sql`, with Hibernate schema generation disabled and Flyway
disabled until versioned migrations are introduced.

## Scope guardrails

- `REQUEST.md` controls roles, business rules, data relationships and Must
  Have acceptance criteria.
- The course outline does not require every chapter technology to be a
  Must Have feature. Spring MVC/REST are architectural tools; Java Mail stays
  optional because email is Nice to Have in `REQUEST.md`.
- The project remains one deployable MVC monolith. Do not introduce a SPA,
  microservices, Docker/Kubernetes or a separate REST service.
- SSR and REST adapters must call the same Service layer so validation,
  authorization, transactions and grading rules are not duplicated.
