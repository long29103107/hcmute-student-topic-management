# Testing Standard

## Verification levels

Run the smallest relevant check for the changed layer:

```powershell
mvn test
mvn package
```

- Model/validator changes: focused unit tests.
- Service/business-rule changes: service tests with fake/in-memory DAO seams
  plus transaction/authorization cases.
- Spring MVC/REST changes: controller tests with MockMvc or an equivalent
  Spring test setup, including JSON/view/error contracts.
- DAO/schema changes: JDBC integration tests against a configured MySQL test
  database, when available.
- Servlet/JSP changes: servlet request/response tests where practical, then
  `mvn package` and a Tomcat smoke pass for the touched flow.

## Required business-rule coverage

Tests must cover, as the corresponding phases are implemented:

- role/session protection;
- registration time windows;
- one-to-two supervisors per topic;
- groups capped at three students and one leader;
- one group registration per group and leader-only actions;
- report upload permissions and file validation;
- board size 3–5, exactly one chair and one secretary;
- supervisor cannot score their own topic;
- final average calculation and result visibility after publication.

## Reporting

Final task notes must list passed checks and explicitly state checks skipped due
to missing Maven dependencies, MySQL, Tomcat or configuration. Do not mark a
phase complete based only on compilation if its done criteria require runtime
behavior.
