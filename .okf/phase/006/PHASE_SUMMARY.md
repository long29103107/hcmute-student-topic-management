---
phase: 006
title: Must Have End-to-End Closure and Hardening
status: planned
created_at: 2026-08-20
updated_at: 2026-08-20
current_task: null
task_count: 4
done_count: 0
depends_on: [005]
---

# Phase 006 Summary

## Phase Goal

Đảm bảo toàn bộ luồng Must Have chạy xuyên suốt từ login đến công bố kết quả,
với validation/authorization/transaction và tài liệu đủ để bàn giao.

## Phase Done Criteria

- 12 tiêu chí nghiệm thu Must Have trong `REQUEST.md` có evidence.
- Mọi form mutation quan trọng có POST/redirect/GET, server validation và
  unauthorized direct URL test.
- SQL không nối input; JSP output được escape; upload và password handling đạt
  security standard.
- README/docs/phase notes phản ánh đúng code thực tế.

## Scope

In: integration tests, navigation/guards, security review, Must Have closure.

Out: Should/Nice to Have functionality unless needed to fix a Must Have.

## Task Index

| Task | Title | Status | Done At |
|---|---|---|---|
| 006_001 | Cross-role navigation and authorization audit | planned | |
| 006_002 | End-to-end Must Have verification | planned | |
| 006_003 | Documentation and handoff closure | planned | |
| 006_004 | Phase closure review | planned | |

## Current Task

No task is active. Next proposal: `006_001`.

## Completed Notes

No phase tasks are complete yet.

## Next Task Proposal

Audit direct URL and role transitions before final smoke; this catches security
gaps that happy-path browser testing misses.

## Task Notes

### 006_001 - Cross-role navigation and authorization audit

#### Step Goal

Audit every route in `docs/authorization-matrix.md` and ensure Filter + Service
enforcement matches the matrix.

#### Dependency

- Phases 001–005.

#### Scope

In: route map review, role matrix tests, session/logout/CSRF/XSS/file checks.

Out: introducing new roles or authentication providers.

#### Acceptance Criteria

- Each protected mutation has a server-side authorization check.
- Resource ownership and assignment checks are not inferred from hidden UI.
- No secret/credential appears in logs, JSP or repository.

#### Affected Files

- filters/interceptors/services/Controllers/REST, security tests,
  `docs/authorization-matrix.md`.

#### Verification

- Full security-focused tests and `mvn test/package`.

#### Foundation for Next Step

End-to-end smoke can trust role gates rather than testing only navigation.

#### Done Notes

Not started.

### 006_002 - End-to-end Must Have verification

#### Step Goal

Run the complete Lecturer → Faculty Head → Student/Group → Board/Score flow
with representative MySQL data.

#### Dependency

- `006_001`.

#### Scope

In: repeatable integration/smoke checklist, safe database reset/seed support,
all 12 acceptance criteria.

Out: deployment automation and CI/CD.

#### Acceptance Criteria

- Build and runtime results are recorded, including skipped environment checks.
- Failure cases for time windows, roles, group size, supervisor conflict and
  result privacy are exercised.

#### Affected Files

- integration tests/scripts, `docs/verification.md` and this summary.

#### Verification

- `mvn test`, `mvn package`, MySQL/Tomcat smoke.

#### Foundation for Next Step

The project has objective handoff evidence for the first release.

#### Done Notes

Not started.

### 006_003 - Documentation and handoff closure

#### Step Goal

Align README, docs, route map, open questions and phase summaries with the
implemented contracts.

#### Dependency

- `006_002`.

#### Scope

In: setup/config commands, known limits, open questions and verification notes.

Out: unrequested product expansion.

#### Acceptance Criteria

- A new agent can locate the current task and run documented checks.
- No copied source-project terminology or stack remains in project specs unless
  it is explicitly required by `REQUEST.md`.

#### Affected Files

- `README.md`, `docs/`, `.okf/phase/` and `AGENT.md`.

#### Verification

- Link/path scan with `rg`; `mvn test/package`.

#### Foundation for Next Step

Should Have work can be planned without disturbing Must Have contracts.

#### Done Notes

Not started.

### 006_004 - Phase closure review

#### Step Goal

Close Must Have only after all criteria and open blockers are explicit.

#### Dependency

- `006_001` through `006_003`.

#### Scope

In: reviewer/QA sign-off and final status update.

Out: new feature work.

#### Acceptance Criteria

- Phase status changes only with evidence.
- Any unresolved lecturer decision is clearly listed as a release limitation.

#### Affected Files

- this summary and verification records.

#### Verification

- Full suite and runtime evidence from preceding tasks.

#### Foundation for Next Step

Phase 007 may selectively implement Should Have polish.

#### Done Notes

Not started.
