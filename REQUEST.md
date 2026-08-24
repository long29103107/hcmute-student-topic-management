# REQUEST - Hệ thống quản lý đề tài sinh viên

## 1. Mục tiêu

Xây dựng website quản lý đề tài cho Khoa Công nghệ Thông tin, áp dụng cho đề tài môn học, nghiên cứu khoa học (NCKH), tiểu luận chuyên ngành (TLCN) và khóa luận tốt nghiệp (KLTN).

Hệ thống hỗ trợ quy trình từ khi tạo đợt đăng ký, giảng viên và sinh viên đăng ký đề tài, phê duyệt, phân công giảng viên, nộp báo cáo, phản biện, chấm điểm đến khi Khoa CNTT công bố kết quả.

## 2. Stack kỹ thuật

1. **Backend:** Java + Spring Framework/Spring MVC trên nền Jakarta Servlet.
2. **View:** JSP + JSTL, render theo Server-Side Rendering (SSR) bằng Spring MVC.
3. **REST:** Spring MVC RESTful API trong cùng một ứng dụng monolith; không tách thành service riêng.
4. **Database:** JDBC + MySQL; DAO dùng `PreparedStatement`.
5. **Build và server:** Maven + Apache Tomcat.
6. **Frontend:** HTML + CSS + JavaScript.
7. **UI:** Tailwind CSS + jQuery.
8. **Email tùy chọn:** Java Mail chỉ dùng cho chức năng thông báo email nếu được chọn ở Nice to Have.
9. **Kiến trúc:** MVC monolith, gồm SSR pages và REST endpoints dùng chung Service/DAO.

Project dùng Spring Framework Core/Spring MVC trực tiếp với Java config và
`DispatcherServlet`; không dùng Spring Boot.

Không sử dụng SPA frontend, microservices, Docker/Kubernetes hoặc hạ tầng phức tạp
ngoài phạm vi môn học. Spring MVC, RESTful API và Java Mail là công nghệ môn học
được phép dùng; REST/Java Mail không được tạo business path riêng ngoài Service.

## 3. Vai trò người dùng

### 3.1. Quản trị viên

- Quản lý tài khoản người dùng.
- Phân quyền và khóa/mở khóa tài khoản.
- Đăng tải và quản lý thông báo.

### 3.2. Trưởng khoa

- Tạo và quản lý đợt đăng ký.
- Phê duyệt và công bố danh sách đề tài.
- Chấp thuận hoặc từ chối nhóm đăng ký đề tài.
- Phân công GVHD và giảng viên đánh giá; quản lý hội đồng phản biện đầy đủ là
  Should Have.
- Công bố kết quả đánh giá.

### 3.3. Giảng viên

- Đăng ký/đề xuất đề tài trong thời gian quy định.
- Hướng dẫn các đề tài được phân công.
- Tham gia hội đồng phản biện khi chức năng Should Have được chọn.
- Chấm điểm và nhập đánh giá cho đề tài được phân công.

### 3.4. Sinh viên

- Tạo hoặc tham gia nhóm.
- Xem danh sách đề tài đã công bố.
- Đăng ký thực hiện đề tài theo nhóm.
- Theo dõi trạng thái đăng ký.
- Xem kết quả của đề tài mình tham gia sau khi được công bố.

### 3.5. Nhóm trưởng

- Đại diện nhóm đăng ký đề tài.
- Là người duy nhất được nộp báo cáo của nhóm.

## 4. Quy trình chính

```text
Trưởng khoa tạo đợt đăng ký
→ Giảng viên đăng ký/đề xuất đề tài
→ Trưởng khoa phê duyệt và công bố danh sách đề tài
→ Sinh viên tạo nhóm và đăng ký một đề tài
→ Trưởng khoa chấp thuận đăng ký
→ Nhóm thực hiện và nhóm trưởng nộp báo cáo
→ Tạo phiên đánh giá, phân công giảng viên chấm
→ Giảng viên chấm điểm, Khoa tổng hợp kết quả
→ Khoa CNTT công bố kết quả
→ Sinh viên xem kết quả
```

## 5. Yêu cầu chức năng bắt buộc - Must Have

### Ranh giới MVP cho đồ án môn học

Các chức năng bên dưới vẫn là phạm vi đầy đủ của hệ thống, nhưng phiên bản
đồ án chỉ cần triển khai bản tối thiểu chạy được end-to-end. Không xây thêm
module, bảng hoặc màn hình phức tạp nếu chưa phục vụ một tiêu chí Must Have.

- Luồng MVP: đăng nhập → đợt đăng ký → đề tài → nhóm → đăng ký đề tài → nộp
  báo cáo → đánh giá/điểm → công bố kết quả.
- Mô hình lõi: `User`, `Department`, `RegistrationPeriod`, `Topic`,
  `StudentGroup`, `TopicRegistration`, `Report` và `Evaluation`.
- `ReviewBoard` đầy đủ, phân công nhiều người, chair/secretary và workflow
  phản biện chi tiết là Should Have; bản Must Have chỉ cần một phiên đánh giá
  và ít nhất một giảng viên được phân công chấm.
- Thông báo có thể triển khai dạng danh sách đăng/xem cơ bản; quản lý phạm vi,
  ẩn/công bố nâng cao là Should Have.
- Chỉ mở rộng lên bản đầy đủ nếu rubric hoặc giảng viên yêu cầu rõ.

### FR-01. Đăng nhập và phân quyền

- Đăng nhập, đăng xuất và quản lý session.
- Phân quyền đúng cho từng vai trò.
- Mật khẩu không được lưu dưới dạng văn bản thuần.

### FR-02. Quản lý tài khoản

- Quản trị viên xem, tạo, cập nhật và khóa/mở khóa tài khoản.
- Gán vai trò quản trị viên, trưởng khoa, giảng viên hoặc sinh viên.

### FR-03. Quản lý bộ môn

- Quản lý danh sách bộ môn trong Khoa CNTT.
- Mỗi đề tài thuộc đúng một bộ môn.

### FR-04. Quản lý đợt đăng ký

Trưởng khoa tạo và quản lý đợt với:

- Tên đợt đăng ký.
- Loại: môn học, NCKH, TLCN hoặc KLTN.
- Thời gian bắt đầu/kết thúc cho giảng viên đăng ký đề tài.
- Thời gian bắt đầu/kết thúc cho sinh viên đăng ký đề tài.
- Hạn GVPB nộp điểm, chỉ thiết lập cho TLCN hoặc KLTN.
- Ngày báo cáo hội đồng, chỉ thiết lập cho KLTN.

Đợt đăng ký gồm hai giai đoạn:

1. Giảng viên đăng ký đề tài; Khoa phê duyệt và công bố danh sách.
2. Nhóm sinh viên đăng ký đề tài đã được công bố.

Hệ thống không cho phép đăng ký ngoài thời gian quy định.

### FR-05. Quản lý đề tài

- Giảng viên tạo/đăng ký đề tài trong thời gian quy định.
- Đề tài có tên, nội dung/mô tả, bộ môn, đợt đăng ký và trạng thái.
- Mỗi đề tài có ít nhất 1 và tối đa 2 GVHD.
- Trưởng khoa phê duyệt hoặc từ chối đề tài.
- Chỉ đề tài đã phê duyệt và công bố mới được sinh viên đăng ký.

### FR-06. Quản lý nhóm sinh viên

- Sinh viên thực hiện đề tài theo nhóm.
- Mỗi nhóm có tối đa 3 sinh viên và đúng 1 nhóm trưởng.
- Mỗi sinh viên chỉ tham gia duy nhất 1 nhóm trong quá trình thực hiện đề tài.

### FR-07. Đăng ký đề tài

- Chỉ nhóm trưởng được gửi yêu cầu đăng ký.
- Chỉ đăng ký trong thời gian dành cho sinh viên.
- Chỉ chọn đề tài đã công bố trong đúng đợt.
- Mỗi nhóm chỉ đăng ký duy nhất 1 đề tài.
- Trưởng khoa chấp thuận hoặc từ chối đăng ký.

### FR-08. Nộp báo cáo

- Chỉ nhóm trưởng của nhóm đã được chấp thuận mới được nộp báo cáo.
- Lưu file báo cáo, người nộp và thời gian nộp.
- Thành viên nhóm, GVHD và người chấm được xem/tải báo cáo theo quyền.

### FR-09. Phân công giảng viên

- Quản lý từ 1 đến 2 GVHD cho mỗi đề tài.
- Bản MVP phân công ít nhất một giảng viên được phép chấm cho mỗi đề tài.
- Một giảng viên có thể chấm nhiều đề tài.
- Giảng viên không được chấm đề tài mình đang hướng dẫn.
- Phân công nhiều loại reviewer và lịch sử phân công chi tiết là Should Have.

### FR-10. Hội đồng phản biện

- Bản MVP có một phiên đánh giá gắn với đề tài và giảng viên được phân công.
- Giảng viên được phân công nhập đánh giá và điểm.
- Không phân công GVHD chấm chính đề tài họ hướng dẫn.
- Hội đồng 3–5 giảng viên, chair/secretary và tổng hợp nhiều đánh giá là
  Should Have, trừ khi rubric bắt buộc.

### FR-11. Chấm điểm và công bố kết quả

- Giảng viên nhập điểm và nhận xét cho đề tài được phân công.
- Điểm cuối cùng là trung bình cộng các điểm thành phần.
- Khoa CNTT công bố kết quả khi phản biện và chấm điểm hoàn tất.
- Sinh viên chỉ xem điểm và kết quả của đề tài mình tham gia.

### FR-12. Quản lý thông báo

- Bản MVP cho phép tạo và xem thông báo cơ bản.
- Quản lý thông báo theo phạm vi trường/khoa, sửa, ẩn và công bố nâng cao là
  Should Have.

## 6. Chức năng nên có - Should Have

Chỉ làm sau khi luồng Must Have đã hoạt động:

- Tìm kiếm và lọc đề tài theo đợt, loại, bộ môn và trạng thái.
- Phân trang các danh sách.
- Hiển thị lý do từ chối đề tài hoặc đăng ký.
- Cho phép nhóm trưởng nộp lại báo cáo trước hạn.
- Hiển thị rõ hạn đăng ký và hạn chấm điểm.
- Khóa sửa điểm sau khi công bố kết quả.
- Giao diện responsive trên desktop/mobile.
- Validation phía client; server vẫn phải kiểm tra lại.
- Hội đồng phản biện đầy đủ: 3–5 giảng viên, chair/secretary, phân công topic
  và tổng hợp nhiều đánh giá.
- Phân công nhiều reviewer/GVPB và lịch sử phân công.
- Quản lý thông báo đầy đủ theo phạm vi, trạng thái và quyền.

## 7. Chức năng có thì tốt - Nice to Have

Không bắt buộc cho phiên bản đầu:

- Dashboard thống kê theo vai trò.
- Nhật ký hoạt động chi tiết.
- Gửi thông báo qua email.
- Tìm kiếm/lọc bằng AJAX.
- Lưu nhiều phiên bản báo cáo.

## 8. Quy tắc nghiệp vụ

| Mã | Quy tắc |
|---|---|
| BR-01 | Mỗi đề tài thuộc đúng một bộ môn. |
| BR-02 | Mỗi đề tài có từ 1 đến 2 GVHD. |
| BR-03 | Mỗi nhóm có tối đa 3 sinh viên và đúng 1 nhóm trưởng. |
| BR-04 | Mỗi sinh viên chỉ tham gia duy nhất 1 nhóm trong quá trình thực hiện đề tài. |
| BR-05 | Mỗi nhóm chỉ đăng ký duy nhất 1 đề tài. |
| BR-06 | Chỉ nhóm trưởng được đăng ký đề tài và nộp báo cáo. |
| BR-07 | Chỉ được đăng ký trong thời gian quy định. |
| BR-08 | Hạn GVPB chỉ áp dụng cho TLCN hoặc KLTN. |
| BR-09 | Ngày báo cáo hội đồng chỉ áp dụng cho KLTN. |
| BR-10 | MVP có ít nhất 1 giảng viên được phân công đánh giá; hội đồng 3–5 GV, đúng 1 chủ tịch và 1 thư ký là Should Have. |
| BR-11 | GV không được chấm đề tài mình hướng dẫn. |
| BR-12 | Điểm cuối cùng là trung bình cộng các điểm thành phần. |
| BR-13 | Sinh viên chỉ xem kết quả của đề tài mình tham gia. |

## 9. Dữ liệu chính và mối quan hệ rút gọn

Để dễ phân tích và triển khai phiên bản đầu, mô hình khái niệm được rút gọn
thành các thực thể lõi sau. Khi thiết kế database, các bảng liên kết kỹ thuật
vẫn có thể tách riêng nhưng không tạo thêm aggregate nghiệp vụ mới nếu không
cần thiết.

### 9.1. Các thực thể lõi

- `User`: tài khoản, thông tin cá nhân, vai trò và trạng thái hoạt động. Bao
  gồm quản trị viên, trưởng khoa, giảng viên và sinh viên; `GROUP_LEADER` là
  thuộc tính của thành viên nhóm.
- `Department`: bộ môn thuộc Khoa CNTT.
- `RegistrationPeriod`: đợt đăng ký, loại đề tài và các mốc thời gian cho GV,
  SV, GVPB và hội đồng.
- `Topic`: đề tài, nội dung, bộ môn, đợt đăng ký, trạng thái và các giảng viên
  hướng dẫn.
- `StudentGroup`: nhóm sinh viên, danh sách thành viên và nhóm trưởng.
- `TopicRegistration`: yêu cầu nhóm đăng ký một đề tài và trạng thái duyệt.
- `Report`: báo cáo của nhóm, thông tin file, người nộp và thời gian nộp.
- `ReviewBoard`: cấu trúc hội đồng mở rộng cho Should Have; MVP chỉ cần giữ
  thông tin giảng viên được phân công trong `Evaluation`.
- `Evaluation`: phiên đánh giá tối giản, người chấm, điểm, nhận xét, điểm tổng
  hợp và trạng thái công bố kết quả.
- `Announcement`: thông báo, phạm vi hiển thị, người đăng và trạng thái công
  bố.

### 9.2. Mối quan hệ chính

| Quan hệ | Bội số | Quy tắc chính |
|---|---:|---|
| `Department` - `Topic` | 1 - N | Mỗi đề tài thuộc đúng một bộ môn. |
| `RegistrationPeriod` - `Topic` | 1 - N | Mỗi đề tài thuộc đúng một đợt đăng ký. |
| `Topic` - `User` (giảng viên hướng dẫn) | N - N | Mỗi đề tài có từ 1 đến 2 GVHD. |
| `User` (sinh viên) - `StudentGroup` | N - N | Nhóm tối đa 3 SV, đúng 1 nhóm trưởng; một SV không tham gia trùng nhóm. |
| `StudentGroup` - `TopicRegistration` | 1 - N theo lịch sử | Mỗi nhóm chỉ có một đăng ký hiện hành trong cùng ngữ cảnh. |
| `Topic` - `TopicRegistration` | 1 - N | Chỉ đề tài đã công bố và đúng đợt mới được đăng ký. |
| `TopicRegistration` - `Report` | 1 - N theo phiên bản | Chỉ nhóm trưởng của đăng ký đã được chấp thuận được nộp. |
| `ReviewBoard` - `User` (giảng viên) | N - N, Should | Chỉ cần khi triển khai hội đồng đầy đủ; board có 3–5 GV, đúng 1 chủ tịch và 1 thư ký. |
| `ReviewBoard` - `Topic` | N - N, Should | Chỉ cần khi triển khai phân công topic vào hội đồng; MVP dùng `Evaluation` trực tiếp. |
| `TopicRegistration` - `Evaluation` | 1 - 0..1 | Kết quả được tổng hợp sau khi chấm và chỉ hiển thị sau khi công bố. |
| `User` - `Announcement` | 1 - N | Người có quyền tạo, sửa, ẩn và công bố thông báo. |

Các quan hệ nhiều-nhiều có thể được triển khai bằng bảng liên kết như
`topic_supervisors`, `group_members`, `review_board_members` và
`board_topic_assignments`; đây là chi tiết database, không phải thực thể lõi
độc lập trong mô hình khái niệm rút gọn.

## 10. Yêu cầu kỹ thuật tối thiểu

- Code theo MVC: Spring MVC Controller/REST Controller → Service → Repository/DAO → JDBC; JSP chỉ hiển thị dữ liệu.
- JSP đặt trong `WEB-INF/views` và truy cập thông qua Spring MVC Controller.
- REST endpoints đặt dưới `/api`, chỉ bind DTO/JSON và gọi lại Service; không đặt nghiệp vụ trong Controller.
- Dùng `PreparedStatement`; không nối input vào câu SQL.
- Các thao tác nhiều bước quan trọng phải dùng transaction.
- Dùng Spring dependency injection bằng constructor; không dùng field injection.
- Kiểm tra quyền và validation ở server.
- Escape dữ liệu hiển thị để hạn chế XSS.
- Kiểm tra loại, kích thước và tên file upload.
- Không hard-code thông tin kết nối database trong Java source.
- Giao diện tiếng Việt, sử dụng Tailwind CSS.

## 11. Tiêu chí nghiệm thu Must Have

1. Build thành công bằng Maven, chạy trên Tomcat và kết nối MySQL.
2. Đăng nhập và phân quyền đúng.
3. Trưởng khoa tạo được đợt với các mốc thời gian hợp lệ.
4. Giảng viên tạo đề tài có 1–2 GVHD; đề tài được duyệt và công bố.
5. Sinh viên tạo nhóm tối đa 3 thành viên, không tham gia trùng nhóm.
6. Nhóm trưởng đăng ký đúng 1 đề tài trong đúng thời gian.
7. Nhóm trưởng nộp được báo cáo.
8. Tạo được một phiên đánh giá và phân công ít nhất một giảng viên chấm.
9. Hệ thống chặn GV chấm đề tài mình hướng dẫn.
10. Giảng viên nhập điểm; hệ thống tính đúng điểm trung bình.
11. Khoa công bố kết quả; sinh viên chỉ xem kết quả của nhóm mình.
12. Người dùng xem được thông báo cơ bản; quản lý nâng cao là Should Have.

## 12. Ngoài phạm vi phiên bản đầu

- SPA frontend và các framework React, Angular hoặc Vue.
- Microservices.
- Docker, Kubernetes hoặc CI/CD.
- Email, dashboard và audit log nâng cao, trừ khi task Nice to Have tương ứng được chọn.
- Báo cáo thống kê phức tạp.
- Hội đồng 3–5 giảng viên với chair/secretary nếu rubric không yêu cầu; bản
  MVP dùng phiên đánh giá đơn giản.
- Mô hình phân công reviewer nhiều tầng hoặc nhiều loại điểm thành phần.

## 13. Điểm cần xác nhận với giảng viên

Đề bài chưa quy định rõ:

1. Một đề tài có thể được nhiều nhóm thực hiện hay chỉ một nhóm.
2. Cách mời/xác nhận thành viên khi tạo nhóm.
3. Thang điểm, các điểm thành phần và quy tắc làm tròn.
4. Loại đề tài nào bắt buộc dùng hội đồng; đề chỉ quy định ngày hội đồng riêng cho KLTN.
5. Hạn nộp báo cáo, loại file và dung lượng tối đa.
6. Người phê duyệt ở từng bước nếu có thêm giáo vụ hoặc Trưởng bộ môn.

Không tự biến các điểm chưa rõ này thành quy tắc bắt buộc trước khi được xác nhận.

## 14. Nguồn yêu cầu

- Đề bài **Hệ thống quản lý đề tài sinh viên** do người dùng cung cấp.
- Stack đã thống nhất theo nội dung môn học: Java Spring MVC/Jakarta Servlet, JSP/JSTL, RESTful API trong cùng monolith, JDBC/MySQL, Maven/Tomcat, Tailwind CSS, jQuery và Java Mail tùy chọn.
