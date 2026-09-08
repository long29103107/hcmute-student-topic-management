# AI Workflow Guide — HCMUTE Student Topic Management

Tài liệu này là context dùng chung cho thành viên khi giao việc cho AI trong
project HCMUTE Student Topic Management. Có thể copy toàn bộ file này vào đầu
prompt, sau đó thêm task cụ thể và các file liên quan.

Tài liệu mô tả implementation hiện tại. Nếu tài liệu này khác code, ưu tiên
theo thứ tự:

1. Code và test đang chạy.
2. PRODUCT_VISION.md.
3. .okf/phase/<phase>/PHASE_SUMMARY.md.
4. Các tài liệu trong docs/.
5. File lịch sử hoặc mô tả cũ.

Không tự tạo lại AGENT_MEMORY.md hoặc task-note Markdown riêng. Implementation
memory phải ghi trong .okf/phase/<phase>/PHASE_SUMMARY.md.

---

## 1. Overview project

### Stack và kiến trúc

- Java 21.
- Spring Boot 4.1.1, Spring MVC và Spring Security.
- MVC monolith server-rendered bằng Thymeleaf.
- REST API nằm trong cùng executable Spring Boot app và dùng chung Service.
- Spring Data JPA/Hibernate.
- MySQL là database runtime.
- H2 in-memory cho test.
- Tailwind CSS 4, Flowbite 4 và JavaScript thuần cho UI.
- Maven là build tool.

Luồng request chuẩn:

```text
Browser / REST client
  -> Security filter và session
  -> Controller / REST Controller
  -> Service: authorization + validation + transaction
  -> Repository / JPA
  -> MySQL
  -> redirect/view hoặc JSON
```

Controller chỉ bind input, chọn view/redirect và chuyển dữ liệu cho Service.
Business rule, permission, ownership, scope và transaction phải nằm ở Service.
UI ẩn button không được xem là authorization.

### Source of truth khi giao task cho AI

AI cần đọc các file sau trước khi sửa code:

- PRODUCT_VISION.md: scope và milestone.
- .okf/README.md: stack, business rule, workflow và quy tắc cộng tác.
- .okf/phase/<phase>/PHASE_SUMMARY.md: implementation memory của phase.
- docs/ui-route-map.md: route SSR/REST.
- docs/workflows.md: state và time gate.
- docs/authorization-matrix.md: role, permission và resource scope.
- SETUP_GUIDE.md: database, seed và tài khoản local.
- Code/test của module đang thay đổi.

Khi task đã có prefix như 006_xxx, phase là 006. Không tự đoán phase mới
từ tên file.

---

## 2. Overview flow chính

Flow học vụ chính:

```text
Seed / Identity
  -> Department + Registration Period
  -> Lecturer tạo Topic Proposal
  -> Faculty Head/Admin review và publish Topic
  -> Student tạo Group
  -> Group Leader submit Topic Registration
  -> Faculty Head/Admin approve hoặc reject Registration
  -> Group Leader upload Report
  -> Faculty Head/Admin assign Evaluator hoặc tạo Review Board
  -> Evaluator / Board member nhập Score
  -> Faculty Head/Admin publish Result
  -> Student xem Result của group mình
```

Announcement là flow song song:

```text
Admin / Faculty Head tạo Draft Announcement
  -> sửa nội dung và scope
  -> Publish hoặc giữ Draft/Hide
  -> người dùng chỉ thấy Announcement PUBLISHED đúng scope
```

Review Board là extension đã implement cho nhánh đánh giá nhiều người:

```text
Approved Registration
  -> Review Board DRAFT
  -> ASSIGNED
  -> SCHEDULED
  -> ACTIVE
  -> COMPLETED
  -> PUBLISHED
  -> CLOSED
```

Nếu registration dùng path evaluator cũ, hệ thống vẫn hỗ trợ một evaluator.
Nếu dùng Review Board, evaluation được materialize theo active board member và
result chỉ publish khi toàn bộ active member đã submit score hợp lệ.

---

## 3. Actors và permission

### Role hiện tại

| Role | Mục đích |
|---|---|
| ADMIN | Quản trị toàn hệ thống, cross-department |
| FACULTY_HEAD | Quản lý workflow trong department được gán |
| LECTURER | Đề xuất topic, supervise, chấm khi được assign |
| STUDENT | Tạo group, đăng ký topic, nộp report, xem result của group |

FACULTY_HEAD nhận Lecturer capability bằng mapping role-permission explicit,
không cần gán thêm role LECTURER.

### Permission bundle chính

| Role | Permission tiêu biểu |
|---|---|
| Admin | Toàn bộ permission hiện có |
| Faculty Head | Lecturer permissions + PERIOD_MANAGE, SUPERVISOR_MANAGE, TOPIC_REVIEW, REGISTRATION_REVIEW, ANNOUNCEMENT_MANAGE, REVIEW_BOARD_MANAGE, GROUP_READ, GROUP_UPDATE |
| Lecturer | TOPIC_PROPOSE, TOPIC_VIEW, REPORT_VIEW, EVALUATION_SUBMIT, RESULT_VIEW, REVIEW_BOARD_VIEW |
| Student | TOPIC_VIEW, GROUP_MANAGE, REGISTRATION_SUBMIT, REPORT_SUBMIT, REPORT_VIEW, RESULT_VIEW |

Các permission identity/admin khác gồm:

```text
DASHBOARD_VIEW
USER_READ, USER_CREATE, USER_UPDATE, USER_LOCK, USER_DELETE, USER_ROLE_ASSIGN
ROLE_READ, ROLE_UPDATE, PERMISSION_ASSIGN
DEPARTMENT_MANAGE
```

Nguyên tắc scope:

- Admin thường thấy và quản lý được tất cả department.
- Faculty Head chỉ quản lý resource thuộc department được gán.
- Lecturer chỉ sửa topic của chính mình, xem/chấm resource được assign.
- Student chỉ thao tác group/registration/report theo ownership và membership.
- Group leader là thuộc tính membership, không phải system role.
- GROUP_UPDATE cho phép Admin/Faculty Head sửa tên group, leader và lifecycle
  status trong scope; không thay thế GROUP_MANAGE của Student.
- Assignment/evaluation/report luôn được Service kiểm tra lại resource
  relationship.

---

## 4. State và business gate

### Registration period

```text
DRAFT -> OPEN -> CLOSED -> ARCHIVED
```

- Lecturer propose topic trong lecturer registration window.
- Student leader submit registration trong student registration window.
- Hai window được so sánh bằng server time và inclusive boundary.
- reviewer_score_deadline và council_report_date chỉ áp dụng cho KLTN.

### Topic

```text
DRAFT -> PENDING_APPROVAL -> APPROVED -> PUBLISHED
                         \-> REJECTED
```

- Lecturer tạo/sửa topic của chính mình khi được phép.
- Submit chuyển DRAFT hoặc REJECTED thành PENDING_APPROVAL.
- Faculty Head/Admin review.
- Topic phải PUBLISHED, period phải OPEN và student window phải mở thì
  student mới được đăng ký.

### Student group

- Group active có tối đa 3 student.
- Active group có đúng một leader và leader phải là member.
- Một student không thuộc quá một active group trong cùng period.
- Chỉ active group mới nhận join/leave/leader transfer.
- Leader không được leave trước khi chuyển leadership.
- Group directory Faculty scope nằm ở /faculty/groups.
- Student group management nằm ở /student/groups.

### Topic registration

```text
PENDING -> APPROVED
       \-> REJECTED
```

- Chỉ current group leader submit.
- Topic và period phải match.
- Group phải active, topic phải published, student window phải mở.
- Một group không có nhiều current registration trong cùng period.
- Reject bắt buộc có reason và row lịch sử không bị xóa.
- Chỉ APPROVED registration mới được upload report, assign evaluator hoặc
  tạo review board.

### Report

- Chỉ group leader của approved registration được upload.
- Bytes lưu ở external storage; database chỉ lưu metadata.
- Mặc định file tối đa 10 MB.
- Content type mặc định: PDF, DOC, DOCX.
- Metadata/download phải qua resource authorization.

### Review Board và evaluation

- Board chỉ tạo cho approved registration.
- Một registration có tối đa một board.
- Board có 3–5 active Lecturer/Faculty Head cùng department.
- Bắt buộc đúng một CHAIR và một SECRETARY.
- Topic supervisor không được làm evaluator/board member của topic đó.
- Chỉ active board member mới chấm khi board đang ACTIVE hoặc COMPLETED.
- Score mặc định từ 0 đến 10, tối đa hai chữ số thập phân.
- Không sửa score sau reviewer deadline hoặc sau result publication.
- Board chỉ publish khi board COMPLETED và toàn bộ active member có score.

### Result

- Average tính từ score hợp lệ, scale 2, rounding HALF_UP.
- Publication ghi publisher/time/status.
- Result đã PUBLISHED không sửa qua flow thường.
- Student chỉ thấy result PUBLISHED của group mà mình là member.

### Announcement

- SCHOOL chỉ Admin được tạo/quản lý.
- Faculty Head chỉ tạo announcement DEPARTMENT trong department của mình.
- Draft/hidden không xuất hiện ở trang đọc.
- Người dùng đọc announcement published tại /announcements theo scope.
- Quản lý announcement tại /announcements/manage.

---

## 5. Hướng dẫn flow theo từng bước

### Flow A — Bootstrap local và seed

Mục tiêu: tạo schema, fixture và tài khoản local để test UI/API.

1. Cài dependency frontend nếu cần:

   ```powershell
   npm install
   npm run build:assets
   ```

2. Nếu database chưa tồn tại, chạy database/1.ddl.sql bằng MySQL CLI hoặc
   DBeaver trước khi start app.
3. Cấu hình MySQL bằng SPRING_DATASOURCE_*.
4. Chạy app:

   ```powershell
   mvn spring-boot:run
   ```

5. Mở http://localhost:5000/seed.
6. Local mặc định bật SEED_PUBLIC_ENABLED=true, nên trang bootstrap không bắt
   buộc login nhưng POST vẫn có CSRF.
7. Chạy pipeline theo thứ tự:
   - DDL.
   - Permissions.
   - Roles.
   - Role-permissions.
   - Departments.
   - Users.
   - Registration periods.
   - Student groups.
   - Topics/supervisors.
   - Topic registrations.
   - Review boards/evaluations/results.
   - Announcements.
8. Đăng nhập local bằng:
   - Email: admin@hcmute.edu.vn
   - Password: admin123
9. Khi xong bootstrap, restart với SEED_PUBLIC_ENABLED=false.
10. Không chạy DDL hoặc /api/admin/seed trên database có dữ liệu cần giữ.

Fixture expected: 4 roles, 28 permissions, 15 departments, 71 users, 1 open
period, 4 groups, 8 topics, 13 supervisor assignments, 3 approved
registrations, 2 boards, 6 board members/evaluations, 3 announcements và
1 published result.

### Flow B — Admin quản lý identity

1. Đăng nhập bằng Admin.
2. Dùng /admin/students để:
   - Xem/search/paginate student.
   - Tạo student bằng MSSV 8 chữ số.
   - Student tự nhận role STUDENT.
   - Email được tạo theo <MSSV>@student.hcmute.edu.vn.
   - Set password ở action riêng.
   - Lock/unlock hoặc safe delete theo dependency.
3. Dùng /admin/lecturers để:
   - Quản lý LECTURER và FACULTY_HEAD accounts.
   - Tạo Lecturer bằng email lecturer.
   - Chọn role/capability theo flow được phép.
   - Set password ở action riêng.
4. Dùng /admin/roles để xem role và permission mapping.
5. Dùng /admin/departments để Admin tạo/sửa/deactivate department.
6. Không tạo system role/permission mới bằng runtime CRUD; thay đổi catalog
   phải đi qua seed/schema change có review.

### Flow C — Mở period và chuẩn bị topic

#### C1. Registration period

1. Admin hoặc Faculty Head mở /faculty/periods.
2. Tạo period với:
   - Type DATN hoặc KLTN.
   - Lecturer registration start/end.
   - Student registration start/end.
   - KLTN có thể có reviewer deadline/council report date.
3. Set period OPEN khi đã sẵn sàng.
4. Không bỏ qua state hoặc chuyển ngược lifecycle.

#### C2. Lecturer tạo topic

1. Lecturer mở /lecturer/topics.
2. Bấm add topic proposal.
3. Chọn department và period active phù hợp.
4. Nhập title/description.
5. Save ở DRAFT.
6. Trong lecturer registration window, submit topic.
7. Topic chuyển PENDING_APPROVAL; lecturer không tự approve/publish topic.
8. Có thể sửa topic own khi state/window cho phép.

#### C3. Supervisor assignment

1. Admin/Faculty Head mở /faculty/topics/supervisors.
2. Chọn topic trong scope.
3. Chọn 1–2 active Lecturer/Faculty Head.
4. Mỗi supervisor phải cùng department với topic.
5. Không chọn duplicate, Student, inactive account hoặc quá 2 người.
6. Save; Service transaction sẽ replace assignment cũ sau khi validate.

#### C4. Faculty review và publish

1. Faculty Head/Admin mở /faculty/topics/review.
2. Chọn topic PENDING_APPROVAL.
3. Approve hoặc reject.
4. Reject có thể lưu reason theo contract hiện tại.
5. Topic approved được đưa sang /faculty/topics/publish.
6. Publish topic khi muốn cho Student thấy trong catalog.
7. Faculty Head chỉ publish topic trong department của mình; Admin publish
   cross-department.

### Flow D — Student group và topic registration

#### D1. Tạo group

1. Student mở /student/groups.
2. Chọn period đang mở.
3. Tạo group; creator tự là leader và member đầu tiên.
4. Chia sẻ group ID để thành viên khác join.
5. Không vượt quá 3 thành viên.
6. Student chỉ thuộc một active group trong cùng period.

#### D2. Join/leave/transfer leader

1. Student join group active bằng group ID.
2. Thành viên không phải leader có thể leave.
3. Leader muốn rời group phải transfer leadership trước.
4. Chỉ transfer cho active student đang là member của group.
5. Group inactive/completed không nhận mutation membership.
6. Các thao tác membership được lock/transaction ở Service để tránh race
   condition.

#### D3. Đăng ký topic

1. Group leader mở /student/groups/register-topic.
2. Chọn topic PUBLISHED cùng period và đang trong student window.
3. Submit một registration.
4. Server kiểm tra leader, group, period, topic, window và duplicate.
5. Registration lưu PENDING.
6. Theo dõi tại /student/registrations.

#### D4. Faculty approve/reject registration

1. Faculty Head/Admin mở /faculty/registrations/review.
2. Chỉ registration trong scope được hiển thị.
3. Approve chuyển PENDING -> APPROVED.
4. Reject bắt buộc reason, chuyển PENDING -> REJECTED.
5. Registration terminal không được mutate lại qua flow review.
6. Chỉ approved registration mới đi tiếp sang report/evaluator/board.

#### D5. Faculty xem/sửa group directory

1. Faculty Head/Admin mở /faculty/groups.
2. Search, sort và pagination giữ query state.
3. Admin thấy toàn bộ group; Faculty Head chỉ thấy group có member thuộc
   department của mình.
4. Action edit chỉ hiển thị khi có GROUP_UPDATE.
5. Popup cho sửa group name, leader và status ACTIVE/COMPLETED/INACTIVE.
6. Server vẫn kiểm tra leader phải là active member trong group.
7. Member list dùng trong popup để chọn leader; không dùng UI visibility thay
   authorization.

### Flow E — Report

1. Đảm bảo registration đã APPROVED.
2. Đăng nhập bằng group leader.
3. Mở form report trong /student/registrations/{registrationId}/report.
4. Gửi groupId, periodId và multipart file.
5. Server kiểm tra group leader, relationship, file size, content type và
   storage.
6. Metadata được lưu sau khi bytes storage thành công.
7. Người có quyền xem report dùng /reports/view?id=... hoặc REST:
   - GET /api/reports/{id}
   - GET /api/reports/{id}/download
8. Không expose stored path hoặc cho phép đoán URL để bypass REPORT_VIEW.

### Flow F — Evaluator assignment, scoring và result

#### F1. Evaluator assignment

1. Faculty Head/Admin mở /faculty/registrations/evaluators.
2. Chọn approved registration trong scope.
3. Chọn active Lecturer/Faculty Head.
4. Evaluator không được là topic supervisor.
5. Save assignment; hệ thống update evaluation row, không tạo duplicate row.

#### F2. Review Board

1. Faculty Head/Admin mở /faculty/boards.
2. Chọn approved registration chưa có board.
3. Chọn 3–5 active Lecturer/Faculty Head cùng department.
4. Chọn đúng một Chair và một Secretary.
5. Không chọn Student, inactive user, cross-department user hoặc topic
   supervisor.
6. Tạo board và chuyển lifecycle theo thứ tự.
7. Board materialize một evaluation cho mỗi active member.

#### F3. Nhập score

1. Assigned evaluator/board member mở /lecturer/scoring.
2. Chỉ score evaluation được assign.
3. Nhập score 0–10, tối đa 2 decimal, và comment tùy chọn.
4. Submit trước reviewer deadline.
5. Không được chấm topic mình supervise.
6. Board PUBLISHED hoặc result đã publish thì score bị khóa.

#### F4. Publish result

1. Faculty Head/Admin mở /faculty/results.
2. Board phải COMPLETED nếu dùng Review Board.
3. Tất cả active member/evaluator phải có score hợp lệ.
4. Publish result.
5. Service tính average scale 2 bằng HALF_UP, ghi publisher/time.
6. Student mở /student/results và chỉ thấy result published của group mình.

### Flow G — Announcement

#### G1. Tạo và quản lý

1. Admin/Faculty Head mở /announcements/manage.
2. Bấm Create announcement; form mở trong popup.
3. Nhập title/content.
4. Admin chọn SCHOOL hoặc DEPARTMENT.
5. Faculty Head chỉ chọn DEPARTMENT thuộc department của mình.
6. Save tạo DRAFT.
7. Search, sort và pagination trên announcement queue dùng server-side query.
8. Edit chỉ được đổi field hợp lệ theo lifecycle.

#### G2. Publish/hide

1. Publish DRAFT hoặc HIDDEN khi nội dung sẵn sàng.
2. Hide announcement đang PUBLISHED.
3. DRAFT và HIDDEN không xuất hiện ở /announcements.
4. /announcements chỉ trả announcement PUBLISHED đúng school/department
   scope của người đọc.
5. Faculty Head không được tạo school-wide announcement.

---

## 6. Flow review và QA sau khi implement

Checklist tối thiểu cho một task:

- Route mới đã thêm vào docs/ui-route-map.md.
- Permission mới đã được seed và role bundle đã cập nhật.
- GET không mutate state.
- Mutation dùng POST/PUT/DELETE, CSRF và PRG nếu là SSR.
- Service kiểm tra role, permission, ownership, scope và status.
- Input invalid được validate server-side.
- Transaction/lock có mặt ở thao tác nhiều bước.
- Test có success, invalid và unauthorized path.
- UI action chỉ là UX; direct request vẫn bị server chặn.
- Nếu có database/schema change, cập nhật DDL và seed.
- Nếu có flow/state mới, cập nhật docs/workflows và phase summary.
- Không xóa dữ liệu nghiệp vụ có lịch sử nếu status/inactive/history phù hợp.

Lệnh verify thường dùng:

```powershell
npm run build:css
mvn '-Dtest=TenTest1,TenTest2' test
mvn package
git diff --check
```

Nếu Maven không tải được dependency do network sandbox, ghi rõ blocker và chạy
lại trong môi trường có Maven dependency cache/network. Không kết luận test pass
chỉ từ compilation.

---

## 7. Prompt mẫu để giao task cho AI

Dùng template sau và thay phần trong ngoặc vuông:

```text
Bạn đang làm trong repo HCMUTE Student Topic Management.

Context:
- Đọc PRODUCT_VISION.md, .okf/README.md, phase summary liên quan,
  docs/ui-route-map.md, docs/workflows.md và docs/authorization-matrix.md.
- Stack là Java 21 + Spring Boot MVC/Thymeleaf + Spring Security + JPA/MySQL.
- Controller mỏng; Service giữ authorization, validation, transaction.
- Không tạo AGENT_MEMORY.md; ghi implementation memory vào
  .okf/phase/<phase>/PHASE_SUMMARY.md.

Task:
[ghi rõ feature/bug, role sử dụng và route màn hình]

Acceptance criteria:
- [ghi success path]
- [ghi invalid/unauthorized path]
- [ghi scope/status/time rule]
- [ghi UI behavior nếu có]

Phạm vi:
- In scope: [...]
- Out of scope: [...]

Yêu cầu thực hiện:
1. Inspect code/test hiện tại trước khi sửa.
2. Giữ nguyên thay đổi không liên quan.
3. Cập nhật route map, seed/DDL/docs nếu contract thay đổi.
4. Viết hoặc cập nhật focused tests.
5. Chạy verification và báo rõ command/result.
6. Cập nhật phase summary sau khi verify.
```

Khi task liên quan một flow, gửi thêm các file module tương ứng. Ví dụ:

- Topic: docs/workflows.md, Topic*Controller, Topic*Service,
  TopicManagementControllerTest.
- Group: docs/workflows.md, StudentGroupService,
  StudentGroupController, FacultyStudentGroupControllerTest.
- Board/scoring: phase 006/007 summary, ReviewBoard*, EvaluationScoring*,
  Result* và tests.
- Announcement: docs/announcement-contract.md, AnnouncementService,
  AnnouncementController, Announcement*Test.
- Seed/setup: SETUP_GUIDE.md, DatabaseSeedService,
  DatabaseSeedControllerTest, database/1.ddl.sql.

## 8. Những điều không được tự suy diễn

- Không thêm role như Giáo vụ/Trưởng bộ môn khi chưa có decision.
- Không đổi scope SCHOOL/DEPARTMENT của announcement theo cảm tính.
- Không cho Faculty Head quản lý cross-department.
- Không cho Student submit thay group leader.
- Không cho supervisor chấm topic của chính mình.
- Không bỏ qua period window, deadline, status hoặc CSRF.
- Không tạo SPA/microservice/Docker nếu task không yêu cầu.
- Không reset, drop hoặc seed database có dữ liệu thật nếu chưa được xác nhận.
- Không xem file Markdown lịch sử là source of truth nếu code/test hiện tại
  đã thay đổi.



