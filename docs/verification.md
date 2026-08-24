# Verification runbook

For the course-project MVP, verify the core path first: authentication, period,
topic, group, registration, report and simple evaluation/result publication.
Full review-board, dashboard, email, audit-log and report-version checks are
optional and should only be run when that scope is selected.

## Local Maven checks

Run from repository root after `pom.xml` exists:

```powershell
mvn test
mvn package
```

Build stylesheet trước khi deploy JSP:

```powershell
npm install
npm run css:build
```

Use a focused test class while iterating, then rerun the full suite before
closing the task. Package output should be a deployable WAR.

## MySQL checks

Use a dedicated test schema and connection values supplied through Maven
profile, system properties or environment variables. Never commit credentials.

Verify at least:

- schema can be created from the repository resources;
- foreign keys/unique constraints exist;
- transaction rollback leaves no partial group, registration or evaluation;
- if the extended board is selected, also verify no partial board/member/topic
  assignment;
- prepared statements handle quotes and malicious-looking input as data.

## Spring MVC/Tomcat smoke checklist

With the WAR deployed:

1. Anonymous user is redirected to login for protected pages.
2. Invalid login does not create an authenticated session.
3. Each role sees only its permitted actions and direct URL/REST access is checked.
4. Faculty Head creates a period with valid windows and invalid windows fail.
5. Lecturer proposes/Faculty Head publishes topic; student cannot select an
   unpublished topic.
6. Student group rules, leader-only registration and duplicate registration are
   enforced.
7. Leader report upload validates permission and configured file policy.
8. Board cardinality/roles and supervisor scoring restriction are enforced.
9. Final average/publication/result visibility follow the configured policy.
10. Published announcements are visible; hidden announcements are not.

## REST smoke checklist

When REST endpoints are implemented, verify at least:

- login/logout session behavior through `/api/auth/*`;
- JSON validation and error status for malformed requests;
- the same role/resource authorization as the SSR route;
- no REST endpoint exposes domain entities, passwords, stack traces or SQL;
- repeated calls do not create duplicate registrations or scores.
