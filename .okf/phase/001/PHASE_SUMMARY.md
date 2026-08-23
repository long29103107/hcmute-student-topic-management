---
phase: 001
title: Maven Spring MVC Foundation, Identity and Accounts
status: in_progress
created_at: 2026-08-20
updated_at: 2026-08-20
current_task: 001_001
task_count: 6
done_count: 0
depends_on: []
---

# Phase 001 Summary

## Phase Goal

Tạo WAR Maven chạy trên Tomcat với Spring MVC/Jakarta Servlet, JSP/JSTL SSR,
REST health/auth adapters, cấu trúc Controller → Service → DAO/JDBC, MySQL
schema nền tảng, đăng nhập/session/phân quyền và quản lý users/departments.

## Phase Done Criteria

- `mvn test` và `mvn package` chạy thành công, tạo WAR.
- Kết nối MySQL lấy từ runtime config; schema users/departments có thể tạo lặp.
- Password không lưu plaintext; login/logout/session và route protection hoạt động.
- Admin quản lý tài khoản, role, active/locked.
- Faculty Head quản lý departments.
- JSP nằm dưới `WEB-INF/views`, không có SQL/business logic trong view.

## Scope

In:

- Maven/Tomcat/Spring MVC/Jakarta Servlet scaffold.
- Spring MVC SSR view resolver và REST controller baseline.
- MySQL datasource, schema nền tảng và seed tối thiểu nếu cần.
- Authentication, session, role authorization.
- User/account và department CRUD.

Out:

- Registration periods, topics, groups, reports, boards, scores and
  notifications.
- Any stack or infrastructure outside `REQUEST.md`.

## Task Index

| Task | Title | Status | Done At |
|---|---|---|---|
| 001_001 | Maven WAR và Spring MVC skeleton | in_progress | |
| 001_002 | DataSource, schema và DAO nền tảng | planned | |
| 001_003 | Login, logout, session và role filter | planned | |
| 001_004 | Admin quản lý users | planned | |
| 001_005 | Faculty Head quản lý departments | planned | |
| 001_006 | Foundation verification và phase closure | planned | |

## Current Task

Current task: `001_001`. Build/package đã đạt; còn chờ smoke trên Tomcat 10.1.

## Completed Notes

No phase tasks are complete yet.

## Next Task Proposal

Complete the Tomcat 10.1 runtime smoke for `001_001`; after that, start
`001_002` for MySQL schema/DAO implementation.

## Task Notes

### 001_001 - Maven WAR và Spring MVC skeleton

#### Step Goal

Tạo `pom.xml`, WAR packaging, Spring MVC application context, DispatcherServlet
mapping, component scan, common filters/interceptors, error pages, asset folders
và một SSR home route cùng REST health route.

#### Dependency

- `REQUEST.md` and `.okf/standards/architecture.md`.

#### Scope

In: Spring MVC/Jakarta Servlet/JSTL dependencies, Maven compiler, Tomcat
deployable WAR, layer folders, `WEB-INF/views`, Tailwind/jQuery assets,
`@Controller`/`@RestController` placeholders.

Out: business tables, authentication behavior, real pages beyond scaffold.

#### Acceptance Criteria

- WAR packages and deploys to the selected Tomcat version.
- An SSR request flows through encoding filter → Spring MVC Controller → JSP.
- A REST request flows through filter → `@RestController` → JSON response.
- Spring MVC is used without ORM; no SPA dependency is introduced.

#### Affected Files

- `pom.xml`, `src/main/java`, `src/main/resources`, `src/main/webapp`,
  `README.md`.

#### Verification

- `mvn package`; deploy minimal WAR to Tomcat if available; smoke SSR home and
  REST health route.

#### Foundation for Next Step

All later code has stable Java package, resource and view locations.

#### Done Notes

Implementation completed for the current code-base scaffold. Runtime smoke is
pending because the machine only has Tomcat 9, while this project uses Jakarta
Servlet 6/Tomcat 10.1.

### 001_002 - DataSource, schema và DAO nền tảng

#### Step Goal

Thiết lập MySQL configuration/DataSource, transaction helper và schema cho
`users`, `departments`; tạo DAO interfaces/implementations với PreparedStatement.

#### Dependency

- `001_001`.

#### Scope

In: runtime properties/env mapping, schema script, connection lifecycle,
`UserDao`, `DepartmentDao`, safe exception mapping.

Out: topic/period schema, ORM, hard-coded credentials.

#### Acceptance Criteria

- Schema creates from repository resources.
- DAO closes JDBC resources and never concatenates user input into SQL.
- Connection values are absent from Java source and versioned secrets.

#### Affected Files

- `config/`, `dao/`, `model/`, `src/main/resources/db/`, tests and Maven config.

#### Verification

- Unit tests for mapping/validation; MySQL integration test when DB is available;
  `mvn test`, `mvn package`.

#### Foundation for Next Step

Authentication and account screens can use one configured data access boundary.

#### Done Notes

Not started.

### 001_003 - Login, logout, session và role filter

#### Step Goal

Implement password-hash verification, login/logout, session lifecycle and
coarse role route protection.

#### Dependency

- `001_002`.

#### Scope

In: `AuthController`, auth filter/interceptor, safe session user DTO,
BCrypt (or approved configured hash), invalid-login messages and access-denied.

Out: enhanced authentication beyond the session-based login required by
`REQUEST.md`.

#### Acceptance Criteria

- Correct credentials create a session without storing plaintext password.
- Logout invalidates session.
- Unauthenticated and wrong-role direct URL requests are blocked.
- Service remains the authority for resource-level permission.

#### Affected Files

- `service/auth`, `controller/auth`, optional `rest/auth`, `filter`, auth JSPs
  and user tests.

#### Verification

- Authentication unit tests plus Spring MVC/REST tests for
  success/failure/logout/403; `mvn test`.

#### Foundation for Next Step

Every later flow can depend on a consistent `CurrentUser` and role check.

#### Done Notes

Not started.

### 001_004 - Admin quản lý users

#### Step Goal

Cho Admin xem, tạo, cập nhật, khóa/mở khóa tài khoản và gán bốn system roles
được phép trong `REQUEST.md`.

#### Dependency

- `001_003`.

#### Scope

In: list/form/actions, server validation, password reset/change hash path,
role/active update, PRG and audit-safe messages.

Out: advanced audit log, email invitations, additional approver roles.

#### Acceptance Criteria

- Chỉ Admin thực hiện mutation qua cả UI và direct URL.
- Locked user cannot authenticate.
- Role values are allowlisted; duplicate login identifier is rejected.

#### Affected Files

- `UserService`, `UserDao`, `AdminUserController`, optional REST adapter,
  admin JSPs and tests.

#### Verification

- Service authorization/validation tests; Spring MVC/REST smoke;
  `mvn test/package`.

#### Foundation for Next Step

User and lecturer/student identities exist for academic workflows.

#### Done Notes

Not started.

### 001_005 - Faculty Head quản lý departments

#### Step Goal

Implement CRUD/active management for departments and make the service expose a
safe list for future topic forms.

#### Dependency

- `001_004`.

#### Scope

In: Faculty Head route, department validation, unique code/name policy,
active/inactive handling.

Out: department-scoped analytics or additional hierarchy.

#### Acceptance Criteria

- Faculty Head can manage departments; other roles cannot mutate them.
- Inactive department cannot be selected for a new topic.
- Existing references are handled without deleting data blindly.

#### Affected Files

- `DepartmentService`, `DepartmentDao`, `DepartmentController`, optional REST
  adapter, JSP and tests.

#### Verification

- DAO/service tests and direct SSR/REST authorization smoke;
  `mvn test/package`.

#### Foundation for Next Step

Topics can require exactly one valid department.

#### Done Notes

Not started.

### 001_006 - Foundation verification và phase closure

#### Step Goal

Verify deployable foundation across layers and record evidence before opening
Phase 002.

#### Dependency

- `001_001` through `001_005` complete.

#### Scope

In: full Maven suite, schema bootstrap, login/admin/department smoke and docs.

Out: new features.

#### Acceptance Criteria

- All phase done criteria have evidence in Done Notes.
- Skipped MySQL/Tomcat checks are listed with reason, not silently omitted.

#### Affected Files

- tests, `README.md`, this phase summary, `docs/verification.md` if needed.

#### Verification

- `mvn test`, `mvn package`, MySQL/Tomcat smoke when available.

#### Foundation for Next Step

Phase 002 may add period/topic workflow on stable identity and department data.

#### Done Notes

Not started.
