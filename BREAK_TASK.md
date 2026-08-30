

# Parallel Development Tasks

Tài liệu này chia phần nghiệp vụ còn lại thành 4 task để 4 thành viên có thể phát triển song song. Mỗi thành viên phải chỉ sửa module và các table được giao. Phần kết nối workflow tổng thể, shared contract, migration dùng chung và integration test do owner của core workflow phụ trách.

## Quy ước chung

- Stack hiện tại: Java 21, Spring Boot, Thymeleaf, JPA, MySQL; test dùng H2 và Maven.
- Phân quyền phải được kiểm tra ở backend, không chỉ ẩn hoặc hiện nút trên UI.
- Request thay đổi dữ liệu phải có CSRF protection và validation.
- Không hard-delete dữ liệu nghiệp vụ đã có lịch sử; dùng `active` hoặc `status`.
- Không tự ý sửa `SecurityConfig`, shared DTO, shared exception, workflow service hoặc DDL chung nếu chưa trao đổi với owner core workflow.
- Mỗi task cần có unit/service/controller test cho trường hợp thành công, dữ liệu không hợp lệ, không có quyền và boundary của thời gian.
- Trong lúc phát triển độc lập, dependency của module khác được mock hoặc gọi qua read-only contract.

## Contract dùng chung

Owner core workflow cần chốt trước khi các task bắt đầu:

- ID và kiểu dữ liệu của entity.
- Danh sách enum/status và các state transition hợp lệ.
- Tên permission dùng trong backend.
- Quy ước request, response, redirect và error message.
- Table ownership và các table chỉ được đọc.
- Migration/DDL cuối cùng, đặc biệt là các field audit của `topics`.

Các module không được cập nhật table của module khác. Nếu cần dữ liệu bên ngoài module, dùng read-only repository/query contract hoặc mock trong test.

---

## Task 1 — Department & Registration Period

### Mục đích

Thiết lập khoa và các kỳ đăng ký DATN/KLTN. Đây là nơi xác định topic thuộc khoa nào và người dùng được thao tác trong khoảng thời gian nào.

### Subtask đề xuất

Task này nên được chia thành 3 subtask nhỏ. Có thể giao cho một người làm lần lượt hoặc chia cho nhiều người nếu đã thống nhất shared contract.

#### Task 1A — Department CRUD

**Mục tiêu:** Quản lý danh sách khoa.

**Phạm vi:**

- Tạo, sửa, xem và vô hiệu hóa department.
- Validate `code` và `name` không trùng.
- Không hard-delete department đang được topic sử dụng.
- Chỉ Faculty Head được thực hiện action thay đổi dữ liệu.

**Table:**

- Được insert/update: `departments`.
- Chỉ đọc: `users`, `topics`.

**AC liên quan:** AC1, AC2, AC9, AC10, AC11.

**Expected output:**

- Department entity/repository/service/controller hoặc API.
- Giao diện department nếu task dùng Thymeleaf.
- Validation và test cho CRUD, duplicate, unauthorized và deactivation.

#### Task 1B — Registration Period CRUD

**Mục tiêu:** Tạo và quản lý kỳ đăng ký DATN/KLTN.

**Phạm vi:**

- Tạo, sửa, xem registration period.
- Chọn loại `DATN` hoặc `KLTN`.
- Cấu hình lecturer/student registration window.
- Lưu người tạo period.
- Hiển thị status của period.

**Table:**

- Được insert/update: `registration_periods`.
- Chỉ đọc: `users`.

**AC liên quan:** AC3, AC4, AC7, AC8, AC10, AC11.

**Expected output:**

- Registration period entity/repository/service/controller hoặc API.
- Form tạo/sửa period.
- Test cho DATN/KLTN, created-by và status.

#### Task 1C — Timeline Rules & Permission Tests

**Mục tiêu:** Hoàn thiện các luật thời gian, chuyển trạng thái và kiểm tra quyền của Task 1A/1B.

**Phạm vi:**

- Chỉ cho nhập `reviewer_score_deadline` và `council_report_date` với KLTN.
- Từ chối timeline sai thứ tự hoặc thiếu field bắt buộc.
- Chặn state transition không hợp lệ.
- Kiểm tra permission ở backend và CSRF cho request thay đổi dữ liệu.
- Bổ sung test boundary time và unauthorized.

**Table:**

- `registration_periods`.
- `departments`.
- `users`, `user_roles`, `roles`, `permissions`, `role_permissions`.

**AC liên quan:** AC5, AC6, AC7, AC10, AC11.

**Expected output:**

- Validator hoặc domain rule dùng được bởi các module Topic và Topic Registration.
- Test matrix cho DATN/KLTN, timeline, status, permission và CSRF.
- Read-only contract để module khác kiểm tra period đang mở.

### Thứ tự làm Task 1

```text
Task 1A — Department CRUD
          ↓
Task 1B — Registration Period CRUD
          ↓
Task 1C — Timeline Rules & Permission Tests
```

Task 1A và phần UI cơ bản của Task 1B có thể làm song song. Task 1C nên làm sau khi contract của 1A/1B đã ổn định.

### Actor và quyền

- Faculty Head: tạo, sửa, xem và vô hiệu hóa department/registration period.
- Lecturer: xem period liên quan tới việc đề xuất topic.
- Student: xem period liên quan tới việc đăng ký topic.
- User không có permission phù hợp: không được gọi các action thay đổi dữ liệu.

### Phạm vi table

#### Được insert/update

- `departments`
- `registration_periods`

#### Chỉ được đọc

- `users`

#### Không được sửa

- `topics`
- `topic_supervisors`
- `student_groups`
- `group_members`
- `topic_registrations`
- `reports`
- `evaluations`

### Acceptance criteria

- [ ] AC1: Faculty Head có thể tạo, sửa, xem và vô hiệu hóa department. Tác động: `departments`.
- [ ] AC2: `code` và `name` của department không được trùng; dữ liệu rỗng hoặc sai format bị từ chối. Tác động: `departments`.
- [ ] AC3: Faculty Head có thể tạo và cập nhật period loại `DATN` hoặc `KLTN`. Tác động: `registration_periods`.
- [ ] AC4: Period lưu được thời gian đăng ký của Lecturer và Student. Tác động: `registration_periods.lecturer_registration_start/end`, `registration_periods.student_registration_start/end`.
- [ ] AC5: `reviewer_score_deadline` và `council_report_date` chỉ được nhập cho period loại `KLTN`. Tác động: `registration_periods`.
- [ ] AC6: Hệ thống từ chối period có các mốc thời gian sai thứ tự hoặc bị thiếu dữ liệu bắt buộc. Tác động: `registration_periods`.
- [ ] AC7: Period có status rõ ràng và không cho phép transition không hợp lệ. Tác động: `registration_periods.status`.
- [ ] AC8: Lưu người tạo period. Tác động: `registration_periods.created_by`, `users`.
- [ ] AC9: Department đang được topic sử dụng không được hard-delete. Tác động: `departments`, `topics`.
- [ ] AC10: Chỉ actor có permission phù hợp mới được thực hiện action thay đổi dữ liệu. Tác động: `users`, `user_roles`, `roles`, `permissions`, `role_permissions`.
- [ ] AC11: Có test cho duplicate code/name, invalid timeline, DATN/KLTN rule, unauthorized request, CSRF và boundary time. Tác động kiểm thử: `departments`, `registration_periods`.

### Expected output

- Backend service/controller và giao diện quản lý department.
- Backend service/controller và giao diện quản lý registration period.
- Validation status/time window dùng được bởi các module khác.
- Unit test và integration/controller test.

### Không làm trong task này

- Không tạo hoặc approve topic.
- Không tạo group hoặc đăng ký topic.
- Không upload report và chấm điểm.

### Handoff

Bàn giao read-only contract để module Topic và Topic Registration kiểm tra department/period tồn tại, status và thời gian đang mở.

---

## Task 2 — Topic Proposal & Review

### Mục đích

Quản lý vòng đời topic từ lúc Lecturer đề xuất đến khi Faculty Head approve, reject hoặc publish.

### Actor và quyền

- Lecturer: tạo và sửa proposal của mình trước khi được duyệt.
- Faculty Head: xem, approve, reject và publish topic.
- Student: chỉ xem topic đã publish.
- User không có permission phù hợp: không được gọi protected action.

### Phạm vi table

#### Được insert/update

- `topics`
- `topic_supervisors`

#### Chỉ được đọc

- `users`
- `user_roles`
- `roles`
- `registration_periods`
- `departments`

#### Không được sửa

- `student_groups`
- `group_members`
- `topic_registrations`
- `reports`
- `evaluations`

### Acceptance criteria

- [ ] AC1: Lecturer có thể tạo topic thuộc department và period hợp lệ, đồng thời period phải cho phép Lecturer đăng ký. Tác động: `topics`, `departments`, `registration_periods`, `users`.
- [ ] AC2: Lecturer chỉ được sửa proposal của mình trước khi topic được approve hoặc publish. Tác động: `topics`, `users`.
- [ ] AC3: Topic có từ 1 đến 2 supervisor hợp lệ. Tác động: `topic_supervisors`, `users`, `user_roles`, `roles`.
- [ ] AC4: Faculty Head có thể approve topic. Tác động: `topics`, `users`.
- [ ] AC5: Faculty Head có thể reject topic và rejection reason là bắt buộc. Tác động: `topics`, `users`.
- [ ] AC6: Chỉ topic đã approve mới được publish; state transition không hợp lệ bị từ chối. Tác động: `topics.status`.
- [ ] AC7: Chỉ topic publish mới được module Topic Registration đọc để đăng ký. Tác động: `topics`, `topic_registrations`.
- [ ] AC8: Lưu actor và timestamp của approve/reject/publish; lưu rejection reason khi bị reject. Tác động: `topics`, `users`.
- [ ] AC9: Approval history vẫn giữ nguyên khi người approve bị đổi role hoặc user role bị deactive. Tác động: `topics`, `users`, `user_roles`.
- [ ] AC10: Chỉ Faculty Head có permission review/publish; Lecturer không được tự approve topic của mình. Tác động: `users`, `user_roles`, `roles`, `permissions`, `role_permissions`, `topics`.
- [ ] AC11: Có test cho supervisor count, invalid state transition, self-approval, rejection reason, period boundary, unauthorized request và duplicate supervisor. Tác động kiểm thử: `topics`, `topic_supervisors`, `registration_periods`.

### Schema requirement

Nếu chưa có trong DDL, owner core workflow cần bổ sung các field audit vào `topics`:

- `approved_by`, `approved_at`
- `rejected_by`, `rejected_at`
- `rejection_reason`

Các field actor tham chiếu tới `users` và phải giữ lịch sử, không cascade delete.

### Expected output

- Lecturer topic proposal UI/API.
- Faculty Head review/approve/reject/publish UI/API.
- Supervisor assignment và validation.
- Read-only query trả về danh sách topic đã publish.
- Test cho permission và state transition.

### Không làm trong task này

- Không tạo student group.
- Không xử lý topic registration.
- Không upload report hoặc chấm điểm.

### Handoff

Bàn giao contract `TopicQuery` để module Topic Registration kiểm tra topic có status `PUBLISHED`, thuộc period nào và department nào.

---

## Task 3 — Student Group & Topic Registration

### Mục đích

Cho phép Student tạo nhóm, chọn leader và đăng ký một topic đã publish trong đúng period.

### Actor và quyền

- Student: tạo group, tham gia group và xem dữ liệu group của mình.
- Group leader: submit topic registration.
- Faculty Head: approve/reject registration.
- Lecturer: xem registration liên quan tới topic được phân công.

### Phạm vi table

#### Được insert/update

- `student_groups`
- `group_members`
- `topic_registrations`

#### Chỉ được đọc

- `users`
- `student_profiles`
- `topics`
- `registration_periods`

#### Không được sửa

- `departments`
- `topic_supervisors`
- `reports`
- `evaluations`

### Acceptance criteria

- [ ] AC1: Student có thể tạo group và group có đúng một leader. Tác động: `student_groups`, `group_members`, `users`.
- [ ] AC2: Group có tối đa 3 thành viên. Tác động: `group_members`.
- [ ] AC3: Một Student không được thuộc nhiều group active. Tác động: `group_members`, `student_groups`, `users`.
- [ ] AC4: Student có thể join/leave theo rule đã thống nhất; không được làm mất leader duy nhất nếu chưa chuyển leader. Tác động: `group_members`, `student_groups`.
- [ ] AC5: Chỉ leader mới được submit topic registration. Tác động: `student_groups`, `group_members`, `users`, `topic_registrations`.
- [ ] AC6: Chỉ topic có status `PUBLISHED` và thuộc period hợp lệ mới được đăng ký. Tác động: `topic_registrations`, `topics`, `registration_periods`.
- [ ] AC7: Registration chỉ được submit trong student registration window. Tác động: `topic_registrations`, `registration_periods`.
- [ ] AC8: Một group không được có nhiều current registration trong cùng một period. Tác động: `topic_registrations`, `student_groups`, `registration_periods`.
- [ ] AC9: Faculty Head có thể approve hoặc reject registration; reject bắt buộc có reason. Tác động: `topic_registrations`, `users`.
- [ ] AC10: Registration đã approve/reject không được sửa trái phép và lịch sử phải được giữ lại. Tác động: `topic_registrations`.
- [ ] AC11: Có test cho group size, duplicate membership, leader rule, duplicate registration, published-topic rule, deadline, unauthorized request và concurrency. Tác động kiểm thử: `student_groups`, `group_members`, `topic_registrations`.

### Expected output

- Student group UI/API.
- Join/leave/member/leader management.
- Topic registration UI/API cho leader.
- Faculty Head approval UI/API.
- Read-only contract để Report và Evaluation lấy approved registration.

### Không làm trong task này

- Không tạo hoặc thay đổi topic.
- Không upload report.
- Không chấm hoặc publish điểm.

### Handoff

Bàn giao contract `RegistrationQuery` để module Report/Evaluation kiểm tra group, leader, topic, period và registration status.

---

## Task 4 — Report & Evaluation

### Mục đích

Xử lý đầu ra của group sau khi topic registration được approve: nộp report, assign evaluator, chấm điểm và publish result.

### Actor và quyền

- Group leader: upload hoặc replace report của group.
- Group member: xem report của group mình.
- Supervisor/evaluator: xem report theo registration được phân công.
- Faculty Head: assign evaluator, quản lý scoring và publish result.
- Evaluator: nhập score/comment trước deadline hoặc trước khi result được publish.
- Student: chỉ xem published result của group mình.

### Phạm vi table

#### Được insert/update

- `reports`
- `evaluations`

#### Chỉ được đọc

- `topic_registrations`
- `student_groups`
- `group_members`
- `topics`
- `topic_supervisors`
- `registration_periods`
- `users`
- `user_roles`
- `roles`

#### Không được sửa

- `departments`
- `topics`
- `topic_supervisors`
- `student_groups`
- `group_members`
- `topic_registrations`

### Acceptance criteria — Report

- [ ] AC1: Chỉ leader của approved registration mới được upload report. Tác động: `reports`, `topic_registrations`, `student_groups`, `group_members`, `users`.
- [ ] AC2: Report chỉ được submit khi registration đã approve và chưa quá deadline. Tác động: `reports`, `topic_registrations`, `registration_periods`.
- [ ] AC3: Validate file type, file size, storage name, original name, content type và uploader. Tác động: `reports`, `users`.
- [ ] AC4: Resubmission phải tuân theo rule đã thống nhất; không tạo dữ liệu mồ côi nếu lưu file thất bại. Tác động: `reports`.
- [ ] AC5: Chỉ group member, supervisor, evaluator và Faculty Head có liên quan mới được xem/download report. Tác động: `reports`, `group_members`, `topic_supervisors`, `evaluations`, `users`.

### Acceptance criteria — Evaluation

- [ ] AC6: Faculty Head có thể assign evaluator cho approved registration. Tác động: `evaluations`, `topic_registrations`, `users`.
- [ ] AC7: Evaluator không được là supervisor của cùng topic. Tác động: `evaluations`, `topic_supervisors`, `users`.
- [ ] AC8: Chỉ user có Lecturer hoặc Faculty Head permission phù hợp mới được nhập score/comment. Tác động: `evaluations`, `users`, `user_roles`, `roles`, `permissions`, `role_permissions`.
- [ ] AC9: Score range, duplicate assignment, deadline và duplicate scoring được validate. Tác động: `evaluations`, `registration_periods`.
- [ ] AC10: Hệ thống tính average score từ các evaluation hợp lệ. Tác động: `evaluations.average_score`.
- [ ] AC11: Faculty Head có thể publish result; result đã publish không được sửa không có audit action. Tác động: `evaluations.status`, `evaluations.published_at`, `users`.
- [ ] AC12: Student chỉ xem được published result của group mình. Tác động: `evaluations`, `topic_registrations`, `student_groups`, `group_members`, `users`.
- [ ] AC13: Evaluation history vẫn giữ nguyên khi evaluator bị đổi role hoặc role bị deactive. Tác động: `evaluations`, `users`, `user_roles`.
- [ ] AC14: Có test cho supervisor/evaluator conflict, score boundary, deadline, publication lock, privacy, unauthorized request và role change. Tác động kiểm thử: `reports`, `evaluations`, `registration_periods`, `topic_supervisors`.

### Schema requirement

Nếu chỉ cho phép một report hiện tại trên mỗi registration, phải enforce rule ở service và database. Nếu cần versioning report, bổ sung `version`/`is_latest` hoặc tạo table `report_versions` sau khi thống nhất với owner core workflow.

### Expected output

- Report upload/download với storage an toàn.
- Phân quyền xem report.
- Evaluator assignment.
- Scoring và average calculation.
- Result publication và student result view.
- Test cho permission, deadline, privacy và publication lock.

### Không làm trong task này

- Không thay đổi topic registration.
- Không thay đổi danh sách supervisor.
- Không thay đổi department hoặc registration period.

### Handoff

Bàn giao contract `ReportQuery` và `EvaluationQuery` cho core workflow để kiểm tra report/result status và tích hợp flow publish cuối cùng.

---

## Quy trình làm việc và bàn giao

### Branch

Mỗi thành viên dùng một branch riêng:

```text
codex/feature/department-period
codex/feature/topic-review
codex/feature/group-registration
codex/feature/report-evaluation
```

### Quy tắc PR

- Một PR chỉ xử lý một task.
- Không sửa file hoặc table ngoài ownership nếu không có lý do rõ ràng trong PR.
- PR phải liệt kê table đã tác động, migration đã dùng và test đã chạy.
- PR phải mô tả dependency nào đã mock.
- Không merge khi `mvn test` fail.
- Owner core workflow review contract và integration point trước khi merge.

### Thứ tự tích hợp

Code có thể làm song song, nhưng khi tích hợp nên theo thứ tự:

```text
Department & Period
        ↓
Topic Proposal & Review
        ↓
Student Group & Registration
        ↓
Report & Evaluation
```

Các dependency runtime vẫn tồn tại; mục tiêu của cách chia này là tránh conflict code và cho phép từng thành viên hoàn thành module bằng mock/contract độc lập.

---

## Breakdown ticket cho Task 2 — Topic Proposal & Review

### [002_001] Topic Proposal CRUD

**Mục tiêu:** Cho phép Lecturer tạo, xem và sửa proposal của chính mình trước khi được duyệt.

**Table:**

- Được insert/update: `topics`.
- Chỉ đọc: `users`, `registration_periods`, `departments`.

**Acceptance criteria:**

- [ ] Lecturer tạo được topic thuộc department và period hợp lệ.
- [ ] Chỉ tạo topic trong Lecturer registration window.
- [ ] Lecturer chỉ sửa được proposal của mình khi topic còn ở trạng thái cho phép sửa.
- [ ] Title/description/department/period được validate.
- [ ] User không có permission không được tạo hoặc sửa topic.
- [ ] Có test success, invalid data, unauthorized và period boundary.

**Expected output:** Topic form/API, service/repository, validation, test và read-only query cho proposal.

### [002_002] Topic Supervisor Assignment

**Mục tiêu:** Quản lý danh sách supervisor của topic.

**Table:**

- Được insert/update: `topic_supervisors`.
- Chỉ đọc: `topics`, `users`, `user_roles`, `roles`.

**Acceptance criteria:**

- [ ] Topic có từ 1 đến 2 supervisor.
- [ ] Supervisor phải là user có role/permission Lecturer hợp lệ.
- [ ] Không được thêm trùng supervisor.
- [ ] Không được tự động vượt quá giới hạn 2 supervisor.
- [ ] Chỉ actor được phép mới được gán hoặc thay đổi supervisor.
- [ ] Có test supervisor count, duplicate, invalid user và unauthorized.

**Expected output:** Supervisor assignment UI/API, validation và test.

### [002_003] Topic Review Approve & Reject

**Mục tiêu:** Cho phép Faculty Head review proposal và ghi nhận quyết định approve/reject.

**Table:**

- Được insert/update: `topics`.
- Chỉ đọc: `users`, `topic_supervisors`.

**Acceptance criteria:**

- [ ] Faculty Head xem được các proposal cần review.
- [ ] Faculty Head approve được topic ở trạng thái hợp lệ.
- [ ] Faculty Head reject được topic và rejection reason là bắt buộc.
- [ ] Lecturer không được approve topic của chính mình.
- [ ] Transition không hợp lệ bị từ chối.
- [ ] Lưu actor và timestamp approve/reject.
- [ ] Approval history vẫn tồn tại khi actor bị đổi role hoặc deactive.
- [ ] Có test permission, self-approval, invalid transition và rejection reason.

**Schema impact:** Nếu chưa có, bổ sung `approved_by`, `approved_at`, `rejected_by`, `rejected_at`, `rejection_reason` vào `topics`.

**Expected output:** Review UI/API, state transition, audit fields và test.

### [002_004] Topic Publication & Published Query

**Mục tiêu:** Publish topic đã được approve và cung cấp danh sách topic mà Student có thể đăng ký.

**Table:**

- Được insert/update: `topics`.
- Chỉ đọc: `registration_periods`, `departments`, `topic_registrations`.

**Acceptance criteria:**

- [ ] Chỉ topic đã approve mới được publish.
- [ ] Topic đã publish không được sửa proposal trái phép.
- [ ] Chỉ topic publish và thuộc period hợp lệ mới xuất hiện trong published query.
- [ ] Student chỉ thấy topic được phép đăng ký, không thấy draft/rejected topic.
- [ ] Faculty Head là actor duy nhất được publish theo permission policy.
- [ ] Có test publish transition, visibility, period filter và unauthorized.

**Expected output:** Publish action, published-topic query/contract cho Task 3 và test.

---

## Breakdown ticket cho Task 3 — Student Group & Topic Registration

### [003_001] Student Group Creation & Membership

**Mục tiêu:** Cho phép Student tạo group và quản lý thành viên cơ bản.

**Table:**

- Được insert/update: `student_groups`, `group_members`.
- Chỉ đọc: `users`, `student_profiles`.

**Acceptance criteria:**

- [ ] Student tạo được group và group có creator hợp lệ.
- [ ] Student có thể join/leave group theo rule.
- [ ] Group không có quá 3 thành viên.
- [ ] Không thêm trùng cùng một student vào group.
- [ ] Student chỉ xem được group mà mình có liên quan.
- [ ] Có test create, join, leave, group-size, duplicate-member và unauthorized.

**Expected output:** Group/member UI/API, service/repository, validation và test.

### [003_002] Group Leader & Membership Rules

**Mục tiêu:** Đảm bảo mỗi group có đúng một leader và xử lý chuyển leader an toàn.

**Table:**

- Được insert/update: `student_groups`, `group_members`.
- Chỉ đọc: `users`, `student_profiles`.

**Acceptance criteria:**

- [ ] Group có đúng một leader đang là member của group.
- [ ] Có thể chuyển leader cho member hợp lệ.
- [ ] Không được leave hoặc remove leader cuối cùng khi chưa chuyển leader.
- [ ] Student không được thuộc nhiều group active.
- [ ] Rule được kiểm tra trong transaction để tránh race condition.
- [ ] Có test leader assignment, leader transfer, last-leader, multi-group và concurrency.

**Expected output:** Leader management, membership rule service và test.

### [003_003] Topic Registration Submission

**Mục tiêu:** Cho phép leader đăng ký một topic đã publish cho group.

**Table:**

- Được insert/update: `topic_registrations`.
- Chỉ đọc: `student_groups`, `group_members`, `topics`, `registration_periods`, `users`.

**Acceptance criteria:**

- [ ] Chỉ leader mới được submit registration.
- [ ] Group phải active và topic phải ở trạng thái `PUBLISHED`.
- [ ] Topic phải thuộc period được chọn.
- [ ] Chỉ submit trong Student registration window.
- [ ] Một group không có nhiều current registration trong cùng period.
- [ ] Lưu submitter và submitted time.
- [ ] Có test leader permission, published rule, period mismatch, duplicate và deadline.

**Expected output:** Registration form/API, validation, service/repository và test.

### [003_004] Topic Registration Review

**Mục tiêu:** Cho phép Faculty Head approve/reject topic registration.

**Table:**

- Được insert/update: `topic_registrations`.
- Chỉ đọc: `student_groups`, `group_members`, `topics`, `registration_periods`, `users`.

**Acceptance criteria:**

- [ ] Faculty Head xem được registration cần review.
- [ ] Faculty Head approve được registration hợp lệ.
- [ ] Faculty Head reject được registration và rejection reason là bắt buộc.
- [ ] Registration đã approve/reject không được sửa trái phép.
- [ ] Registration history được giữ lại.
- [ ] Report/Evaluation có read-only query lấy được approved registration.
- [ ] Có test permission, invalid transition, rejection reason và history.

**Expected output:** Review UI/API, state transition, rejection handling, query contract và test.

---

## Breakdown ticket cho Task 4 — Report & Evaluation

### [004_001] Report Upload & Metadata

**Mục tiêu:** Cho phép leader upload report cho approved registration và lưu metadata.

**Table:**

- Được insert/update: `reports`.
- Chỉ đọc: `topic_registrations`, `student_groups`, `group_members`, `registration_periods`, `users`.

**Acceptance criteria:**

- [ ] Chỉ leader của approved registration được upload.
- [ ] Validate file type và file size.
- [ ] Lưu original name, stored name, content type, file size, uploader và submitted time.
- [ ] Không lưu database record nếu lưu file thất bại.
- [ ] File storage không dùng tên file người dùng cung cấp làm tên vật lý trực tiếp.
- [ ] Có test permission, invalid file, metadata và storage failure.

**Expected output:** Upload endpoint/UI, storage service, report metadata persistence và test.

### [004_002] Report Access, Deadline & Resubmission

**Mục tiêu:** Quản lý quyền xem/download report, deadline và hành vi nộp lại.

**Table:**

- Được insert/update: `reports`.
- Chỉ đọc: `topic_registrations`, `student_groups`, `group_members`, `topic_supervisors`, `evaluations`, `registration_periods`, `users`.

**Acceptance criteria:**

- [ ] Group member chỉ xem report của group mình.
- [ ] Supervisor, evaluator và Faculty Head chỉ xem report có liên quan.
- [ ] User không liên quan không được download report.
- [ ] Report quá deadline bị từ chối theo policy.
- [ ] Resubmission policy được implement rõ ràng và không tạo dữ liệu mồ côi.
- [ ] Có test privacy, deadline, download và resubmission.

**Expected output:** Access/download authorization, deadline rule, resubmission behavior và test.

### [004_003] Evaluator Assignment

**Mục tiêu:** Faculty Head assign evaluator cho approved registration.

**Table:**

- Được insert/update: `evaluations`.
- Chỉ đọc: `topic_registrations`, `topics`, `topic_supervisors`, `users`, `user_roles`, `roles`.

**Acceptance criteria:**

- [ ] Faculty Head assign được evaluator hợp lệ.
- [ ] Evaluator phải có Lecturer hoặc Faculty Head permission phù hợp.
- [ ] Evaluator không được là supervisor của cùng topic.
- [ ] Không tạo duplicate assignment.
- [ ] Chỉ assign cho approved registration.
- [ ] Có test role validation, supervisor conflict, duplicate và unauthorized.

**Expected output:** Evaluator assignment UI/API, validation, service/repository và test.

### [004_004] Scoring & Average Calculation

**Mục tiêu:** Cho phép evaluator nhập điểm/nhận xét và tính điểm trung bình.

**Table:**

- Được insert/update: `evaluations`.
- Chỉ đọc: `topic_registrations`, `registration_periods`, `users`.

**Acceptance criteria:**

- [ ] Chỉ evaluator được assign mới được nhập score/comment.
- [ ] Score nằm trong range hợp lệ.
- [ ] Không được nhập điểm sau deadline hoặc sau khi result đã publish.
- [ ] Không được duplicate scoring cho cùng assignment.
- [ ] Average score được tính từ các evaluation hợp lệ.
- [ ] Có test score boundary, deadline, duplicate và average calculation.

**Expected output:** Scoring UI/API, average calculation service và test.

### [004_005] Result Publication & Student View

**Mục tiêu:** Faculty Head publish result và cho Student xem đúng kết quả của mình.

**Table:**

- Được insert/update: `evaluations`.
- Chỉ đọc: `topic_registrations`, `student_groups`, `group_members`, `users`.

**Acceptance criteria:**

- [ ] Faculty Head publish được result đã đủ dữ liệu.
- [ ] Result đã publish không được sửa nếu không có audited action.
- [ ] Student chỉ xem được published result của group mình.
- [ ] Student không xem được draft/unpublished result của group khác.
- [ ] Evaluation history vẫn giữ nguyên khi evaluator bị đổi role hoặc deactive.
- [ ] Có test publication lock, privacy, missing-data và role change.

**Expected output:** Result publication action, student result view, privacy rule, audit-friendly status và test.

---

## Tổng hợp ticket cần tạo trên GitHub Project

- Task 1: `[001_001]`, `[001_002]`, `[001_003]` — đã tạo.
- Task 2: `[002_001]` đến `[002_004]` — 4 ticket.
- Task 3: `[003_001]` đến `[003_004]` — 4 ticket.
- Task 4: `[004_001]` đến `[004_005]` — 5 ticket.

Tổng cộng cần tạo thêm **13 ticket**. Tất cả ticket mới đặt ở cột `Backlog`, dùng issue thật trong repository `long29103107/hcmute-student-topic-management`.
