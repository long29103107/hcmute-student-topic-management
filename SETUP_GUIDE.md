# Setup local database và seed dữ liệu

> Tài liệu này bám theo schema, seed pipeline và cấu hình runtime hiện tại của
> HCMUTE Student Topic Management.

## 1. Yêu cầu môi trường

- Java 21
- Maven 3.9+ hoặc Maven Wrapper đi kèm project
- MySQL 8+ và tài khoản có quyền tạo database/table
- Node.js và npm nếu cần build lại Tailwind CSS hoặc đồng bộ Flowbite
- Git

Kiểm tra nhanh trong PowerShell:

```powershell
java -version
mvn -version
mysql --version
node --version
npm --version
```

MySQL là database runtime bắt buộc. H2 chỉ được cấu hình cho test và first-look.

## 2. Cài frontend assets

Nếu `node_modules` chưa có, cài dependency:

```powershell
npm install
```

Sau khi sửa template/CSS hoặc khi checkout source mới, build lại asset:

```powershell
npm run build:assets
```

Lệnh này chạy `build:css` rồi đồng bộ Flowbite vendor vào
`src/main/resources/static/js/vendor/flowbite.min.js`. Nếu chỉ cần build
Tailwind, dùng:

```powershell
npm run build:css
```

## 3. Tạo database và schema

Đảm bảo MySQL Server đang chạy, sau đó mở PowerShell tại thư mục project.

### Cách 1: MySQL CLI

File DDL tự tạo database `hcmute_topic_management` nếu database chưa tồn tại:

```powershell
Get-Content -Raw .\database\1.ddl.sql | mysql -u root -p
```

Nhập password MySQL khi được hỏi. Nếu user local không có password, dùng
`Get-Content -Raw .\database\1.ddl.sql | mysql -u root`.

### Cách 2: DBeaver

1. Tạo connection tới MySQL bằng user có quyền tạo database.
2. Mở `database/1.ddl.sql`.
3. Chọn **Execute SQL Script** hoặc **Alt+X**.
4. Không dùng **Execute SQL Statement** cho toàn bộ file vì DDL có nhiều câu
   SQL và có lệnh `CREATE DATABASE`/`USE`.

Schema hiện tại do `database/1.ddl.sql` quản lý explicit. Ứng dụng dùng
`spring.jpa.hibernate.ddl-auto=none` và tắt Flyway vì project chưa có migration
riêng; app không tự tạo hoặc reset schema khi khởi động.

Nếu database `hcmute_topic_management` chưa tồn tại, phải chạy DDL bằng MySQL
CLI hoặc DBeaver trước khi khởi động app. Endpoint `/api/seed/ddl` chỉ drop và
tạo lại các bảng trong database mà app đã kết nối được; nó không thể bootstrap
một database connection chưa tồn tại.

Kiểm tra schema:

```powershell
mysql -u root -p -D hcmute_topic_management -e "SHOW TABLES;"
```

DDL hiện tại tạo 18 bảng:

```text
roles, permissions, departments, users, user_roles, role_permissions,
registration_periods, announcements, topics, topic_supervisors, student_groups,
group_members, topic_registrations, reports, review_boards,
review_board_members, evaluations, registration_results
```

Nếu database được tạo từ phiên bản cũ, hãy backup dữ liệu cần giữ rồi tạo lại
schema hiện tại. Project không duy trì script patch cho schema legacy.

## 4. Cấu hình kết nối ứng dụng

Ứng dụng mặc định chạy ở port `5000` và dùng:

```text
jdbc:mysql://127.0.0.1:3306/hcmute_topic_management
username: root
password: empty
```

Khuyến nghị override bằng biến môi trường thay vì sửa source hoặc commit
password. Trong PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://127.0.0.1:3306/hcmute_topic_management?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
$env:SPRING_DATASOURCE_USERNAME = 'root'
$env:SPRING_DATASOURCE_PASSWORD = '<mysql-password>'
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = 'com.mysql.cj.jdbc.Driver'
```

Nếu MySQL local không có password:

```powershell
$env:SPRING_DATASOURCE_PASSWORD = ''
```

Các cấu hình runtime đáng chú ý:

| Biến môi trường | Mặc định | Mục đích |
|---|---:|---|
| `SERVER_PORT` | `5000` | Port HTTP của ứng dụng |
| `REPORT_STORAGE_DIRECTORY` | `storage/reports` | Nơi lưu bytes của report |
| `REPORT_UPLOAD_MAX_SIZE_BYTES` | `10485760` | Giới hạn bytes của report |
| `REPORT_UPLOAD_MAX_FILE_SIZE` | `10MB` | Giới hạn file multipart |
| `REPORT_UPLOAD_MAX_REQUEST_SIZE` | `11MB` | Giới hạn request multipart |
| `EVALUATION_SCORE_MIN` | `0` | Điểm tối thiểu |
| `EVALUATION_SCORE_MAX` | `10` | Điểm tối đa |
| `SESSION_COOKIE_SECURE` | `false` | Bật khi chạy sau HTTPS |

OAuth Google và remember-me đều tắt mặc định. Chỉ bật OAuth khi đã cung cấp
đầy đủ `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` và
`GOOGLE_OAUTH_ALLOWED_DOMAINS`. Nếu bật remember-me, bắt buộc cung cấp
`REMEMBER_ME_KEY` runtime tối thiểu 32 ký tự.

## 5. Chạy ứng dụng

Build/test backend:

```powershell
mvn test
mvn package
```

Test dùng H2 in-memory theo `src/test/resources/application.properties`, không
cần trỏ test vào MySQL. Chạy app local:

```powershell
mvn spring-boot:run
```

Sau đó mở:

```text
http://localhost:5000/seed
```

## 6. Chạy bằng Docker Compose

Docker Compose chạy app cùng MySQL 8.4. DDL được MySQL thực thi tự động ở lần
khởi tạo volume đầu tiên; seed fixture vẫn là thao tác chủ động trên `/seed`.

```powershell
docker compose up --build
```

Sau khi hai container healthy, mở:

```text
http://localhost:5000/seed
```

Chạy seed trên trang này rồi dùng các workflow trong `docs/HUMAN_WORKFLOW.md`.
Compose bám các default trong `src/main/resources/application.properties`:
app port `5000`, MySQL database `hcmute_topic_management`, user `root` và
password rỗng. MySQL service dùng `MYSQL_ALLOW_EMPTY_PASSWORD=yes` cho local
và chỉ expose trong Docker network, không chiếm port MySQL `3306` trên host.
Riêng datasource URL dùng hostname Docker service là `mysql`; không dùng
`127.0.0.1` vì app đang chạy trong container riêng.

Dừng container nhưng giữ dữ liệu:

```powershell
docker compose down
```

Xoá cả database volume để chạy lại từ schema sạch:

```powershell
docker compose down -v
```

`SEED_PUBLIC_ENABLED=true` chỉ phù hợp local. Khi dùng shared/staging, đặt
`SEED_PUBLIC_ENABLED=false` và cấu hình password MySQL riêng, không dùng
empty-password Compose setup.

## 7. Seed dữ liệu theo pipeline

Trang `/seed` gọi các API POST theo đúng thứ tự sau; mỗi bước chỉ chạy sau khi
bước trước thành công:

1. `POST /api/seed/ddl` — drop và tạo lại 18 bảng từ `database/1.ddl.sql`.
2. `POST /api/seed/permissions` — tạo/cập nhật 28 permissions.
3. `POST /api/seed/roles` — tạo/cập nhật 4 roles.
4. `POST /api/seed/role-permissions` — tạo lại mapping role-permission.
5. `POST /api/seed/departments` — tạo/cập nhật 15 departments.
6. `POST /api/seed/users` — tạo/cập nhật 71 accounts và role assignment.
7. `POST /api/seed/registration-periods` — tạo 1 registration period `OPEN`.
8. `POST /api/seed/student-groups` — tạo 4 student groups, mỗi group tối đa 3
   thành viên.
9. `POST /api/seed/topics` — tạo 8 topics và 13 supervisor assignments.
10. `POST /api/seed/topic-registrations` — tạo 3 registrations ở trạng thái
    `APPROVED`.
11. `POST /api/seed/review-boards` — tạo 2 review boards, 6 board members và
    6 evaluations.
12. `POST /api/seed/announcements` — tạo 3 announcements.

Seed fixture sau cùng gồm:

- 4 roles: `ADMIN`, `FACULTY_HEAD`, `LECTURER`, `STUDENT`.
- 28 permissions; `ADMIN` nhận toàn bộ, `FACULTY_HEAD` nhận bundle lecturer
  cộng các quyền faculty workflow, còn `LECTURER` và `STUDENT` nhận bundle
  riêng theo seed.
- 15 departments.
- 71 users: 1 admin, 4 Faculty Heads, 16 Lecturers và 50 Students.
- 4 groups: Phoenix, Orion, Nova và Atlas.
- 8 topics, 13 supervisor assignments và 3 approved registrations.
- 2 review boards: một board `PUBLISHED` có 3 điểm đã submit và result trung
  bình `8.50`, một board `ACTIVE` có các evaluation draft.
- 3 announcements: 1 school-wide published, 1 CNTT published và 1 CNTT draft.

Các bước fixture có thể chạy lại để upsert/cập nhật dữ liệu mẫu. Bước DDL là
destructive vì drop/recreate toàn bộ schema.

## 8. Chế độ bảo vệ seed

`SEED_PUBLIC_ENABLED` quyết định quyền truy cập `/seed` và `/api/seed/**`:

- `true`: local bootstrap anonymous được phép mở trang và gọi pipeline, nhưng
  các request POST vẫn phải có CSRF token. Trang `/seed` tự lấy token và gửi
  trong request.
- `false`: `/seed` và các API seed yêu cầu tài khoản có role `ADMIN`.

Ứng dụng hiện mặc định `SEED_PUBLIC_ENABLED=true` để bootstrap local. Sau khi
seed xong, nên dừng app và chạy lại với chế độ bảo vệ:

```powershell
$env:SEED_PUBLIC_ENABLED = 'false'
mvn spring-boot:run
```

Không bật public seed trên shared, staging hoặc production.

## 9. Reset/reseed đầy đủ bằng API admin

`POST /api/admin/seed` yêu cầu role `ADMIN`, có CSRF khi gọi từ browser, và
truncate 18 bảng trước khi seed lại roles, permissions, departments, users,
groups, topics, registrations, review boards, results và announcements. Endpoint
này không tự chạy DDL; database phải có sẵn 18 bảng trước khi gọi.

Ví dụ gọi từ browser đã đăng nhập bằng admin:

```text
POST /api/admin/seed
```

Response chính có các field:

```text
tablesReset=18
roles=4
permissions=28
departments=15
users=71
registrationPeriods=1
studentGroups=4
topics=8
topicSupervisors=13
registrations=3
reviewBoards=2
reviewBoardMembers=6
facultyHeads=4
lecturers=16
students=50
announcements=3
registrationResults=1
```

Không gọi endpoint này trên database có dữ liệu cần giữ. Nếu database chưa có
bảng, chạy pipeline `/seed` bắt đầu từ `POST /api/seed/ddl` trước.

## 10. Tài khoản local mặc định

Sau khi seed thành công:

| Email đăng nhập | Password | Role |
|---|---|---|
| `admin@hcmute.edu.vn` | `admin123` | `ADMIN` |

Staff dùng domain `@lecturer.hcmute.edu.vn`; Student dùng MSSV làm login
identifier và email dạng `<MSSV>@student.hcmute.edu.vn`. Các tài khoản local
fixture dùng password hash trong source; thông tin trên chỉ dành cho local,
không dùng khi chia sẻ database hoặc deploy.

Một số fixture hữu ích:

- Faculty Head CNTT: `nguyen.van.khang@lecturer.hcmute.edu.vn`
- Student: `24110000@student.hcmute.edu.vn`
- Student accounts có MSSV từ `24110000` đến `24110049`.

## 11. Kiểm tra seed thành công

```powershell
mysql -u root -p -D hcmute_topic_management -e "SELECT code FROM roles ORDER BY id;"
mysql -u root -p -D hcmute_topic_management -e "SELECT code FROM permissions ORDER BY id;"
mysql -u root -p -D hcmute_topic_management -e "SELECT login_identifier, email_or_code AS email FROM users ORDER BY id;"
mysql -u root -p -D hcmute_topic_management -e "SELECT COUNT(*) AS groups_count FROM student_groups;"
mysql -u root -p -D hcmute_topic_management -e "SELECT scope, status, COUNT(*) AS total FROM announcements GROUP BY scope, status;"
mysql -u root -p -D hcmute_topic_management -e "SELECT status, COUNT(*) AS total FROM registration_results GROUP BY status;"
```

Kiểm tra nhanh expected count:

```text
roles=4, permissions=28, departments=15, users=71
registration_periods=1, student_groups=4, topics=8
topic_registrations=3, review_boards=2, announcements=3
registration_results=1
```

## 12. Lỗi thường gặp

### `Communications link failure` hoặc connection refused

- Kiểm tra MySQL Server đã chạy chưa.
- Kiểm tra port MySQL, thường là `3306`.
- Kiểm tra `SPRING_DATASOURCE_URL`, username và password.
- Đảm bảo database name là `hcmute_topic_management`.

### `Table doesn't exist`

Database chưa có schema hiện tại. Chạy lại DDL:

```powershell
Get-Content -Raw .\database\1.ddl.sql | mysql -u root -p
```

Hoặc mở `/seed` và chạy pipeline từ bước `POST /api/seed/ddl`.

### `/seed` hoặc API seed trả về `403`

- Nếu đang bootstrap local, chạy app với:

  ```powershell
  $env:SEED_PUBLIC_ENABLED = 'true'
  mvn spring-boot:run
  ```

- Nếu dùng chế độ bảo vệ, đăng nhập bằng tài khoản có role `ADMIN`.
- Không bỏ qua CSRF khi gọi POST từ browser; nên chạy pipeline trên trang
  `/seed` để trang tự gửi token.

### `/api/admin/seed` báo lỗi dù `/api/seed/**` chạy được

Đây là hai cơ chế khác nhau. `/api/admin/seed` luôn yêu cầu `ADMIN` và chỉ
truncate/reseed schema đã tồn tại; nó không thay thế bước DDL đầu tiên.

### Đổi biến môi trường nhưng app vẫn dùng cấu hình cũ

Dừng process Maven đang chạy, mở terminal mới, set lại biến môi trường rồi
chạy lại `mvn spring-boot:run`. Nếu dùng IntelliJ, kiểm tra Environment
variables trong Run Configuration.

### Build giao diện thiếu CSS hoặc Flowbite

Chạy lại:

```powershell
npm install
npm run build:assets
```
