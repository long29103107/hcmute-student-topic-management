# Agent Instructions

Đây là entry point cho coding agent làm việc trong repository này. Giữ file
này đồng bộ với `REQUEST.md`, `README.md` và `.okf/`.

## Mission

Xây dựng hệ thống quản lý đề tài sinh viên cho Khoa CNTT theo stack hiện tại:
Java 21 + Spring Boot/Spring MVC trên Jakarta Servlet, Thymeleaf SSR,
RESTful API trong cùng monolith, Spring Data JPA/Hibernate + MySQL,
Maven/executable JAR với embedded Tomcat, HTML/CSS/JS, Tailwind CSS, Flowbite
và Java Mail tùy chọn.

`PRODUCT_VISION.md` là nguồn sự thật ưu tiên cho product scope, milestone và
việc tạo task. `REQUEST.md` và `docs/` là tài liệu tham chiếu cho implementation
và business rules khi không mâu thuẫn với Product Vision. Những điểm chưa rõ
không được tự biến thành quy tắc nghiệp vụ bắt buộc.

## Course-project MVP boundary

Đây là đồ án môn học, không phải hệ thống production đầy đủ. Ưu tiên một luồng
MVC chạy được end-to-end với các thực thể lõi:
`User`, `Department`, `RegistrationPeriod`, `Topic`, `StudentGroup`,
`TopicRegistration`, `Report` và `Evaluation`.

- Chỉ xây module/bảng/màn hình cần cho Must Have trong `REQUEST.md`.
- Bản MVP dùng một phiên đánh giá và ít nhất một giảng viên được phân công;
  không tự mở rộng thành hội đồng 3–5 người, chair/secretary hoặc nhiều tầng
  reviewer nếu task/rubric chưa yêu cầu.
- Hội đồng đầy đủ, dashboard, audit log, email, AJAX search, nhiều phiên bản
  báo cáo và UI polish thuộc Should/Nice to Have.
- Khi requirement chi tiết và scope MVP mâu thuẫn, giữ capability nghiệp vụ
  nhưng chọn mô hình dữ liệu và giao diện tối giản; hỏi lại chỉ khi ảnh hưởng
  tiêu chí nghiệm thu.

## Current identity rules

- All password logins use the normalized, case-insensitive `email_or_code`
  value as the login email. `login_identifier` remains an immutable internal
  account identifier and is not accepted by the login form. Student creation
  requires a unique 8-digit MSSV; the server assigns `STUDENT`, generates the
  login email as `<MSSV>@student.hcmute.edu.vn` and leaves `password_hash`
  unset. The UI does not allow editing the generated email or setting a
  password in the create flow. An account without a password is blocked from
  login until an administrator sets one through edit/reset. Lecturer creation
  auto-assigns `LECTURER` and uses the manually entered, server-validated email
  as the login email; a separate login identifier is not accepted.
  Lecturer creation also leaves `password_hash` unset; an administrator must
  set a password through edit/reset before the account can log in.
  The admin directory exposes separate Manage students, Manage lecturers and
  Manage faculty heads views, while the reusable account modal is shared where
  the action is allowed.
- Keep `FACULTY_HEAD` and `LECTURER` as shared access roles assigned through
  `user_roles`; never split authentication into role-specific user tables.
  The default `FACULTY_HEAD` permission bundle includes all Lecturer
  permissions plus department, registration-period, topic-review and
  topic-registration-review permissions. This is an explicit role-permission
  bundle rather than runtime role inheritance. Add Lecturer creates only
  `LECTURER`; `FACULTY_HEAD` is granted separately.
- `ADMIN` is the full-capability system role. The seed assigns every current
  permission to Admin, route-level authorization accepts Admin for Lecturer,
  Faculty Head and Student workspaces, and the sidebar exposes available role
  workspaces to Admin. Resource-specific own/group/assigned constraints remain
  explicit service rules unless an Admin-wide operational view is defined.
- The revised schema has no separate Student Profile table. For Student
  accounts, the immutable `users.login_identifier` is the unique 8-digit MSSV;
  the generated student email is stored in `users.email_or_code`.
- Normal User edit never changes the MSSV/login identifier. A future MSSV change
  must be a separately authorized action/API.
- The admin-only `/seed` page destructively drops and recreates the 17 revised
  schema tables through `POST /api/seed/ddl`, then invokes the permissions,
  roles, role-permissions, departments, users and registration-periods endpoints
  in that order with
  `fetch` plus the session CSRF token; keep a confirmation before running it.
  These endpoints and the page require an authenticated `ADMIN` by default.
  For a local bootstrap before an admin exists, `SEED_PUBLIC_ENABLED=true`
  explicitly enables anonymous access while CSRF remains required; never set
  that flag in a deployed environment. The legacy `POST /api/admin/seed`
  endpoint remains destructive and always requires an authenticated `ADMIN`.
  Existing legacy databases should be backed up and recreated from
  `database/1.ddl.sql`; no legacy patch script is maintained.

## Required reading

Trước khi tạo task/plan hoặc thay đổi code, đọc:

1. `PRODUCT_VISION.md`
2. `REQUEST.md`
3. `.okf/standards/architecture.md`
4. `.okf/standards/coding-style.md`
5. `.okf/standards/testing.md`
6. Workflow tương ứng trong `.okf/workflows/`
7. Phase summary và các tài liệu `docs/` liên quan

## Working rules

- Giữ Spring MVC Controller/REST Controller mỏng; nghiệp vụ nằm trong Service,
  truy cập dữ liệu nằm trong Spring Data repository, Thymeleaf chỉ render dữ liệu.
- Mọi truy vấn tùy biến phải dùng tham số binding của JPA/JPQL; không nối chuỗi
  input vào query.
- Kiểm tra quyền, session, trạng thái và thời gian ở server.
- Dùng transaction cho các thao tác nhiều bước như tạo nhóm, đăng ký đề tài,
  chấm điểm và công bố kết quả.
- Không lưu mật khẩu plaintext; không commit secret, password, token hoặc
  file `.env`.
- Không đưa SPA, microservices, Docker hoặc hạ tầng ngoài phạm vi vào code nếu
  task không được cập nhật rõ trong `REQUEST.md`.
- Không tạo task mới ngoài active milestone trong `PRODUCT_VISION.md`. Mỗi task
  hoặc plan mới phải ghi rõ `Vision alignment`, outcome và out-of-scope boundary.
- Không sửa/khôi phục thay đổi không liên quan của người dùng.
- Khi hoàn thành task, cập nhật tài liệu/phase summary phù hợp sau khi đã verification.
- Sau mỗi lần update code hoặc UI, cập nhật document liên quan và ghi một entry
  ngắn vào `AGENT_MEMORY.md` để các lượt làm việc sau giữ được context.
- Feedback sau các thao tác quản trị dùng fragment toast tái sử dụng tại
  `src/main/resources/templates/fragments/toast.html`; layout đọc các flash
  attribute `successMessage`, `warningMessage`, `errorMessage`. Logic đóng và
  tự ẩn/countdown phải nằm trong `static/js/app.js` vì CSP không cho inline
  script. Toast hiển thị tối đa 3 item và fragment nhận `duration` theo giây.
- User directory canonical routes are `/admin/students` and `/admin/lecturers`,
  backed by separate controllers and form models. Keep `/admin/users` only as
  a legacy compatibility route; new navigation and flows must use the typed
  resource path.
- Student directory tables must omit the Roles column entirely because the
  `/admin/students` route already scopes every account to the Student role;
  lecturer/legacy directories may continue rendering role badges.
- Student edit modals must show the immutable MSSV field only; do not
  render password-reset or system-role controls in the normal edit flow. The
  shared form fragment must gate those sections by `studentAccount`, not only
  by the generic `editing` flag.
- Dashboard and canonical student/lecturer directory content must use the full
  available content width with internal responsive padding, matching Roles &
  permissions; do not add `max-w-7xl` wrappers to those pages.
- Treat Roles & permissions as the visual reference for all administration
  pages going forward: full-width content, responsive inner padding, compact
  Flowbite-style panels, and consistent headings/actions/forms/tables/modals/
  toasts. Only use a constrained `max-w-*` layout when the page genuinely
  needs it and the task explicitly justifies the exception.
- Topic proposal create/edit modals are a justified constrained exception:
  keep the modal panel at `max-w-2xl` so the form stays focused instead of
  expanding across the full viewport.
- Inline explanatory helper text for immutable/generated fields should use the
  shared info-icon tooltip pattern in the user form, with hover and keyboard
  focus support instead of always-visible paragraphs.
- Canonical Student and Lecturer directories use shared server-side pagination,
  search filtering, and sortable visible columns. Keep `search`, `sort`,
  `direction`, `page`, and `size` in pagination/sort links; the search input
  also has a visible Search button and a short debounce on typing.
- Mobile administration layouts must constrain wrappers with `min-width: 0`
  and contain wide data tables inside their panel scroll area; never allow a
  table to create horizontal overflow on the whole page.
- Passwords are not collected during Student/Lecturer creation or normal edit;
  credential setup uses the separate reusable Set password modal and the
  dedicated `/{id}/password` action protected by `USER_UPDATE`.
- Registration periods are managed at `/faculty/periods` by users with
  `PERIOD_MANAGE` (Faculty Head and Admin). The Service validates both lecturer
  and student windows, restricts reviewer and council milestones to graduation
  thesis periods, enforces the forward-only `DRAFT -> OPEN -> CLOSED ->
  ARCHIVED` lifecycle, exposes the read-only period-window contract used by
  topic modules, and stores the active creator in `registration_periods.created_by`.
- Error pages for HTTP 400, 401, 403, 404, 500 and 503 live as standalone templates
  under `src/main/resources/templates/error/`. They must not decorate the main
  layout or include the application sidebar/header/footer; the 403 page is also
  the Spring Security access-denied destination. Keep their shared visual
  styling in `static/css/error.css` instead of relying on generated Tailwind
  utilities that may be stale until assets are rebuilt. Browser error handling
  redirects to semantic paths such as `/not-found` and `/forbidden`; API
  requests keep their status/JSON contract. `/error/{status}` remains a legacy
  alias.

## Verification

Chạy kiểm tra nhỏ nhất phù hợp và báo rõ phần bị bỏ qua:

```powershell
mvn test
mvn package
```

Nếu task chạm MySQL/Tomcat, bổ sung smoke test theo `docs/verification.md` khi
môi trường đã có các dịch vụ đó.
