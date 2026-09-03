# Setup database và seed dữ liệu

> Tài liệu này hướng dẫn chạy database MySQL cho local và seed dữ liệu mẫu cho
> project HCMUTE Student Topic Management.

## 1. Yêu cầu môi trường

- Java 21
- Maven
- MySQL 8 trở lên
- Git

Có thể dùng DBeaver hoặc MySQL Command Line Client để chạy file SQL.

Kiểm tra nhanh:

```powershell
java -version
mvn -version
mysql --version
```

## 2. Tạo database và schema

Đảm bảo MySQL Server đang chạy, sau đó mở PowerShell tại thư mục project.

### Cách 1: chạy bằng MySQL CLI

File DDL sẽ tự tạo database `hcmute_topic_management` nếu database chưa tồn tại:

```powershell
mysql -u root -p < database/1.ddl.sql
```

Nhập password MySQL khi được hỏi.

### Cách 2: chạy bằng DBeaver

1. Tạo connection tới MySQL bằng user có quyền tạo database.
2. Mở file `database/1.ddl.sql`.
3. Chọn **Execute SQL Script** hoặc **Alt+X**.
4. Không dùng **Execute SQL Statement** cho toàn bộ file vì file có nhiều câu SQL.

Nếu database đã được tạo từ phiên bản cũ của project, hãy backup dữ liệu cần
giữ rồi tạo lại database theo schema hiện tại. Project không còn duy trì script
patch cho schema legacy.

Kiểm tra schema:

```powershell
mysql -u root -p -D hcmute_topic_management -e "SHOW TABLES;"
```

## 3. Cấu hình kết nối cho ứng dụng

Ứng dụng mặc định chạy tại port `5000` và kết nối tới:

```text
jdbc:mysql://127.0.0.1:3306/hcmute_topic_management
```

Khuyến nghị cấu hình bằng biến môi trường thay vì ghi password vào source code.
Trong PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://127.0.0.1:3306/hcmute_topic_management?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
$env:SPRING_DATASOURCE_USERNAME = 'root'
$env:SPRING_DATASOURCE_PASSWORD = '<mysql-password>'
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = 'com.mysql.cj.jdbc.Driver'
```

Nếu MySQL local không có password, dùng:

```powershell
$env:SPRING_DATASOURCE_PASSWORD = ''
```

## 4. Chạy ứng dụng

```powershell
mvn spring-boot:run
```

Mở trang:

```text
http://localhost:5000/seed
```

## 5. Seed dữ liệu theo pipeline

Trang `/seed` gọi các API theo đúng thứ tự sau:

1. `POST /api/seed/ddl` — tạo schema từ `database/1.ddl.sql`.
2. `POST /api/seed/permissions` — tạo permissions.
3. `POST /api/seed/roles` — tạo roles.
4. `POST /api/seed/role-permissions` — gán permissions cho roles.
5. `POST /api/seed/users` — tạo tài khoản mẫu và role assignment.

### Seed không cần đăng nhập trên local

Local mặc định đã bật chế độ bootstrap anonymous. Nếu muốn tắt anonymous
migration, mở một PowerShell mới và chạy:

```powershell
$env:SEED_PUBLIC_ENABLED = 'false'
mvn spring-boot:run
```

Khi `SEED_PUBLIC_ENABLED=true`, `/seed` và các API pipeline không yêu cầu login
nhưng vẫn yêu cầu CSRF token. Chỉ giữ giá trị `true` trên local. Trang sẽ tự
gửi CSRF token cho các request POST.

### Seed khi đã có tài khoản admin

Để giữ seed page ở chế độ bảo vệ, đặt:

```powershell
$env:SEED_PUBLIC_ENABLED = 'false'
mvn spring-boot:run
```

Đăng nhập bằng tài khoản admin rồi mở `/seed` và chạy pipeline.

> Không bật `SEED_PUBLIC_ENABLED=true` trên server public hoặc môi trường
> production. Sau khi bootstrap local xong, nên tắt app và chạy lại với giá trị
> `false`.

## 6. Tài khoản local mặc định

Sau khi seed thành công, tài khoản quản trị là:

| Email đăng nhập | Password | Role |
|---|---|---|
| `admin@hcmute.local` | `admin123` | `ADMIN` |

Thông tin này chỉ dùng cho local development. Đổi password trước khi chia sẻ
database hoặc triển khai ứng dụng.

Seed cũng tạo các fixture role/user phục vụ test:

- `FACULTY_HEAD`
- `LECTURER`
- `STUDENT`

## 7. Kiểm tra seed thành công

```powershell
mysql -u root -p -D hcmute_topic_management -e "SELECT code FROM roles ORDER BY id;"
mysql -u root -p -D hcmute_topic_management -e "SELECT code FROM permissions ORDER BY id;"
mysql -u root -p -D hcmute_topic_management -e "SELECT login_identifier, email_or_code AS email FROM users ORDER BY id;"
mysql -u root -p -D hcmute_topic_management -e "SELECT login_identifier, email_or_code FROM users WHERE email_or_code LIKE '%@student.hcmute.edu.vn';"
```

Database local chuẩn thường có:

- 4 roles
- 21 permissions
- 4 users
- 1 Student account fixture with MSSV `24110000`

DDL hiện tại là bản revised ngày 2026-09-03, gồm 17 bảng. So với schema cũ,
`student_groups` được scope theo `period_id`, evaluation hỗ trợ nhiều lecturer
cho một registration, và có thêm `review_boards`, `review_board_members` cùng
`registration_results`. Hãy backup rồi tạo lại database cũ; project không duy
trì patch migration cho dữ liệu legacy.

## 8. Chạy lại seed và lưu ý dữ liệu

Các endpoint trên trang `/seed` có thể chạy lại để tạo/cập nhật fixture. Riêng
bước `role-permissions` sẽ tạo lại các mapping role-permission theo bundle mặc
định.

Endpoint reset/reseed đầy đủ sau đây có tính destructive: endpoint truncate 17
bảng của schema revised trước khi seed lại roles, permissions và users:

```text
POST /api/admin/seed
```

Response trả về `tablesReset: 17`, `roles: 4`, `permissions: 21` và `users: 4`.
Không gọi endpoint này trên database có dữ liệu cần giữ. Nếu đã xoá hẳn
database, chạy `POST /api/seed/ddl` trước; endpoint reset/reseed chỉ hoạt động
khi 17 bảng đã tồn tại.

## 9. Một số lỗi thường gặp

### `Communications link failure` hoặc connection refused

- Kiểm tra MySQL Server đã chạy chưa.
- Kiểm tra port MySQL, thường là `3306`.
- Kiểm tra `SPRING_DATASOURCE_URL`, username và password.

### `Table doesn't exist`

Chạy lại DDL trên database mới:

```powershell
mysql -u root -p < database/1.ddl.sql
```

Với database cũ, backup dữ liệu cần giữ rồi tạo lại schema bằng
`database/1.ddl.sql`.

### Trang `/seed` trả về `403`

- Nếu dùng anonymous bootstrap, kiểm tra:

  ```powershell
  $env:SEED_PUBLIC_ENABLED = 'true'
  ```

- Nếu seed page đang bảo vệ, đăng nhập bằng tài khoản có role `ADMIN`.
- Không gọi các API POST seed trực tiếp mà bỏ qua CSRF; nên chạy từ trang `/seed`.

### Đổi biến môi trường nhưng app vẫn dùng cấu hình cũ

Tắt process Maven đang chạy, mở terminal mới, set lại biến môi trường rồi chạy
lại `mvn spring-boot:run`.
