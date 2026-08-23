---
phase: 008
title: Nice to Have Backlog
status: planned
created_at: 2026-08-20
updated_at: 2026-08-20
current_task: null
task_count: 4
done_count: 0
depends_on: [007]
---

# Phase 008 Summary

## Phase Goal

Theo dõi các chức năng Nice to Have trong `REQUEST.md` mà không làm chậm hoặc
mở rộng sai phiên bản đầu.

## Phase Done Criteria

Chỉ đóng từng task khi có yêu cầu rõ, acceptance criteria riêng và không ảnh
hưởng các phase Must/Should Have đã hoàn tất.

## Scope

In: dashboard thống kê, activity log, email notification, AJAX search và nhiều
phiên bản report như backlog có điều kiện.

Out: mọi năng lực không xuất hiện trong `REQUEST.md` hoặc không được chọn trong
phiên bản đầu.

## Task Index

| Task | Title | Status | Done At |
|---|---|---|---|
| 008_001 | Role dashboard statistics | planned | |
| 008_002 | Detailed activity log | planned | |
| 008_003 | Java Mail notifications | planned | |
| 008_004 | AJAX search and report versions | planned | |

## Current Task

No task is active. This phase is intentionally not part of the first release.

## Completed Notes

No phase tasks are complete yet.

## Next Task Proposal

Do not start until Phase 007 is complete and a separate user request selects a
specific Nice to Have item.

## Task Notes

### 008_001 - Role dashboard statistics

Only plan after explicit request. Metrics must be derived from existing DAO and
Service contracts and must not create complex reporting infrastructure.

### 008_002 - Detailed activity log

Only plan after explicit request. Define retention, sensitive-data redaction and
authorization before adding a table or UI.

### 008_003 - Java Mail notifications

Only plan after explicit request and SMTP/provider confirmation. Email must not
become a hidden dependency of the core workflow.

### 008_004 - AJAX search and report versions

Only plan after explicit request. AJAX must reuse REST Controller/Service rules and
report versions must resolve the current open file-history policy.
