# Code Smell Rule Template

Use this template to convert any code smell description (e.g., from refactoring.guru)
into a structured rule consistent with this skill's format.

---

## Prompt

Paste the following into any AI model, replacing the placeholder at the bottom:

---

```
Convert the following code smell description into a structured skill rule.

Use EXACTLY this format (include only sections that have content — IGNORE IF is optional):

#### [S-CODE] — [Smell Name]
```
IF: [detection condition, be specific and measurable where possible]
THEN: flag as [WARNING | CRITICAL]

[Add a second IF/THEN block if there are two severity tiers]

WHY: [Root cause — why this smell happens and why it matters. 2-4 lines.]

SIGNAL: [One concrete, actionable heuristic a developer can apply in practice
         to confirm whether the smell is present. Start with an action verb.]

FIX (choose based on [context]):
  1. [Refactoring technique] — [when to apply it]
  2. [Refactoring technique] — [when to apply it]
  3. [Refactoring technique] — [when to apply it]

PAYOFF:
  - [Benefit of fixing]
  - [Benefit of fixing]

IGNORE IF: [when NOT to fix this smell — one line]
```

Rules:
- Keep everything inside a single fenced code block (triple backticks)
- Use imperative language in FIX steps
- SIGNAL must be concrete and specific — not a rephrasing of WHY
- IGNORE IF is only included if the source material has a "when to ignore" section
- Preserve the exact keywords: IF, THEN, WHY, SIGNAL, FIX, PAYOFF, IGNORE IF

---

Input smell description:
[PASTE THE REFACTORING.GURU CONTENT HERE]
```

---

## Section Reference

| Section | Required | Rule |
|---|---|---|
| `IF / THEN` | ✅ Yes | Must be measurable (line count, param count) OR behavioral (pattern description). Add a second block for a second severity tier. |
| `WHY` | ✅ Yes | Explain root cause, not just consequence. 2–4 lines. |
| `SIGNAL` | ✅ Yes | One concrete heuristic to confirm the smell. Must start with an action verb: "Delete…", "Try renaming…", "Count…" |
| `FIX` | ✅ Yes | Ordered list — most common/impactful fix first. Include when-to-apply context per item. |
| `PAYOFF` | ✅ Yes | Direct benefits only. No marketing language. |
| `IGNORE IF` | ⚡ Optional | Include only when the source explicitly says when to ignore the smell. |

---
