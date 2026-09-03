# Spring MVC HTTP and REST Contract Standard

REST endpoints are required only for the selected MVP/core flow. Do not add
AJAX-specific endpoints for search, dashboards, email or extended board
management unless the corresponding Should/Nice to Have scope is approved.

This project is a Spring MVC monolith with SSR pages and a RESTful API adapter.
The API is deployed in the same executable Spring Boot JAR and reuses the same
Service/repository contracts;
it is not a separate service or SPA backend.

- Keep SSR routes and `/api` routes documented in `docs/ui-route-map.md`.
- Use explicit request DTOs or command objects between Controller and Service.
- Keep domain entities out of Thymeleaf when a view model can expose less data.
- Keep domain entities out of REST JSON when a response DTO can expose less data.
- Use stable error keys for field-level validation and Vietnamese messages at
  the presentation boundary.
- Use PRG (Post/Redirect/Get) for successful state changes.
- Use normal HTTP methods and status codes for REST resources/actions.
- REST endpoints must call the same server-side authorization, validation and
  transaction logic as SSR forms.
- Return an appropriate 404/403/400 page or redirect for invalid access; never
  expose stack traces or SQL details.
- Any AJAX added for a Should Have feature must call a Spring MVC REST endpoint
  that reuses the same Service rules; it must not create a second business path.
