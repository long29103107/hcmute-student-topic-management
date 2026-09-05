# Authorization matrix

`GROUP_LEADER` is a group membership attribute. A request must satisfy both
the system role and the resource relationship where a row says “own/group”.

For the course-project MVP, use this matrix with a simple assigned-lecturer
evaluation. The full review-board/member/role flow is a Should Have extension;
do not create extra authorization paths for it unless selected.

| Capability | Admin (full capability) | Faculty Head | Lecturer | Student | Group leader |
|---|---:|---:|---:|---:|---:|
| Sign in/out | yes | yes | yes | yes | yes |
| Manage users, roles, lock/unlock, safe delete | yes | no | no | no | no |
| Manage departments, including safe delete | yes | no | no | no | no |
| View own department members | yes | yes | no | no | no |
| Create/manage registration periods | yes | yes | no | no | no |
| Propose topics | yes | yes, via Lecturer permissions | yes | no | no |
| Approve/reject topics | yes | yes | no | no | no |
| Publish approved topics | yes | yes | no | no | no |
| View published topics | yes | yes | yes | yes | yes |
| Create/join group | yes | no | no | yes | yes |
| Transfer group leader | yes | no | no | no | current leader only |
| Submit topic registration | yes | no | no | no | yes |
| Approve/reject group registration | yes | yes | no | no | no |
| Submit report | yes | no | no | no | group leader only |
| View/download permitted report | yes | yes | assigned roles | group members + assigned roles | group member |
| Manage topic supervisors | yes | yes, own department | no | no | no |
| Manage review board/assignments | yes | yes, Should | no | no | no |
| Enter score for assigned topic | yes | assigned Faculty Head only when also the assigned lecturer | assigned lecturer | no | no |
| Aggregate board result | yes | yes/assigned chair, Should | chair only, Should | no | no |
| Publish final result | yes | yes | no | no | no |
| View own group result after publication | yes | no | no | own group only | own group only |
| Manage announcements | yes | yes if granted | no | no | no |
| View published announcements | yes | yes | yes | yes | yes |

Admin is a full-capability system role for operational surfaces: the seed
assigns every current permission to `ADMIN`, route gates accept Admin for
every role workspace, and the sidebar exposes available role workspaces.
Resource-specific actor rules still apply; Admin can publish across
departments, while Faculty Head publication is limited to the assigned
department. Faculty Head receives the
Lecturer permission bundle explicitly through `role_permissions`, then
receives additional faculty workflow permissions such as `PERIOD_MANAGE`,
`SUPERVISOR_MANAGE`, `TOPIC_REVIEW` and `REGISTRATION_REVIEW`. Resource-specific rules still apply
where a contract explicitly says own/group/assigned; full Admin capability
does not silently impersonate another user's ownership relationship.

## Enforcement notes

- Use an authentication filter for unauthenticated route protection and a
  Service authorization check for resource ownership/assignment.
- UI visibility is not a security boundary.
- Group membership mutations and leader transfer run in a transaction with
  pessimistic locks on the affected group and registration-period rows. This
  serializes concurrent joins/creates for one period and concurrent updates to
  one group before the service rechecks its invariants.
- Admin has full operational capability across current role surfaces except
  explicitly documented actor-only transitions; future own/group views must
  document whether they are Admin-wide operational views or remain
  relationship-scoped.
- The exact approver model when giáo vụ/trưởng bộ môn exists is open; do not add
  those roles until `REQUEST.md` is confirmed.
- Issue #13 adds the seeded `REPORT_VIEW` capability for report
  metadata/download checks. Student access is limited to group members;
  Lecturer access is limited to supervisors/evaluators; Faculty Head access is
  limited to the assigned department; Admin has operational access across
  reports. The Service enforces these resource relationships for both metadata
  and file download.
