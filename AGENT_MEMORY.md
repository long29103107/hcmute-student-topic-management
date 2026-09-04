# Agent Memory

Durable project context for future coding-agent sessions. Keep entries concise,
factual and free of secrets. Update this file after every code or UI update.

## 2026-09-04 — Account directory layout and department display

- Canonical account directories are `/admin/students` and `/admin/lecturers`;
  `/admin/users` remains a legacy compatibility route.
- The `Account` cell displays the user's full name first and `email_or_code`
  below it without a leading `@`. There is no separate `Email` table column.
- Student and Lecturer directories display Department code/name in a separate
  column; missing assignments render as `Not assigned`.
- Admin account create/edit forms support selecting an active department, and
  `UserManagementService` persists the selection to `users.department_id`.
- Existing accounts without a department remain valid; selecting an inactive or
  unknown department is rejected by the service.
- Verification after this update: `mvn test` — 54 tests passed.

## 2026-09-04 — Department delete and action dropdown

- Admin Department actions now use a dropdown matching the Student directory;
  it contains Edit, Activate/Deactivate and Delete.
- `POST /admin/departments/{id}/delete` is protected by Admin plus
  `DEPARTMENT_MANAGE` and CSRF.
- Delete is allowed only when no users and no topics reference the department.
  In-use departments remain available for deactivation instead, with a flash
  error explaining the blocker.
- A dedicated delete confirmation modal is rendered per department.
- Documents updated: `docs/ui-route-map.md` and
  `docs/authorization-matrix.md`.
- Verification: `mvn test` — 56 tests passed.

## 2026-09-04 — Department directory search and sorting

- The admin Department directory supports server-side search across department
  code, name and Active/Inactive status.
- Code, department name and status headers sort ascending/descending and retain
  the current search term.
- Department results are paginated with a default page size of 10; pagination
  links retain the current search and sort query state.
- Invalid sort or direction query values fall back to name ascending.
- Documents updated: `docs/ui-route-map.md`.
- Verification: `mvn test` — 58 tests passed.

## 2026-09-04 — Topic Proposal CRUD

- Project #5 task #4, `[002_001] Topic Proposal CRUD`, was selected from Ready
  and moved to In progress.
- Lecturer-capable users with `TOPIC_PROPOSE` can list, create and edit their
  own proposals at `/lecturer/topics` and `/api/lecturer/topics`.
- Creation/update requires an active department and an `OPEN` registration
  period whose lecturer window includes the current time; only owned `DRAFT`
  and `REJECTED` proposals are editable, and a corrected rejected proposal
  returns to `DRAFT`.
- Documents and phase planning updated: `docs/ui-route-map.md`,
  `docs/domain-model.md`, `.okf/phase/002/PHASE_SUMMARY.md` and
  `.okf/phase/002/002_001-topic-proposal-crud.md`.
- Verification: focused tests pass 6/6; full `mvn test` passes 64/64 after
  updating the security regression expectation for the now-existing protected
  topic route.
- `mvn package -DskipTests` also passes and produces the executable JAR. All
  six GitHub acceptance criteria are checked and the Project #5 item is now
  Done.

## 2026-09-04 — Authenticated login redirect

- `GET /login` now redirects authenticated users to `/dashboard`; anonymous
  users and users arriving after logout still receive the login page.
- Documents updated: `docs/ui-route-map.md`.
- Verification: `mvn -Dtest=SecurityConfigTest test` — 7 tests passed; full
  `mvn test` — 65 tests passed; `mvn package -DskipTests` passed.

## 2026-09-04 — Admin full capability rule

- Admin is the full-capability system role, with all current seeded
  permissions and route gates for Lecturer, Faculty Head and Student
  workspaces.
- The sidebar exposes the available Lecturer and Faculty workflow sections to
  Admin; method/service checks still preserve explicitly documented ownership
  or assigned-resource constraints.
- Documents updated: `AGENT.md`, `docs/authorization-matrix.md`.
- Verification: focused seed/security tests pass 11/11; full `mvn test` passes
  66/66; `mvn package -DskipTests` passes and produces the executable JAR.

## 2026-09-04 — Standalone HTTP error pages

- Added standalone Thymeleaf pages for HTTP 400, 401, 403, 500 and 503 under
  `src/main/resources/templates/error/`; none uses the main layout or includes
  the application sidebar, header or footer.
- Spring Boot's error controller resolves the generic status pages, while the
  Spring Security access-denied flow renders the same 403 template.
- Documents updated: `AGENT.md`, `docs/ui-route-map.md`.
- Verification: focused `SecurityConfigTest` passes 13/13; full `mvn test`
  passes 71/71; `mvn package -DskipTests` passes.

## 2026-09-04 — Error page visual refresh

- Replaced the first error-page styling with a centered responsive card, clear
  status typography, branded identity, contextual copy and usable actions.
- Moved error-page styling to `static/css/error.css` so the standalone pages do
  not break when the generated Tailwind bundle has not been rebuilt.
- Documents updated: `AGENT.md`, `docs/ui-route-map.md`.
- Verification: local 403 page inspected in the browser; focused
  `SecurityConfigTest` passes 13/13; full `mvn test` passes 71/71; package
  passes.

## 2026-09-04 — Error page redirect paths

- Added explicit `/error/400`, `/error/401`, `/error/403`, `/error/500` and
  `/error/503` routes that render the matching standalone page while preserving
  its HTTP status.
- HTML error dispatches and browser access-denied responses redirect to the
  matching error path; API access-denied responses retain their status/JSON
  behavior. `/access-denied` remains a compatibility redirect to `/error/403`.
- Documents updated: `AGENT.md`, `docs/ui-route-map.md`.
- Verification: focused `SecurityConfigTest` passes 14/14; full `mvn test`
  passes 72/72; `mvn package -DskipTests` passes.

## 2026-09-04 — Semantic error URLs

- Canonical error page URLs are now `/bad-request`, `/unauthorized`,
  `/forbidden`, `/not-found`, `/internal-server-error` and
  `/service-unavailable`; the matching 404 page was added.
- HTML error redirects and browser access-denied redirects use the semantic
  paths. Existing `/error/{status}` routes remain available as compatibility
  aliases and preserve the corresponding HTTP status.
- Documents updated: `AGENT.md`, `docs/ui-route-map.md`.
- Verification: focused `SecurityConfigTest` passes 15/15; full `mvn test`
  passes 73/73; `mvn package -DskipTests` passes.

## Entry template

### YYYY-MM-DD — Short update title

- Code/UI change:
- Business or authorization rule:
- Documents updated:
- Verification:
