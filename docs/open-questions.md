# Open questions carried from project memory

These questions are intentionally not resolved by the spec clone. A code task
may prepare a seam/configuration, but must not silently choose a mandatory rule.

| # | Question | Impacted area | Safe interim treatment |
|---|---|---|---|
| 1 | Can one topic be done by multiple groups? | topic registration, unique constraints, result | Keep registration context configurable; do not add a global topic uniqueness constraint |
| 2 | How are members invited/confirmed? | group workflow, notifications, membership status | Use explicit membership status/Service seam; do not assume auto-join or invite-only |
| 3 | What is the grading scale, components and rounding? | scores, final results, UI validation | Keep score policy configurable and block final calculation if policy is absent |
| 4 | Which topic types require a review board? | board creation, publication gate | Treat board requirement as a policy/configuration pending confirmation |
| 5 | What report file types and maximum size are allowed? | upload validation, deployment config | Require configured allowlist/size before enabling upload |
| 6 | Who approves each step if giáo vụ/trưởng bộ môn exists? | roles, permissions, filters | Use only roles in REQUEST; leave extension point for future approver |

## Change protocol

When an answer is confirmed:

1. Record the confirmed decision in `.okf/decisions/` and update
   `.okf/README.md`.
2. Update this table, `docs/domain-model.md`, `docs/workflows.md` and the
   affected phase task.
3. Add/adjust database constraints and tests in the same task.
4. Do not close the phase until acceptance criteria reflect the new policy.
