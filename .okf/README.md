# OKF Operating Guide

Thư mục này theo dõi các phase và implementation task của hệ thống quản lý
đề tài sinh viên.

## How to work

1. Đọc `PRODUCT_VISION.md` để xác định milestone và scope được phép tạo task.
2. Đọc `REQUEST.md`.
3. Đọc `.okf/standards/` và tài liệu liên quan trong `docs/`, đặc biệt
   `docs/course-alignment.md` khi chọn framework/UI.
4. Đọc phase summary hiện tại (Phase 001 nếu đang truy vết nền identity,
   sau đó Phase 002 cho academic workflow).
5. Chọn task nhỏ nhất phù hợp với active milestone.
6. Implement một lát cắt hoàn chỉnh theo task note.
7. Chạy verification rồi mới cập nhật trạng thái task/phase.

## Source of truth

- `PRODUCT_VISION.md`: nguồn ưu tiên về product scope, milestone, task order và
  điều kiện để tạo task mới.
- `PHASE_SUMMARY.md`: trạng thái thực thi của milestone hiện tại và task hiện tại.
- `REQUEST.md`: tài liệu tham chiếu về stack, chức năng, business rules và open
  questions khi không mâu thuẫn với Product Vision.
- `.okf/standards/`: quy tắc Spring MVC/REST, coding, Servlet/Thymeleaf, security,
  Java Mail và test.
- `docs/`: đặc tả miền và hợp đồng dùng chung để sinh code.

## Current scope

Phase `001` identity/access đã hoàn tất. Phase `002` academic workflow được
mở sau quyết định rõ ràng của user, hiện bắt đầu từ Topic Proposal CRUD. Các
task tiếp theo vẫn phải bám đúng scope và không được tự mở rộng sang phần chưa
được chọn.

Không sao chép các tài liệu hoặc phase từ dự án khác nếu chúng đưa vào stack,
kiến trúc hoặc hạ tầng ngoài phạm vi `REQUEST.md`.

## Phase discipline

Không tự tạo phase mới. Khi user chọn rõ một milestone roadmap mới, phase note
phải ghi lại quyết định đó, có `Vision alignment` rõ ràng và giữ boundary
không mở rộng. Các điểm chưa được xác nhận trong `docs/open-questions.md`
phải giữ dạng cấu hình/placeholder hoặc được ghi rõ là chờ xác nhận, không tự
suy diễn.
