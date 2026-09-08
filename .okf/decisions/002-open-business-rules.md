# ADR 002: Unresolved Business Rules Stay Unresolved

Status: Accepted

## Context

The project memory lists six questions that need lecturer confirmation: multiple
groups per topic, member invitation, grading scale/rounding, board-required
topic types, report file limits, and approver roles.

## Decision

Do not hard-code those answers as mandatory domain rules. Until confirmed:

- keep them as explicit configuration, nullable fields or extension points;
- show a clear TODO/open-question note in the owning phase;
- avoid database constraints that prevent either plausible interpretation;
- do not claim the corresponding acceptance criterion is complete.

## Consequence

Generated code must fail safely and ask for a confirmed policy at the service
boundary rather than silently choosing a product rule.
