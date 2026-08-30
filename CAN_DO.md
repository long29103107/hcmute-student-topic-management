# Việc có thể làm trong lúc chờ team

Danh sách các việc owner core workflow có thể làm song song trong lúc 4 thành viên phát triển các module nghiệp vụ.

## Ưu tiên chính

1. Chốt state/status và state transition cho toàn bộ workflow.
2. Chốt permission cho Faculty Head, Lecturer, Student và Group Leader.
3. Hoàn thiện database migration, constraint và index.
4. Làm Core Workflow Service để điều phối các module.
5. Tạo API contract và read-only query contract giữa các module.
6. Tạo seed data và test fixture dùng chung.
7. Viết integration test cho flow chính.
8. Rà lại `SecurityConfig`, đặc biệt rule giữa `FACULTY_HEAD` và `LECTURER`.

## Việc hỗ trợ thêm

9. Làm shared UI: layout, sidebar, breadcrumb, flash message và error page.
10. Chuẩn hóa validation và error response dùng chung.
11. Setup CI chạy build/test tự động.
12. Viết PR template và code review checklist.
13. Cập nhật `README.md`, `BREAK_TASK.md` và `SETUP_GUIDE.md`.
14. Chuẩn bị mock data để mỗi module có thể test độc lập.
15. Kiểm tra logging, transaction và xử lý lỗi toàn hệ thống.

## Core Workflow cần đạt

```text
Tạo period
→ Đề xuất topic
→ Approve/publish topic
→ Tạo group/đăng ký topic
→ Approve registration
→ Nộp report
→ Assign evaluator
→ Chấm điểm
→ Publish result
→ Student xem kết quả
```

## Thứ tự nên làm

```text
State machine
→ Permission
→ Migration/constraint
→ API contract
→ Core workflow
→ Test fixture
→ Integration test
→ CI và tài liệu
```

## Nguyên tắc tránh conflict

- Không sửa table thuộc module của thành viên khác.
- Không tự ý đổi shared DTO, enum hoặc permission sau khi đã chốt contract.
- Core workflow chỉ gọi module qua service/query contract.
- Mọi thay đổi database dùng chung phải được review trước khi merge.
- Mỗi thay đổi phải có test tương ứng.
