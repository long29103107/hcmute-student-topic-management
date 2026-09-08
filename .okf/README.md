# OKF Operating Guide

Thư mục này theo dõi các phase và implementation task của hệ thống quản lý
đề tài sinh viên.

## How to work

1. Đọc `PRODUCT_VISION.md` để xác định milestone và scope được phép tạo task.
2. Đọc phần Project memory bên dưới để nắm stack, workflow và business rules
   đã được chốt.
3. Đọc `.okf/standards/` và tài liệu liên quan trong `docs/`, đặc biệt
   `docs/course-alignment.md` khi chọn framework/UI.
4. Đọc `PHASE_SUMMARY.md` của phase liên quan để lấy implementation memory và
   trạng thái ticket.
5. Chọn task nhỏ nhất phù hợp với active milestone.
6. Implement một lát cắt hoàn chỉnh theo task note.
7. Chạy verification rồi mới cập nhật trạng thái task/phase.

## Source of truth

- `PRODUCT_VISION.md`: nguồn ưu tiên về product scope, milestone, task order và
  điều kiện để tạo task mới.
- `PHASE_SUMMARY.md`: implementation memory, ticket outcome, verification và
  contract bền vững của từng phase.
- Phần Project memory bên dưới: bản tóm tắt đã chuyển từ các planning/spec
  notes cũ; dùng cùng `PRODUCT_VISION.md` và không được override Product Vision.
- `.okf/standards/`: quy tắc Spring MVC/REST, coding, Servlet/Thymeleaf, security,
  Java Mail và test.
- `docs/`: đặc tả miền và hợp đồng dùng chung để sinh code.

## Project memory

### Purpose and stack

Hệ thống là MVC monolith server-rendered cho quy trình quản lý đề tài sinh
viên của Khoa CNTT. Stack chuẩn: Java 21, Spring Boot/Spring MVC trên Jakarta
Servlet, Thymeleaf SSR, REST endpoint trong cùng executable JAR, Spring Data
JPA/Hibernate, MySQL, Maven/embedded Tomcat, Tailwind CSS/Flowbite và
JavaScript. H2 được dùng cho test/local first-look.

Không tự thêm SPA/React/Vue/Angular, microservices, Docker/Kubernetes, CI/CD,
email, dashboard hoặc audit log nâng cao nếu chưa được user chọn thành task
riêng trong Product Vision.

### Core workflow

```text
Registration period
→ Topic proposal
→ Topic approval/publication
→ Student group/topic registration
→ Registration approval
→ Report upload/access
→ Evaluator assignment
→ Scoring
→ Result publication
→ Student result view
```

### Domain rules

- Mỗi topic thuộc đúng một department và có 1–2 supervisor.
- Group tối đa 3 student, có đúng một leader; một student không thuộc nhiều
  group ACTIVE trong cùng registration period.
- Chỉ leader được submit topic registration và report.
- Topic registration phải dùng topic đã publish, đúng period và đúng registration
  window; một group chỉ có một registration hiện hành trong cùng period.
- Chỉ evaluator được assign mới được chấm; supervisor không được chấm topic
  mình hướng dẫn; score/deadline/status/publication đều được kiểm tra server-side.
- Kết quả là average của các evaluation hợp lệ và chỉ student thuộc group mới
  xem được published result.
- KLTN mới có reviewer-score deadline và council-report date; timeline sai,
  transition sai hoặc thiếu field bắt buộc phải bị từ chối.
- Announcement chỉ hiển thị khi đã publish và nằm trong scope của người đọc.

### Engineering and collaboration rules

- Controller/REST Controller mỏng; nghiệp vụ ở Service, dữ liệu ở Repository;
  REST chỉ bind DTO và gọi Service.
- Mutation phải có server-side authorization, validation, CSRF khi dùng session,
  transaction cho thao tác nhiều bước và test cho success/invalid/unauthorized/
  boundary-time.
- Query dùng binding parameters; không nối input vào JPQL/native query. Không
  hard-delete dữ liệu nghiệp vụ có lịch sử; dùng status/active/history.
- Shared enum/status/permission/DTO/DDL và table ownership là contract; module
  khác chỉ đọc qua query contract hoặc mock trong test.
- Mỗi thay đổi phải cập nhật phase summary tương ứng sau verification; status
  ticket lấy từ GitHub Project #5, không suy đoán từ file lịch sử.

### Current implementation memory

GitHub Project #5 đã được scan theo prefix và phase `001` đến `007` đều đã
hoàn tất. Mỗi phase chỉ giữ một `PHASE_SUMMARY.md`; các file đó là nơi ghi
ticket index, outcome và verification chi tiết. Không khôi phục các task note
lịch sử đã compact.

### Open decisions

Các điểm chưa được xác nhận phải giữ dạng cấu hình/placeholder, không tự biến
thành business rule: một topic có được nhiều group thực hiện hay không; cách
mời/xác nhận group member; thang điểm và làm tròn; loại topic bắt buộc có board;
report file type/size/deadline; và actor phê duyệt bổ sung nếu có giáo vụ hoặc
trưởng bộ môn. Quyết định mới ghi vào `.okf/decisions/` và phase summary bị ảnh
hưởng.

Không sao chép tài liệu hoặc phase từ dự án khác nếu chúng đưa vào stack, kiến
trúc hoặc hạ tầng ngoài phạm vi Product Vision và Project memory.

## Phase discipline

Không tự tạo phase mới. Khi user chọn rõ một milestone roadmap mới, phase note
phải ghi lại quyết định đó, có `Vision alignment` rõ ràng và giữ boundary
không mở rộng. Các điểm chưa được xác nhận trong `docs/open-questions.md`
phải giữ dạng cấu hình/placeholder hoặc được ghi rõ là chờ xác nhận, không tự
suy diễn.
