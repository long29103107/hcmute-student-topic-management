# Domain model

## Roles and permissions

- `RoleEntity` là access bundle được gán cho tài khoản.
- `PermissionEntity` là quyền thao tác nhỏ nhất, có `code`, tên hiển thị và
  nhóm chức năng để giao diện gom theo từng section.
- Một `UserEntity` có thể nhận nhiều role thông qua `UserRoleEntity`.
- Một role có nhiều permission thông qua `RolePermissionEntity`; một permission
  có thể dùng lại cho nhiều role.
- Các role hệ thống mặc định vẫn là:
  - `ADMIN`: quản lý tài khoản và thông báo.
  - `FACULTY_HEAD`: quản lý đợt, duyệt/công bố, phân công, hội đồng và kết quả.
  - `LECTURER`: đề xuất đề tài, hướng dẫn, phản biện và chấm theo phân công.
  - `STUDENT`: tạo/tham gia nhóm, đăng ký và xem kết quả của nhóm mình.
- `GROUP_LEADER`: một sinh viên giữ vai trò đại diện trong đúng một nhóm; đây
  là thuộc tính thành viên, không nhất thiết là system role riêng.

## Core aggregates

### Course-project MVP model

For the môn học đồ án, the default model is intentionally small:
`User`, `Department`, `RegistrationPeriod`, `Topic`, `StudentGroup`,
`TopicRegistration`, `Report` and `Evaluation`. `RoleEntity` và
`PermissionEntity`, `UserRoleEntity` và `RolePermissionEntity` là nhóm thực
thể hỗ trợ authentication/authorization. The detailed aggregates below
describe extension points, not a requirement to create every table in the
first implementation.

The MVP can keep assigned lecturer, score, comment, average and publication
status inside `Evaluation`. `ReviewBoard`, separate reviewer assignments,
component-score records and final-result aggregates are Should Have unless the
rubric explicitly requires them.

### User and Department

`User` có tài khoản đăng nhập, họ tên, email/mã số, role, trạng thái active và
password hash. `User` tham chiếu một `RoleEntity`; role chứa các
`PermissionEntity` được phép thực hiện. Quan hệ gán role và permission được
lưu qua `UserRoleEntity` và `RolePermissionEntity`. `Department` là bộ môn;
mỗi `Topic` tham chiếu đúng một bộ môn.

### RegistrationPeriod

Thuộc tính tối thiểu:

- id, name;
- type: `COURSE`, `RESEARCH`, `SPECIALIZED_ESSAY`, `GRADUATION_THESIS`;
- lecturer registration start/end;
- student registration start/end;
- reviewer score deadline, nullable và chỉ hợp lệ với TLCN/KLTN;
- council report date, nullable và chỉ hợp lệ với KLTN;
- status/created metadata theo nhu cầu triển khai.

Không dùng một khoảng thời gian chung thay cho hai giai đoạn đăng ký.

### Topic and supervisors

`Topic` thuộc một `RegistrationPeriod` và một `Department`, có title,
description/content và trạng thái đề xuất. `TopicSupervisor` liên kết topic
với lecturer và có thứ tự/primary marker nếu giao diện cần phân biệt. Service
phải giới hạn từ 1 đến 2 GVHD.

Trạng thái kỹ thuật đề xuất: `DRAFT`, `PENDING_APPROVAL`, `REJECTED`,
`APPROVED`, `PUBLISHED`. Chỉ `PUBLISHED` mới được nhóm chọn.

### StudentGroup and members

`StudentGroup` thuộc ngữ cảnh thực hiện đề tài, có tên và trạng thái. `GroupMember`
liên kết group với student, có `isLeader` và thời điểm tham gia.

Invariants bắt buộc:

- tối đa 3 thành viên/group;
- đúng 1 leader/group;
- một student không ở quá 1 group trong quá trình thực hiện đề tài;
- chỉ leader gửi đăng ký topic và nộp report.

Cách mời/xác nhận thành viên chưa được chốt; xem `open-questions.md`.

### TopicRegistration

Liên kết một group với một topic trong một period, có người gửi, thời điểm,
trạng thái duyệt và lý do từ chối (nếu có). Trạng thái đề xuất:
`PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`.

Bắt buộc chặn group gửi hơn một đăng ký trong cùng ngữ cảnh và chặn người
không phải leader gửi yêu cầu.

### Report

Report liên kết với registration/group/topic, lưu metadata file, uploader và
submitted time. Chỉ leader của registration đã được chấp thuận được nộp.
Thành viên group, GVHD và người chấm được xem/tải theo quyền.

Hạn nộp báo cáo, loại file và dung lượng tối đa chưa được xác nhận.

### ReviewBoard and assignments

This is the extended Should Have model. It is not required for the default MVP;
the MVP may store the assigned lecturer directly on `Evaluation`.

`ReviewBoard` có tên, period/topic scope nếu cần và trạng thái. `ReviewBoardMember`
liên kết lecturer với board và có role `CHAIR`, `SECRETARY`, `MEMBER`.
`BoardTopicAssignment` liên kết board với topic.

Invariants bắt buộc:

- 3–5 lecturer/board;
- đúng 1 chair và đúng 1 secretary, đều thuộc board;
- topic được phân công cho board trước khi nhập điểm;
- GVHD không được chấm chính topic mình hướng dẫn.

`ReviewerAssignment` có thể biểu diễn GVPB và các lecturer chấm được giao;
không giả định GVPB luôn là một role hệ thống riêng.

### Scores and final results

`Score` liên kết topic/assignment/lecturer, có điểm thành phần, nhận xét,
thời điểm nhập và trạng thái. `FinalResult` lưu điểm trung bình, kết luận,
người tổng hợp/chủ tịch và trạng thái công bố.

Điểm cuối là trung bình cộng các điểm thành phần theo `REQUEST.md`. Thang điểm,
thành phần, trọng số và làm tròn chưa được chốt; không ghi cứng trong code.

### Announcement

Announcement có title, content, scope trường/khoa, người đăng, thời gian,
trạng thái draft/hidden/published và thời gian công bố. Người dùng chỉ xem
announcement đã published theo phạm vi được phép.

## Relationship sketch

```text
Department 1 ── * Topic * ── * Lecturer (TopicSupervisor)
RegistrationPeriod 1 ── * Topic
RegistrationPeriod 1 ── * TopicRegistration
Student 1 ── * GroupMember * ── 1 StudentGroup
StudentGroup 1 ── * TopicRegistration * ── 1 Topic
TopicRegistration 1 ── * Report
ReviewBoard 1 ── * ReviewBoardMember * ── 1 Lecturer
ReviewBoard 1 ── * BoardTopicAssignment * ── 1 Topic
Topic/Assignment 1 ── * Score
Topic 1 ── 0..1 FinalResult
```

Các quan hệ `*` và `0..1` trên sơ đồ không thay thế các business rule ở
`REQUEST.md`; đặc biệt quan hệ nhiều nhóm trên một topic vẫn là open question.
