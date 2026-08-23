# Authorization matrix

`GROUP_LEADER` is a group membership attribute. A request must satisfy both
the system role and the resource relationship where a row says “own/group”.

| Capability | Admin | Faculty Head | Lecturer | Student | Group leader |
|---|---:|---:|---:|---:|---:|
| Sign in/out | yes | yes | yes | yes | yes |
| Manage users, roles, lock/unlock | yes | no | no | no | no |
| Manage departments | no | yes | view as needed | no | no |
| Create/manage registration periods | no | yes | no | no | no |
| Propose topics | no | no | yes | no | no |
| Approve/reject/publish topics | no | yes | no | no | no |
| View published topics | yes | yes | yes | yes | yes |
| Create/join group | no | no | no | yes | yes |
| Submit topic registration | no | no | no | no | yes |
| Approve/reject group registration | no | yes | no | no | no |
| Submit report | no | no | no | no | group leader only |
| View/download permitted report | yes | yes | assigned roles | group members + assigned roles | group member |
| Manage supervisors/board/assignments | no | yes | no | no | no |
| Enter score for assigned topic | no | no | assigned lecturer | no | no |
| Aggregate board result | no | yes/assigned chair | chair only | no | no |
| Publish final result | no | yes | no | no | no |
| View own group result after publication | no | no | no | own group only | own group only |
| Manage announcements | yes | yes if granted | no | no | no |
| View published announcements | yes | yes | yes | yes | yes |

## Enforcement notes

- Use an authentication filter for unauthenticated route protection and a
  Service authorization check for resource ownership/assignment.
- UI visibility is not a security boundary.
- Admin rights do not automatically grant student-owned result visibility in
  the student route; use explicit operational and student views.
- The exact approver model when giáo vụ/trưởng bộ môn exists is open; do not add
  those roles until `REQUEST.md` is confirmed.
