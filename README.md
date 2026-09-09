# Hệ thống quản lý đề tài sinh viên

Đây là ứng dụng MVC monolith cho Khoa Công nghệ Thông tin. Product scope và
milestone hiện tại được xác định trong [`PRODUCT_VISION.md`](PRODUCT_VISION.md).

Milestone hiện tại chỉ hoàn thành Identity and Access: Login, User, Role và
seed-managed Role/Permission catalog và authorization. Các module học vụ được ghi nhận trong Later
product roadmap và chỉ được mở sau khi user chọn scope tiếp theo.

## Tài liệu nguồn cho code generation

- [`SETUP_GUIDE.md`](SETUP_GUIDE.md) — hướng dẫn tạo database MySQL và seed dữ liệu local.
- [`Dockerfile`](Dockerfile) và [`docker-compose.yaml`](docker-compose.yaml) — chạy app cùng MySQL bằng Docker Compose.

- `PRODUCT_VISION.md` là nguồn ưu tiên để agent tạo task, plan hoặc quyết định
  product scope.
- `.okf/README.md` chứa project memory về yêu cầu, business rules và workflow;
  các phase summary ghi implementation memory chi tiết.
- `.okf/` chứa quy trình spec-driven: standards, agents, workflows, phase và
  task notes.
- `docs/` chứa đặc tả miền, workflow, phân quyền, dữ liệu, màn hình và các
  quyết định cần giữ ổn định khi sinh code; `docs/course-alignment.md` phân
  biệt nội dung môn học với yêu cầu project.

Đọc theo thứ tự: `PRODUCT_VISION.md` → `.okf/README.md` → phase summary liên
quan → tài liệu liên quan trong `docs/`.

## Stack dùng trong project

- Java 21 + Spring Boot 4.1.1 / Spring MVC
- Thymeleaf + Thymeleaf Layout Dialect cho server-side rendering
- Spring Data JPA/Hibernate, với schema MySQL quản lý bằng DDL explicit
- Spring Security với BCrypt và RBAC
- MySQL cho dữ liệu thật; H2 in-memory cho test/first-look
- Maven + executable JAR + embedded Tomcat
- Tailwind CSS 4 + Flowbite 4 cho UI; JavaScript thuần cho các tương tác riêng

Không đưa SPA, React/Vue/Angular, microservices, Kubernetes hoặc CI/CD vào
phiên bản đầu. Docker Compose được hỗ trợ cho local app + MySQL setup; xem
[`SETUP_GUIDE.md`](SETUP_GUIDE.md).

### Frontend assets

Tailwind CSS được build từ `src/main/resources/static/css/input.css`. Flowbite
được tích hợp qua Tailwind plugin/source scan và JavaScript vendor local để các
component tương tác vẫn chạy khi deploy không có CDN:

```powershell
npm install
npm run build:assets
```

## Quy ước triển khai

Mỗi task phải giữ validation và kiểm tra quyền ở server, dùng
transaction cho thao tác nhiều bước, escape dữ liệu trong Thymeleaf và không
ghi mật khẩu dạng plaintext. Thông tin kết nối MySQL phải
được lấy từ cấu hình/runtime, không hard-code trong Java source.

Các lệnh kiểm tra dự kiến sau khi scaffold Maven được tạo:

```powershell
mvn test
mvn package
```

Việc chạy đầy đủ cần MySQL; Tomcat được đóng gói sẵn trong executable JAR.

## Chạy backend

```powershell
mvn test
mvn spring-boot:run
```

Không cấu hình biến môi trường thì app dùng H2 in-memory để debug giao diện.

### Cấu hình MySQL

Chạy [`database/1.ddl.sql`](database/1.ddl.sql) trên database mới. Database
cũ cần được backup và tạo lại theo schema hiện tại trước khi chạy seed.
Sau đó override các biến runtime:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://127.0.0.1:3306/hcmute_topic_management?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
$env:SPRING_DATASOURCE_USERNAME = 'root'
$env:SPRING_DATASOURCE_PASSWORD = '<local-secret>'
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = 'com.mysql.cj.jdbc.Driver'
mvn spring-boot:run
```

### Seed dữ liệu local qua API

Mặc định, sau khi có một tài khoản `ADMIN` đang đăng nhập, mở `/seed` và bấm
**Run seed pipeline**. Mỗi lần chạy pipeline sẽ xoá toàn bộ 18 bảng của schema
revised, chạy lại `database/1.ddl.sql` và tạo lại toàn bộ fixture local. Nếu cần
bootstrap database local trước khi có tài khoản,
có thể bật tạm migration anonymous bằng biến môi trường:

```powershell
$env:SEED_PUBLIC_ENABLED = 'true'
mvn spring-boot:run
```

Khi bật cờ này, `/seed` và các API pipeline không yêu cầu login nhưng vẫn yêu
cầu CSRF token. Chỉ bật trên local, sau khi migrate xong hãy tắt cờ. Trang sẽ
gọi tuần tự các API dưới đây:

```text
POST /api/seed/ddl
POST /api/seed/permissions
POST /api/seed/roles
POST /api/seed/role-permissions
POST /api/seed/departments
POST /api/seed/users
POST /api/seed/registration-periods
POST /api/seed/student-groups
POST /api/seed/topics
POST /api/seed/topic-registrations
POST /api/seed/review-boards
POST /api/seed/announcements
```

Các API seed fixture có thể chạy lại; riêng `/api/seed/ddl` là bước destructive
drop/recreate schema. Các bước còn lại tạo/cập nhật roles, permissions, 15
departments, 4 faculty-head accounts, 16 lecturer accounts, 50 student accounts,
8 topics, 13 supervisor assignments, 3 approved registrations, 2 review boards with 6 members/evaluations,
one published board result (average `8.50`) and 2 published announcements.
`POST /api/admin/seed`
là API reset/reseed đầy đủ: truncate 18 bảng của schema revised trong một
transaction, sau đó tạo lại 4 roles, 26 permissions, 15 departments và 71 tài
khoản (gồm 4 faculty heads, 16 lecturers, 50 students, admin), 3 approved registrations, 2 review boards với 6 board members/evaluations,
1 published registration result và 2 announcements.
Đây là thao
tác destructive dành
cho local; không gọi trên database có dữ liệu cần giữ. Khi xoá hẳn database,
hãy chạy `POST /api/seed/ddl` trước rồi mới chạy các bước seed fixture. Khi không
bật anonymous migration, database mới cần một bước bootstrap admin riêng trước
khi gọi API. Tài khoản fixture dùng mật khẩu local đã được hash trong source.
Tài khoản quản trị mặc định là email `admin@hcmute.edu.vn` /
`admin123`; chỉ dùng thông tin này cho môi trường local và đổi trước khi chia
sẻ hoặc deploy.

Khi tạo Student từ `Manage students`, MSSV được dùng để tạo email đăng nhập,
`STUDENT` được gán tự động và password để trống. Admin cần mở Edit để thiết lập
password trước khi Student có thể đăng nhập bằng email.

### Cấu hình security khi chạy thật

- `SESSION_COOKIE_SECURE=true` khi ứng dụng chạy sau HTTPS.
- Remember-me bị tắt mặc định. Nếu bật bằng `REMEMBER_ME_ENABLED=true`, phải
  cung cấp `REMEMBER_ME_KEY` runtime có tối thiểu 32 ký tự.
- Google OAuth chỉ bật khi có credential, domain được phép qua
  `GOOGLE_OAUTH_ALLOWED_DOMAINS`, và email Google phải khớp một tài khoản đang
  được cấp trong hệ thống.

Schema revised được quản lý explicit bằng `database/1.ddl.sql`; Hibernate để
`ddl-auto=none` và Flyway được tắt cho đến khi project có migration riêng. App
không tự reset schema khi khởi động; thao tác drop/recreate chỉ chạy khi Admin
chủ động bấm seed pipeline.
Schema hiện có 18 bảng, bao gồm `announcements`, `review_boards`, `review_board_members`,
`evaluations` theo lecturer và `registration_results`.

Các route hiện có:

- `/login` — đăng nhập bằng email và password.
- `/dashboard` — dashboard sau khi đăng nhập.
- `/profile` — thông tin tài khoản và role hiện tại.

Tài khoản local và quyền mẫu được tạo bởi `POST /api/admin/seed`; không commit
mật khẩu mới hoặc mật khẩu plaintext vào source.
