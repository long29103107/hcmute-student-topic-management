# OKF Operating Guide

Thư mục này theo dõi các phase và implementation task của hệ thống quản lý
đề tài sinh viên.

## How to work

1. Đọc `REQUEST.md`.
2. Đọc `.okf/standards/` và tài liệu liên quan trong `docs/`, đặc biệt
   `docs/course-alignment.md` khi chọn framework/UI.
3. Đọc `.okf/phase/<phase>/PHASE_SUMMARY.md` của phase đang làm.
4. Chọn task nhỏ nhất trong `Task Index`/`Task Notes`.
5. Implement một lát cắt hoàn chỉnh theo task note.
6. Chạy verification rồi mới cập nhật trạng thái task/phase.

## Source of truth

- `REQUEST.md`: phạm vi, stack, chức năng, business rules và open questions.
- `PHASE_SUMMARY.md`: trạng thái phase, task hiện tại và tiêu chí đóng phase.
- `.okf/standards/`: quy tắc Spring MVC/REST, coding, Servlet/JSP, security,
  Java Mail và test.
- `docs/`: đặc tả miền và hợp đồng dùng chung để sinh code.

Không sao chép các tài liệu hoặc phase từ dự án khác nếu chúng đưa vào stack,
kiến trúc hoặc hạ tầng ngoài phạm vi `REQUEST.md`.

## Phase discipline

Phase sau chỉ chuyển khỏi `planned` khi phase trước đã đạt toàn bộ done
criteria. Các điểm chưa được xác nhận trong `docs/open-questions.md` phải giữ
dạng cấu hình/placeholder hoặc được ghi rõ là chờ xác nhận, không tự suy diễn.
