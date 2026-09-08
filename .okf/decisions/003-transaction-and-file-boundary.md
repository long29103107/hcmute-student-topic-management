# ADR 003: Transaction and Report File Boundary

Status: Accepted for implementation planning

## Decision

Use Service-owned transactions for multi-step operations. Spring Data
repository calls must participate in the enclosing `@Transactional` context.

For reports, persist metadata (`report_id`, group/topic relation, stored name,
original display name, content type, size, uploader and submitted time) in
MySQL. Store bytes in a configured non-public filesystem location unless a
later approved decision selects another storage provider. Never use the
original filename as the path.

The exact report file type and maximum size remain open per the project memory
in `.okf/README.md`.
