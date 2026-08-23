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
| Servlet/JSP/JSTL | JSP/JSTL server-rendered pages under `WEB-INF/views`; Spring MVC runs on Jakarta Servlet/Tomcat. |
| Spring/Spring MVC | Main MVC framework, dependency injection and controller layer. |
| RESTful API | REST controllers under `/api` in the same WAR; they reuse Service/DAO and do not become a separate service or SPA backend. |
| JDBC | DAO persistence with MySQL and `PreparedStatement`; no ORM is assumed. |
| Bootstrap + jQuery | Use Tailwind CSS + jQuery; Bootstrap is not a project dependency. |
| Java Mail | Optional Nice to Have notification adapter; it is not required for the core workflow. |

The baseline uses Spring Framework Core/Spring MVC directly with Java
configuration and `DispatcherServlet`; Spring Boot is not part of this code
base.

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
