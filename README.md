# Hệ thống quản lý đề tài sinh viên

Đây là ứng dụng MVC monolith cho Khoa Công nghệ Thông tin. Product scope và
milestone hiện tại được xác định trong [`PRODUCT_VISION.md`](PRODUCT_VISION.md).

Milestone hiện tại chỉ hoàn thành Identity and Access: Login, User, Role và
Permission CRUD/authorization. Các module học vụ được ghi nhận trong Later
product roadmap và chỉ được mở sau khi user chọn scope tiếp theo.

## Tài liệu nguồn cho code generation

- `PRODUCT_VISION.md` là nguồn ưu tiên để agent tạo task, plan hoặc quyết định
  product scope.
- `REQUEST.md` là tài liệu tham chiếu về yêu cầu và business rules khi không
  mâu thuẫn với Product Vision.
- `.okf/` chứa quy trình spec-driven: standards, agents, workflows, phase và
  task notes.
- `docs/` chứa đặc tả miền, workflow, phân quyền, dữ liệu, màn hình và các
  quyết định cần giữ ổn định khi sinh code; `docs/course-alignment.md` phân
  biệt nội dung môn học với yêu cầu project.

Đọc theo thứ tự: `PRODUCT_VISION.md` → `REQUEST.md` → `.okf/README.md` → phase
`001` → tài liệu liên quan trong `docs/`.

## Stack dùng trong project

- Java 21 + Spring Boot/Spring MVC
- Thymeleaf + Layout Dialect cho server-side rendering
- Spring Data JPA/Hibernate
- Spring Security với BCrypt và RBAC
- MySQL cho dữ liệu thật; H2 in-memory cho test/first-look
- Maven + embedded Tomcat
- Tailwind CSS 4 + Flowbite 4 cho UI; JavaScript thuần cho các tương tác riêng

Không đưa SPA, React/Vue/Angular, microservices, Docker/Kubernetes hoặc CI/CD
vào phiên bản đầu.

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
`PreparedStatement`, dùng transaction cho thao tác nhiều bước, escape dữ liệu
trong JSP và không ghi mật khẩu dạng plaintext. Thông tin kết nối MySQL phải
được lấy từ cấu hình/runtime, không hard-code trong Java source.

Các lệnh kiểm tra dự kiến sau khi scaffold Maven được tạo:

```powershell
mvn test
mvn package
```

Việc chạy đầy đủ còn cần MySQL và Apache Tomcat theo cấu hình môi trường.

## Chạy backend

```powershell
mvn test
mvn spring-boot:run
```

Không cấu hình biến môi trường thì app dùng H2 in-memory để debug giao diện.

### Cấu hình MySQL

Chạy [`database/1.ddl.sql`](database/1.ddl.sql) trên database mới. Với database
đã tồn tại, chạy [`database/4.update-ddl.sql`](database/4.update-ddl.sql) để
patch các cột User/Profile hiện tại (đặc biệt `users.date_of_birth`). File
[`database/3.student-profile.sql`](database/3.student-profile.sql) là script
upgrade cũ, chỉ dùng khi cần giữ quy trình cũ.
Sau đó override các biến runtime:

```powershell
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/hcmute_topic_management?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = '<local-secret>'
$env:DB_DRIVER = 'com.mysql.cj.jdbc.Driver'
mvn spring-boot:run
```

### Seed dữ liệu local qua API

Seed không còn chạy bằng SQL thủ công. Sau khi có một tài khoản `ADMIN` đang
đăng nhập, gọi endpoint từ browser session và gửi kèm CSRF token:

```javascript
const csrfToken = document.querySelector('input[name="_csrf"]').value;
fetch('/api/admin/seed', {
  method: 'POST',
  headers: { 'X-CSRF-TOKEN': csrfToken }
});
```

API sẽ truncate toàn bộ bảng ứng dụng rồi tạo lại roles, permissions, các tài
khoản test và Student Profile mẫu. Đây là thao tác destructive dành cho local;
không gọi trên database có dữ liệu cần giữ. Vì endpoint cần tài khoản ADMIN
hiện có, database mới cần một bước bootstrap admin riêng trước khi gọi API.
Tài khoản fixture dùng mật khẩu local cũ đã được hash trong source; hãy đổi
trước khi chia sẻ hoặc deploy.

Khi tạo Student từ `Manage students`, MSSV đồng thời là login identifier,
`STUDENT` được gán tự động, email được sinh theo MSSV và password để trống.
Admin cần mở Edit để thiết lập password trước khi Student có thể đăng nhập.

### Cấu hình security khi chạy thật

- `SESSION_COOKIE_SECURE=true` khi ứng dụng chạy sau HTTPS.
- Remember-me bị tắt mặc định. Nếu bật bằng `REMEMBER_ME_ENABLED=true`, phải
  cung cấp `REMEMBER_ME_KEY` runtime có tối thiểu 32 ký tự.
- Google OAuth chỉ bật khi có credential, domain được phép qua
  `GOOGLE_OAUTH_ALLOWED_DOMAINS`, và email Google phải khớp một tài khoản đang
  được cấp trong hệ thống.

Schema MVP được quản lý explicit bằng hai file SQL nên Hibernate để
`ddl-auto=none` và Flyway được tắt cho đến khi project có migration riêng.

Các route hiện có:

- `/login` — đăng nhập bắt buộc.
- `/dashboard` — dashboard sau khi đăng nhập.
- `/profile` — thông tin tài khoản và role hiện tại.

Tài khoản local và quyền mẫu được tạo bởi `POST /api/admin/seed`; không commit
mật khẩu mới hoặc mật khẩu plaintext vào source.
