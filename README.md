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

## Stack dùng trong môn học và project

- Java + Spring Framework/Spring MVC trên Jakarta Servlet
- JSP + JSTL, server-side rendering
- RESTful API trong cùng Spring MVC monolith
- JDBC + MySQL
- Maven + Apache Tomcat
- HTML/CSS/JavaScript, Tailwind CSS và jQuery
- Java Mail cho luồng email tùy chọn

Giao diện dùng Tailwind CSS. Không đưa SPA, React/Vue/Angular,
microservices, Docker/Kubernetes hoặc CI/CD vào phiên bản đầu.

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

## Code base hiện tại

Nền tảng đã có:

- WAR Maven chạy trên Tomcat 10.1.
- Java config cho Spring Framework Core/Spring MVC; không dùng Spring Boot.
- `@Controller` cho SSR và `@RestController` cho `/api`.
- `DataSource`/DAO JDBC với `PreparedStatement` và schema nền tảng cho users/departments.
- JSP/JSTL dưới `WEB-INF/views`.
- Tailwind CLI và jQuery WebJar.

### Build backend

```powershell
mvn clean test package
```

WAR tạo tại `target/hcmute-student-topic-management.war`.

### Build Tailwind

```powershell
npm install
npm run css:build
```

### Cấu hình MySQL

Override các biến runtime trước khi chạy Tomcat:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/hcmute_topic_management?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = '<local-secret>'
```

Các route kiểm tra nền tảng sau khi deploy:

- `/` — trang SSR foundation.
- `/api/health` — REST health response.

Project dùng Spring Framework Core/Spring MVC trực tiếp với
`DispatcherServlet`/Java config; không thêm `spring-boot-starter-*`.
