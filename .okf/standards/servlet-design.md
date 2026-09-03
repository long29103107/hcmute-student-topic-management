# Spring MVC, Servlet and View Design Standard

## Route conventions

Use human-readable Vietnamese UI labels while keeping stable Spring MVC route
names. A suggested mapping is documented in `docs/ui-route-map.md`; add SSR or
REST routes there before implementing a new screen/API.

- `GET` displays a page or detail view.
- `POST` creates a resource or executes an explicit action.
- `GET` must not mutate state.
- Mutations redirect back to a GET page after success.
- Use a common error/validation model so Thymeleaf forms can redisplay safe input.

## Controller and filter responsibilities

- `@Controller` reads/binds form parameters and returns a view name.
- `@RestController` binds JSON DTOs and returns a documented JSON response.
- Servlet filters/Spring MVC interceptors handle encoding, session and
  cross-cutting concerns.
- Check CSRF protection for state-changing forms if the application enables a
  token filter/helper.
- Delegate authorization and business validation to Service.
- Put only view data and flash messages into request/session scope.
- Never build SQL, calculate final grades, or decide role access in a Controller.

## Thymeleaf responsibilities

- Render model data with escaped Thymeleaf expressions.
- Reuse common header, navigation, flash and validation fragments.
- Show actions only when the server-provided permission model allows them, but
  treat this as UX; the server remains the authority.
- Keep file input and date/time formatting consistent with `docs/` contracts.

## REST and error handling

REST errors use a stable JSON envelope with an application error code, message
and field errors where applicable. The same Service exception mapping is used
by SSR and REST adapters.

Map expected domain errors to Vietnamese field/page messages. Do not expose
SQL, stack traces, connection strings or filesystem paths to users. Log a
correlation id and the server-side cause where logging is configured.
