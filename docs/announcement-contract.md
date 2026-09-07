# Announcement contract

## Vision alignment

This is the user-selected contract task for the Announcement bundle in the
later academic-workflow roadmap. It prepares the shared vocabulary and server
authorization contract for the backend task without adding the persistence,
service, SSR or REST implementation owned by the following tasks.

## Domain fields

The `Announcement` entity introduced by the backend task must contain:

| Field | Contract |
| --- | --- |
| `id` | Generated numeric identifier. |
| `title` | Required, trimmed text with a bounded length suitable for a page heading. |
| `content` | Required text body; rendered through escaped Thymeleaf output. |
| `scope` | `SCHOOL` or `DEPARTMENT`. |
| `department` | Required only for `DEPARTMENT` scope; null for `SCHOOL`. |
| `status` | `DRAFT`, `PUBLISHED` or `HIDDEN`. |
| `author` | Required reference to the user who created the announcement. |
| `publishedAt` | Null until publication; set by the publish operation. |
| `createdAt` | Immutable audit timestamp. |
| `updatedAt` | Updated by every persisted change. |

The status and scope values are represented by
`AnnouncementStatus` and `AnnouncementScope` rather than free-form strings.

## Lifecycle

- Creation always starts in `DRAFT`.
- `DRAFT -> PUBLISHED` is the publish transition.
- `PUBLISHED -> HIDDEN` is the hide transition.
- `HIDDEN -> PUBLISHED` is allowed when an authorized manager republishes the
  announcement.
- Draft and hidden announcements are not visible to end users.
- `publishedAt` is set when publishing and is not cleared when an announcement
  is hidden, preserving the publication audit timestamp.
- State-changing operations are server-side, transactional and CSRF-protected
  through the existing MVC/REST conventions.

## Visibility and authorization

- `SCHOOL` + `PUBLISHED`: visible to authenticated users in every department.
- `DEPARTMENT` + `PUBLISHED`: visible only to users in the referenced
  department. Admin can view all departments as an operational role.
- `DRAFT` and `HIDDEN`: never returned by the published announcement query.
- Admin receives `ANNOUNCEMENT_MANAGE` and can create, edit, publish and hide
  announcements across departments.
- Faculty Head receives `ANNOUNCEMENT_MANAGE` and can create, edit, publish and
  hide department-scoped announcements in the Faculty Head's department.
- Lecturer and Student do not receive `ANNOUNCEMENT_MANAGE`; they can only read
  published announcements allowed by the visibility rules above.
- A user without a department can read school-wide published announcements but
  cannot satisfy a department-scoped visibility check.

The service layer remains the source of truth for resource scope. Hiding a UI
action or protecting only an SSR route is not sufficient for authorization;
the same rules must apply to REST operations.

## Implementation boundary

This contract task owns:

- the `AnnouncementScope` and `AnnouncementStatus` enums;
- the `ANNOUNCEMENT_MANAGE` seeded permission and Admin/Faculty Head mapping;
- this documentation and focused seed authorization coverage.

The following task owns the `announcements` table, JPA entity/repository,
service transitions, published-scope queries and operation endpoints.
