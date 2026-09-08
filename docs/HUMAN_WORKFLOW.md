# Human Workflow — HCMUTE Student Topic Management

Tài liệu này dành cho người chạy thử hệ thống bằng trình duyệt. Mục tiêu là đi
từ database seed, chuẩn bị tài khoản, tạo đề tài, đăng ký nhóm, lập hội đồng,
chấm điểm, công bố kết quả và kiểm tra thông báo.

Đây là hướng dẫn thao tác thủ công, không phải tài liệu cho AI. Các URL bên
dưới là route của ứng dụng local, mặc định bắt đầu bằng `http://localhost:8080`.

## 1. Chuẩn bị môi trường

1. Làm theo [SETUP_GUIDE.md](../SETUP_GUIDE.md) để chạy MySQL, DDL và ứng dụng.
2. Mở `http://localhost:8080/seed` bằng trình duyệt.
3. Bấm seed theo thứ tự trên màn hình, hoặc gọi seed API theo setup guide.
4. Chỉ seed trên database local có thể reset. Seed sẽ truncate dữ liệu nghiệp vụ.
5. Sau khi seed, mở `http://localhost:8080/login`.

Seed tạo sẵn một kỳ đăng ký đang `OPEN`, bốn nhóm sinh viên, tám đề tài, ba
đăng ký đã `APPROVED`, hai hội đồng, ba thông báo và một kết quả đã publish.
Các record có sẵn dùng để kiểm tra nhanh; phần sau có thêm đường chạy tạo dữ
liệu mới để kiểm thử đầy đủ từ đầu đến cuối.

## 2. Tài khoản dùng trong kịch bản

### 2.1 Tài khoản Admin ban đầu

| Mục | Giá trị |
|---|---|
| Email | `admin@hcmute.edu.vn` |
| Password | `admin123` |
| Vai trò | `ADMIN` |
| Phạm vi | Toàn hệ thống |

### 2.2 Tài khoản nghiệp vụ

Các tài khoản dưới đây có sẵn sau seed. Sau khi đăng nhập Admin, hãy đặt
password `admin123` cho các tài khoản cần dùng. Toàn bộ kịch bản trong tài liệu
này dùng thống nhất password `admin123` để đổi account nhanh.

| Vai trò | Email đăng nhập | Phạm vi / mục đích |
|---|---|---|
| Faculty Head | `nguyen.van.khang@lecturer.hcmute.edu.vn` | CNTT; duyệt đề tài, đăng ký, hội đồng, kết quả, thông báo khoa |
| Faculty Head | `le.quang.huy@lecturer.hcmute.edu.vn` | CNPM; quản lý board Atlas và scoring |
| Lecturer | `nguyen.thanh.binh@lecturer.hcmute.edu.vn` | CNTT; tạo đề tài |
| Lecturer | `vo.hoang.nam@lecturer.hcmute.edu.vn` | CNTT; supervisor/evaluator |
| Lecturer | `bui.thanh.ha@lecturer.hcmute.edu.vn` | CNTT; thành viên hội đồng |
| Lecturer | `phan.tuan.anh@lecturer.hcmute.edu.vn` | CNPM; Chair của board Atlas |
| Lecturer | `huynh.thi.my.linh@lecturer.hcmute.edu.vn` | CNPM; Secretary của board Atlas |
| Student | `24110000@student.hcmute.edu.vn` | Leader nhóm Phoenix; kiểm tra student flow |
| Student | `24110001@student.hcmute.edu.vn` | Thành viên nhóm Phoenix |
| Student | `24110006@student.hcmute.edu.vn` | Tài khoản leader trống để tạo flow mới |
| Student | `24110007@student.hcmute.edu.vn` | Thành viên cho flow mới |
| Student | `24110008@student.hcmute.edu.vn` | Thành viên cho flow mới |

### 2.3 Đặt password `admin123` cho account test

Thực hiện một lần sau mỗi lần reset seed:

1. Đăng nhập bằng Admin.
2. Vào `/admin/lecturers`.
3. Search từng Faculty Head/Lecturer ở bảng account.
4. Bấm action password/reset password, nhập `admin123` hai lần và lưu.
5. Vào `/admin/students`.
6. Search `24110000`, `24110001`, `24110006`, `24110007`, `24110008`.
7. Đặt cùng password `admin123` và lưu.
8. Sau bước này, tất cả account trong kịch bản đều đăng nhập bằng password
   `admin123`.

Mỗi lần đổi vai trò, luôn bấm Logout rồi đăng nhập lại. Không mở nhiều account
trong cùng một session/cửa sổ nếu chưa chắc cookie của browser đã được tách.

## 3. Flow tổng quát

```text
Admin seed + chuẩn bị account
        ↓
Faculty Head mở kỳ đăng ký
        ↓
Lecturer tạo đề tài → Submit
        ↓
Faculty Head duyệt → gán supervisor → publish đề tài
        ↓
Student tạo nhóm → các student khác join
        ↓
Leader đăng ký đề tài → Faculty Head approve
        ↓
Faculty Head tạo board → assign 3–5 lecturer → ACTIVE
        ↓
Board members đăng nhập lần lượt và submit score
        ↓
Faculty Head COMPLETED → Faculty Results publish
        ↓
Student xem kết quả của nhóm mình
```

Trạng thái cần quan sát:

| Đối tượng | Trạng thái chính |
|---|---|
| Registration period | `DRAFT → OPEN → CLOSED → ARCHIVED` |
| Topic | `DRAFT → PENDING_APPROVAL → APPROVED → PUBLISHED` |
| Topic registration | `PENDING → APPROVED` hoặc `REJECTED` |
| Review board | `DRAFT → ASSIGNED/SCHEDULED → ACTIVE → COMPLETED → PUBLISHED` |
| Evaluation | `DRAFT → SUBMITTED` |
| Result | `DRAFT/READY → PUBLISHED` |
| Announcement | `DRAFT → PUBLISHED` hoặc `PUBLISHED → HIDDEN` |

## 4. Kịch bản chạy đầy đủ từ đầu đến cuối

### Bước 0 — Seed và xác nhận dữ liệu

**Đăng nhập:** chưa cần login để mở `/seed`; sau seed dùng Admin.

1. Seed database.
2. Đăng nhập Admin.
3. Mở lần lượt `/admin/departments`, `/admin/lecturers`,
   `/admin/students` để kiểm tra account và department.
4. Đặt password `admin123` cho các account ở mục 2.3.
5. Logout Admin.

**Kết quả mong đợi:** database có dữ liệu fixture; Admin vào được dashboard và
các trang administration; account chưa được đặt password không thể login.

### Bước 1 — Kiểm tra kỳ đăng ký

**Đăng nhập:** Faculty Head CNTT
`nguyen.van.khang@lecturer.hcmute.edu.vn` / `admin123`.

1. Mở `/faculty/periods`.
2. Tìm `Đợt đăng ký đề tài học kỳ 1 năm học 2026-2027`.
3. Kiểm tra status `OPEN`, lecturer window và student window bao phủ thời điểm
   hiện tại.
4. Không cần tạo kỳ mới cho happy path vì seed đã tạo kỳ này.
5. Nếu muốn test form riêng, tạo một period test với tên khác, các mốc bắt đầu
   trước hiện tại và kết thúc sau hiện tại, status `OPEN`; không sửa period seed
   khi đang dùng các fixture còn lại.

**Kết quả mong đợi:** Lecturer thấy period trong form tạo topic; Student thấy
period trong form tạo group/đăng ký topic.

### Bước 2 — Lecturer tạo và submit đề tài

**Đăng nhập:** Lecturer CNTT
`nguyen.thanh.binh@lecturer.hcmute.edu.vn` / `admin123`.

1. Mở `/lecturer/topics`.
2. Bấm `Add topic proposal`.
3. Nhập một đề tài mới, ví dụ:
   - Title: `Manual E2E Topic 2026`
   - Description: `Topic created for the complete human workflow test.`
   - Department: `CNTT`
   - Registration period: kỳ seed đang `OPEN`
4. Lưu proposal.
5. Kiểm tra proposal xuất hiện ở `My topic proposals` với status `DRAFT`.
6. Bấm action submit/review.
7. Refresh trang và xác nhận status thành `PENDING_APPROVAL`.
8. Logout.

**Kết quả mong đợi:** Lecturer chỉ sửa được proposal của mình khi proposal còn
editable; sau submit, proposal chờ Faculty Head review.

### Bước 3 — Faculty Head review đề tài

**Đăng nhập:** Faculty Head CNTT `nguyen.van.khang@lecturer.hcmute.edu.vn`.

1. Mở `/faculty/topics/review`.
2. Tìm `Manual E2E Topic 2026`.
3. Mở chi tiết để kiểm tra proposer, department và period.
4. Bấm approve.
5. Xác nhận status thành `APPROVED`.
6. Mở `/faculty/topics/supervisors`.
7. Tìm topic vừa duyệt.
8. Chọn 1–2 supervisor thuộc CNTT, ví dụ `Võ Hoàng Nam` và `Đặng Minh Trí`.
9. Lưu assignment.

Không chọn supervisor ngoài department hoặc account inactive. Supervisor của
topic này sẽ không được chọn làm thành viên review board cho chính topic đó.

**Kết quả mong đợi:** Topic được duyệt và có supervisor; topic chưa public nên
Student chưa thể đăng ký topic này.

### Bước 4 — Faculty Head publish đề tài

**Vẫn dùng:** Faculty Head CNTT.

1. Mở `/faculty/topics/publish`.
2. Tìm topic `Manual E2E Topic 2026` ở danh sách approved.
3. Bấm publish và xác nhận.
4. Mở `/topics` để kiểm tra topic xuất hiện trong catalog public.
5. Logout.

**Kết quả mong đợi:** status chuyển `APPROVED → PUBLISHED`; topic hiện cho
Student trong thời gian student window đang mở.

### Bước 5 — Student tạo group và join member

**Đăng nhập:** Student `24110006@student.hcmute.edu.vn` / `admin123`.

1. Mở `/student/groups`.
2. Bấm tạo group.
3. Nhập tên duy nhất, ví dụ `Nhóm Manual E2E` và chọn period seed.
4. Lưu. Student hiện tại trở thành leader.
5. Logout.

**Đăng nhập:** Student `24110007@student.hcmute.edu.vn`.

1. Mở `/student/groups`.
2. Tìm `Nhóm Manual E2E`.
3. Bấm join.
4. Logout.

**Đăng nhập:** Student `24110008@student.hcmute.edu.vn`.

1. Mở `/student/groups`.
2. Join `Nhóm Manual E2E`.
3. Logout.

**Kiểm tra:** đăng nhập lại bằng `24110006`, mở `/student/groups` và xác nhận
group có 3 thành viên, đúng leader. Có thể mở `/faculty/groups` bằng Faculty
Head để kiểm tra group nằm trong scope CNTT.

### Bước 6 — Leader đăng ký topic

**Đăng nhập:** Student leader `24110006@student.hcmute.edu.vn`.

1. Mở `/student/groups/register-topic`.
2. Chọn `Nhóm Manual E2E`.
3. Chọn `Manual E2E Topic 2026` đang `PUBLISHED`.
4. Submit registration.
5. Mở `/student/registrations`.
6. Xác nhận registration có status `PENDING`.
7. Logout.

Chỉ leader được submit registration. Topic chưa publish, period đã hết hạn,
group không thuộc period hoặc member không cùng department đều phải bị server
từ chối.

### Bước 7 — Faculty Head duyệt topic registration

**Đăng nhập:** Faculty Head CNTT.

1. Mở `/faculty/registrations/review`.
2. Tìm `Nhóm Manual E2E` và topic tương ứng.
3. Kiểm tra group leader, member count, topic và period.
4. Bấm approve.
5. Xác nhận status `PENDING → APPROVED`.
6. Logout.

Nếu muốn test nhánh reject, ở bước này chọn reject và nhập reason; khi đó leader
thấy `REJECTED`, không thể tạo board cho registration đó. Muốn chạy tiếp happy
path thì seed lại hoặc tạo registration mới.

### Bước 8 — Tạo và kích hoạt review board

**Đăng nhập:** Faculty Head CNTT.

1. Mở `/faculty/boards`.
2. Bấm `Create board`.
3. Chọn registration `Nhóm Manual E2E` đã `APPROVED`.
4. Chọn đúng 3 lecturer-capability account cùng department CNTT. Với topic ở
   bước trên, không chọn hai supervisor `Võ Hoàng Nam`/`Đặng Minh Trí`. Có thể
   chọn:
   - Chair: `Nguyễn Văn Khang`
   - Secretary: `Nguyễn Thanh Bình`
   - Member: `Bùi Thanh Hà`
5. Chọn lịch review hợp lệ và lưu board.
6. Xác nhận board có đúng 3 member, đúng một Chair và một Secretary.
7. Chuyển status lần lượt `ASSIGNED` hoặc `SCHEDULED`, sau đó `ACTIVE`.
8. Logout.

Server phải chặn board có dưới 3 hoặc trên 5 member, trùng member, thiếu Chair/
Secretary, member khác department, hoặc supervisor ngồi vào board.

### Bước 9 — Board members chấm điểm

Lặp lại các thao tác sau với từng account board member. Mỗi lần làm xong phải
Logout để đổi account.

1. Đăng nhập account member.
2. Mở `/lecturer/scoring`.
3. Tìm evaluation của `Nhóm Manual E2E`.
4. Nhập điểm hợp lệ trong range của form, ví dụ `8.50`, `9.00`, `8.00`.
5. Submit/save score.
6. Kiểm tra evaluation chuyển `SUBMITTED`.
7. Logout.

Chạy với:

- `nguyen.van.khang@lecturer.hcmute.edu.vn` — Chair — `8.50`.
- `nguyen.thanh.binh@lecturer.hcmute.edu.vn` — Secretary — `9.00`.
- `bui.thanh.ha@lecturer.hcmute.edu.vn` — Member — `8.00`.

Không thể sửa score sau khi board đã `COMPLETED` hoặc result đã `PUBLISHED`.
Không thể submit điểm ngoài range, sai precision hoặc thay score của evaluator
khác.

### Bước 10 — Complete board và publish result

**Đăng nhập:** Faculty Head CNTT.

1. Mở `/faculty/boards`.
2. Tìm board `Nhóm Manual E2E`.
3. Kiểm tra cả 3 evaluation đều đã submitted.
4. Chuyển board `ACTIVE → COMPLETED`.
5. Mở `/faculty/results`.
6. Kiểm tra result của group đã đủ evaluation và có average. Với điểm mẫu,
   average kỳ vọng là `8.50`.
7. Bấm publish.
8. Xác nhận result thành `PUBLISHED`; board cũng chuyển sang `PUBLISHED`.
9. Logout.

Nếu còn một evaluation chưa submit, server phải từ chối publish. Nếu board
chưa `COMPLETED`, server cũng phải từ chối publish.

### Bước 11 — Student xem kết quả và kiểm tra quyền riêng tư

**Đăng nhập:** từng Student của `Nhóm Manual E2E`.

1. Mở `/student/results`.
2. Xác nhận chỉ thấy result của group mình.
3. Mở `/reports/view` hoặc link report nếu group đã có report.
4. Logout.

**Negative check:** đăng nhập một student ngoài group, ví dụ
`24110001@student.hcmute.edu.vn`, và xác nhận không thấy result của `Nhóm Manual
E2E`. Faculty Head chỉ thấy result thuộc department; Admin thấy toàn hệ thống.

### Bước 12 — Tạo và publish announcement

#### Thông báo trong khoa

**Đăng nhập:** Faculty Head CNTT.

1. Mở `/announcements/manage`.
2. Bấm `Create announcement` để mở popup.
3. Nhập title/content, chọn scope department CNTT, lưu draft.
4. Dùng search, sort và pagination để tìm draft vừa tạo.
5. Mở edit popup, sửa nội dung và lưu.
6. Bấm publish.
7. Mở `/announcements` để kiểm tra thông báo đã public.
8. Logout.

Faculty Head được tạo thông báo trong department của mình. Không dùng Faculty
Head để tạo school-wide announcement.

#### Thông báo toàn trường

**Đăng nhập:** Admin.

1. Mở `/announcements/manage`.
2. Tạo announcement với scope school-wide.
3. Lưu draft, kiểm tra draft không xuất hiện với người dùng public.
4. Publish.
5. Logout và đăng nhập một Student.
6. Mở `/announcements`, xác nhận Student thấy school-wide announcement.

Admin có thể xem, sửa, publish và hide announcement trong toàn hệ thống. Khi
hide một thông báo đã publish, thông báo vẫn còn trong queue quản trị nhưng
không còn ở trang public.

## 5. Các flow có sẵn sau seed để kiểm tra nhanh

Không cần tạo dữ liệu mới nếu chỉ muốn smoke test:

| Dữ liệu | Cách kiểm tra |
|---|---|
| `Nhóm Phoenix` | Faculty Head mở `/faculty/groups`; Student leader `24110000` mở `/student/groups` |
| `Nhóm Nova` | Dùng để kiểm tra group/registration thuộc KHMT |
| `Nhóm Atlas` | Faculty Head CNPM mở `/faculty/boards`; board đang `ACTIVE` và có draft scores |
| Phoenix board | Đã `PUBLISHED`, result trung bình `8.50` |
| Topic published | Mở `/topics` bằng Student hoặc public browser |
| Announcement school-wide | Mở `/announcements` bằng Student |
| Announcement department draft | Faculty Head CNTT mở `/announcements/manage` |

Khi cần chạy lại happy path, seed lại database để xoá các record test và đưa
toàn bộ trạng thái về fixture ban đầu. Nhớ đặt lại password `admin123` cho
account nghiệp vụ sau mỗi lần seed.

## 6. Checklist nghiệm thu cuối

- [ ] Admin login bằng `admin@hcmute.edu.vn / admin123`.
- [ ] Staff/student login bằng password `admin123` đã được Admin reset.
- [ ] Faculty Head chỉ thấy và sửa được dữ liệu trong department của mình.
- [ ] Lecturer tạo, sửa khi còn draft, submit được topic.
- [ ] Faculty Head approve, gán supervisor và publish được topic.
- [ ] Student tạo group, join member và chỉ leader submit registration.
- [ ] Faculty Head approve registration và tạo board hợp lệ.
- [ ] Board có 3–5 member, một Chair, một Secretary; không có supervisor trong board.
- [ ] Từng evaluator submit được một score hợp lệ.
- [ ] Không publish được khi thiếu score hoặc board chưa completed.
- [ ] Result publish chuyển board sang published và Student xem được đúng result.
- [ ] Faculty Head tạo được department announcement; Admin tạo được school-wide announcement.
- [ ] Draft/hidden announcement không hiện ở public page.
- [ ] Search, sort, pagination và popup của các directory hoạt động sau refresh.
- [ ] Logout trước mỗi lần đổi account.

## 7. Tài liệu liên quan

- [SETUP_GUIDE.md](../SETUP_GUIDE.md) — cài đặt, DDL, seed và runtime config.
- [AI_WORKFLOW_GUIDE.md](AI_WORKFLOW_GUIDE.md) — handoff cho AI/teammate khi
  implement hoặc tiếp tục task.
- [PRODUCT_VISION.md](../PRODUCT_VISION.md) — phạm vi sản phẩm và business flow.
