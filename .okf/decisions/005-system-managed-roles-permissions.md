---
decision: system-managed-roles-and-permissions
date: 2026-08-30
status: accepted
---

# Decision: system-managed roles and permissions

Roles and permissions are fixed system catalog data. The seed pipeline owns
their creation, naming and baseline activation. Runtime administration may
view the catalog and update role-permission assignments, but the application
does not provide Role CRUD or Permission CRUD.

This keeps authorization identifiers stable and makes a local reset
reproducible. A new role or permission must be introduced through a reviewed
DDL/seed change and then covered by authorization tests. `FACULTY_HEAD` remains
a separate role; its Lecturer capabilities are an explicit
`role_permissions` bundle, not runtime role inheritance.
