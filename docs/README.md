# Project specification map

`REQUEST.md` vẫn là nguồn sự thật cao nhất. Các tài liệu dưới đây là bản phân
rã để lập kế hoạch và sinh code, không mở rộng phạm vi sản phẩm.

Đây là đồ án môn học nên mặc định triển khai MVP: luồng đăng nhập → đợt → đề
tài → nhóm → đăng ký → báo cáo → đánh giá/kết quả. Không tự mở rộng thành hệ
thống production với hội đồng nhiều tầng, dashboard, audit log, email hoặc
nhiều phiên bản báo cáo nếu chưa được chọn trong `REQUEST.md`.

| Tài liệu | Dùng khi |
|---|---|
| [`domain-model.md`](domain-model.md) | Tạo model, enum, DTO và quan hệ domain |
| [`course-alignment.md`](course-alignment.md) | Phân biệt đề cương môn học với quyết định kỹ thuật của project |
| [`workflows.md`](workflows.md) | Implement trạng thái, time window và transaction |
| [`authorization-matrix.md`](authorization-matrix.md) | Implement filter, Service authorization và menu |
| [`database-design.md`](database-design.md) | Tạo schema, DAO và foreign key |
| [`ui-route-map.md`](ui-route-map.md) | Tạo Spring MVC Controller, REST Controller, form action và JSP dưới `WEB-INF/views` |
| [`open-questions.md`](open-questions.md) | Gặp điểm REQUEST yêu cầu giảng viên xác nhận |
| [`verification.md`](verification.md) | Chọn test/build/smoke cho task |

Các tên Java, route và trạng thái có chữ “đề xuất” là hợp đồng kỹ thuật tạm
thời; phải giữ đúng nghiệp vụ của `REQUEST.md` và cập nhật đồng bộ khi code
được scaffold.
