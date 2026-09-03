# Plan: Student, Lecturer and Faculty Head account directories

## Vision alignment

- Active milestone: `001 — Identity and access`.
- Product Vision capability: role-aware User CRUD, server-side authorization,
  separate role directories and safe account details.
- Advances task: `001_003 — Complete role-aware User CRUD, MSSV handling,
  status management and credential reset`.
- User outcome: an authorized administrator can create the appropriate account,
  browse a role-specific list and open a safe detail view for Students,
  Lecturers and Faculty Heads.
- Explicitly out of scope: topic/department/registration workflows, runtime
  role/permission CRUD, delete/reset/status changes beyond existing behavior, audit
  history, search API optimization and separate authentication tables.

## Objective

Implement one shared User service contract and separate Student/Lecturer-
capability directory surfaces. The role determines which identity fields and permissions are shown;
it must not create a second User/credential persistence path.

### Role behavior

| Role | Create account | List/detail data | Role rule |
|---|---|---|---|
| `STUDENT` | Enter unique 8-digit MSSV in `users.login_identifier`; server generates readonly `<MSSV>@student.hcmute.edu.vn` | Account fields, status, roles and MSSV | MSSV is immutable in normal edit and unique in the database |
| `LECTURER` | Enter and server-validate email; no MSSV/profile fields | Account fields, status, roles and email | Add Lecturer assigns only `LECTURER` |
| `FACULTY_HEAD` | Create/prepare a normal staff account, then explicitly assign `FACULTY_HEAD` through role assignment | Account fields, status, roles and email | Receives the explicit seeded Faculty Head permission bundle; no runtime role inheritance |

Faculty Head creation remains a separate role-assignment step. A dedicated
Faculty Head directory may list/detail accounts with that active role, but the
Add Lecturer flow must never silently grant it.

## Priority and MVP boundary

- Priority: Must Have.
- Core MVP capability affected: User CRUD and identity/access foundation.
- Optional scope explicitly approved: a REST read adapter for the same list and
  detail contracts, if the existing API surface is required by the client.

## Canonical contracts

Prefer a single role-filtered resource over three duplicated business paths:

### SSR

- `GET /admin/users?role=STUDENT|LECTURER|FACULTY_HEAD` — role directory list.
- `GET /admin/users/{id}` — safe account detail view; verify the account has
  the requested/visible role before rendering a role-specific link.
- Existing `POST /admin/users` remains the shared create command. The server
  validates the selected role and applies the Student/Lecturer rules above.
- Existing edit route remains the write path; normal Student edit ignores any
  submitted MSSV change.

### REST adapter (same Service path)

- `GET /api/admin/users?role=STUDENT|LECTURER|FACULTY_HEAD` — list DTOs.
- `GET /api/admin/users/{id}` — detail DTO.
- `POST /api/admin/users` — create command DTO, only if REST is needed by the
  current client; never expose entities or password hashes.
- Return stable `403`, `404` and `400` responses and reuse SSR service
  authorization/validation.

## Steps

1. Update `docs/ui-route-map.md` and define the SSR/REST response fields,
   role filter semantics and authorization matrix before implementation.
2. Add repository queries for active role membership and detail loading. Keep
   `users`, `roles` and `user_roles`; do not add
   `lecturer_users` or `faculty_head_users` tables.
3. Split the service contract into safe list/detail view models and shared
   create commands. Exclude password hashes and enforce active role checks,
   Student MSSV/email generation, Lecturer email validation and explicit
   Faculty Head assignment.
4. Add the Student and Lecturer-capability SSR directory routes/views. Reuse
   the existing Flowbite/Thymeleaf account template; show MSSV/profile data
   only for Students and show email-only staff data for Lecturer/Faculty Head.
5. Keep Faculty Head accounts in the Lecturer-capability directory; role
   assignment remains explicit and must not create a separate directory.
6. Add REST DTOs/controller only when the current client needs them; route all
   mutations and reads through the same service as SSR.
7. Add focused tests for each role’s create/list/detail path, duplicate and
   invalid data, role filtering, authorization, password-hash exclusion and
   Student MSSV immutability.
8. Run `mvn test`, `mvn package` and the relevant MySQL/Tomcat smoke checks;
   update the phase summary with evidence.

## Boundaries

- Spring MVC Controller/REST Controller: bind path/query/JSON, select view or
  response status, and pass safe view models; no role business rules.
- Service: role authorization, role-specific validation, transaction boundaries,
  MSSV/email derivation and shared list/detail use cases.
- DAO/JDBC: role-filtered and detail queries with prepared statements. In the
  current codebase, preserve the existing repository implementation rather
  than introducing a parallel persistence abstraction.
- JSP/assets: SSR rendering, Flowbite modal/list/detail UI and responsive
  presentation only; no append-based dynamic business flow.
- Database: shared authentication tables with unique `users.login_identifier`
  storing the MSSV for Student accounts; no role-specific credential tables.

## Verification

- Student create generates the expected email, rejects duplicate/invalid MSSV,
  and list/detail expose MSSV without exposing password data.
- Lecturer create accepts a valid email, rejects invalid/duplicate email, and
  list/detail never render Student fields.
- Lecturer-capability list/detail includes active `LECTURER` and
  `FACULTY_HEAD` accounts; its Lecturer-capability permissions come from the
  explicit seeded `role_permissions` bundle, not an implicit second role.
- An account with both `LECTURER` and `FACULTY_HEAD` appears in both relevant
  directories and retains both explicit role assignments.
- Anonymous, non-admin and insufficient-permission requests receive the
  expected denial; unknown ids return the expected not-found behavior.
- Normal Student edit keeps the original MSSV and generated email.
- `mvn test`, `mvn package` and `git diff --check` pass; unavailable MySQL/
  Tomcat checks are recorded rather than guessed.
