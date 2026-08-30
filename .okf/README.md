# OKF Operating Guide

Thư mục này theo dõi các phase và implementation task của hệ thống quản lý
đề tài sinh viên.

## How to work

1. Đọc `PRODUCT_VISION.md` để xác định milestone và scope được phép tạo task.
2. Đọc `REQUEST.md`.
3. Đọc `.okf/standards/` và tài liệu liên quan trong `docs/`, đặc biệt
   `docs/course-alignment.md` khi chọn framework/UI.
4. Đọc `.okf/phase/001/PHASE_SUMMARY.md`.
5. Chọn task nhỏ nhất phù hợp với active milestone.
6. Implement một lát cắt hoàn chỉnh theo task note.
7. Chạy verification rồi mới cập nhật trạng thái task/phase.

## Source of truth

- `PRODUCT_VISION.md`: nguồn ưu tiên về product scope, milestone, task order và
  điều kiện để tạo task mới.
- `PHASE_SUMMARY.md`: trạng thái thực thi của milestone hiện tại và task hiện tại.
- `REQUEST.md`: tài liệu tham chiếu về stack, chức năng, business rules và open
  questions khi không mâu thuẫn với Product Vision.
- `.okf/standards/`: quy tắc Spring MVC/REST, coding, Servlet/JSP, security,
  Java Mail và test.
- `docs/`: đặc tả miền và hợp đồng dùng chung để sinh code.

## Current scope

Chỉ có phase `001` đang hoạt động. Mục tiêu là hoàn thành User, Role,
Permission và Login CRUD/authorization. Các module học vụ nằm trong Later
product roadmap của `PRODUCT_VISION.md` và không được biến thành task khi chưa
có quyết định mới từ user.

Không sao chép các tài liệu hoặc phase từ dự án khác nếu chúng đưa vào stack,
kiến trúc hoặc hạ tầng ngoài phạm vi `REQUEST.md`.

## Phase discipline

Không tự tạo phase mới. Task mới phải nằm trong active milestone của
`PRODUCT_VISION.md`, có `Vision alignment` rõ ràng và được thêm vào phase 001.
Các điểm chưa được xác nhận trong `docs/open-questions.md` phải giữ dạng cấu
hình/placeholder hoặc được ghi rõ là chờ xác nhận, không tự suy diễn.
