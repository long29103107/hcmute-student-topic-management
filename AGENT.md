# Agent Instructions

Đây là entry point cho coding agent làm việc trong repository này. Giữ file
này đồng bộ với `REQUEST.md`, `README.md` và `.okf/`.

## Mission

Xây dựng hệ thống quản lý đề tài sinh viên cho Khoa CNTT theo đúng stack môn
học: Java + Spring MVC/Jakarta Servlet, JSP/JSTL SSR, RESTful API trong cùng
monolith, JDBC/MySQL, Maven/Tomcat, HTML/CSS/JS, Tailwind CSS, jQuery và Java
Mail tùy chọn. Dùng Spring Framework Core/Spring MVC trực tiếp; không dùng
Spring Boot.

`REQUEST.md` là nguồn sự thật. Những điểm chưa rõ trong mục 13 không được tự
biến thành quy tắc nghiệp vụ bắt buộc.

## Required reading

Trước khi thay đổi code hoặc tài liệu lâu dài, đọc:

1. `REQUEST.md`
2. `.okf/standards/architecture.md`
3. `.okf/standards/coding-style.md`
4. `.okf/standards/testing.md`
5. Workflow tương ứng trong `.okf/workflows/`
6. Phase summary và các tài liệu `docs/` liên quan

## Working rules

- Giữ Spring MVC Controller/REST Controller mỏng; nghiệp vụ nằm trong Service,
  truy cập dữ liệu nằm trong DAO/JDBC, JSP chỉ render dữ liệu.
- Mọi truy vấn nhận input phải dùng `PreparedStatement`.
- Kiểm tra quyền, session, trạng thái và thời gian ở server.
- Dùng transaction cho các thao tác nhiều bước như tạo nhóm, đăng ký đề tài,
  chấm điểm và công bố kết quả.
- Không lưu mật khẩu plaintext; không commit secret, password, token hoặc
  file `.env`.
- Không đưa SPA, microservices, Docker hoặc hạ tầng ngoài phạm vi vào code nếu
  task không được cập nhật rõ trong `REQUEST.md`.
- Không sửa/khôi phục thay đổi không liên quan của người dùng.
- Khi hoàn thành task, cập nhật phase summary sau khi đã verification.

## Verification

Chạy kiểm tra nhỏ nhất phù hợp và báo rõ phần bị bỏ qua:

```powershell
mvn test
mvn package
```

Nếu task chạm MySQL/Tomcat, bổ sung smoke test theo `docs/verification.md` khi
môi trường đã có các dịch vụ đó.
