# Hệ thống quản lý đề tài sinh viên

Đây là ứng dụng MVC monolith cho Khoa Công nghệ Thông tin, được đặc tả trong
[`REQUEST.md`](REQUEST.md).

Đây là đồ án môn học nên mục tiêu hiện tại là MVP có thể demo end-to-end:
đăng nhập → đợt đăng ký → đề tài → nhóm → đăng ký → báo cáo → đánh giá/kết
quả. Không tự mở rộng thành hệ thống production hoặc triển khai hội đồng nhiều
tầng, dashboard, audit log, email và nhiều phiên bản báo cáo nếu chưa được
chọn rõ trong Must/Should/Nice to Have.

## Tài liệu nguồn cho code generation

- `REQUEST.md` là nguồn sự thật cho mục tiêu, chức năng, quy tắc nghiệp vụ và
  các điểm chưa được xác nhận.
- `.okf/` chứa quy trình spec-driven: standards, agents, workflows, phase và
  task notes.
- `docs/` chứa đặc tả miền, workflow, phân quyền, dữ liệu, màn hình và các
  quyết định cần giữ ổn định khi sinh code; `docs/course-alignment.md` phân
  biệt nội dung môn học với yêu cầu project.

Đọc theo thứ tự: `REQUEST.md` → `.okf/README.md` → phase hiện tại → tài liệu
liên quan trong `docs/`.

## Stack dùng trong project

- Java 21 + Spring Boot/Spring MVC
- Thymeleaf + Layout Dialect cho server-side rendering
- Spring Data JPA/Hibernate
- Spring Security với BCrypt và RBAC
- MySQL cho dữ liệu thật; H2 in-memory cho test/first-look
- Maven + embedded Tomcat
- HTML/CSS/JavaScript thuần cho giao diện MVP

Không đưa SPA, React/Vue/Angular, microservices, Docker/Kubernetes hoặc CI/CD
vào phiên bản đầu.

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

Chạy lần lượt [`database/ddl.sql`](database/ddl.sql) và
[`database/seed.sql`](database/seed.sql) trên MySQL trước khi chạy app.
Sau đó override các biến runtime:

```powershell
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/hcmute_topic_management?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = '<local-secret>'
$env:DB_DRIVER = 'com.mysql.cj.jdbc.Driver'
mvn spring-boot:run
```

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

Tài khoản local và quyền mẫu được tạo bởi `database/seed.sql`; không commit
mật khẩu mới hoặc mật khẩu plaintext vào source.
