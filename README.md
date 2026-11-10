# SpendWise Backend — v6: monthly_trend + fewer unnecessary clarifying questions

## What changed from v5

**The bug:** "Give me a bar chart of monthly spendings" got a clarifying
question instead of an answer, and after answering, the reply was wrong
(₹0 across the wrong months). Root cause: the tool schema had no way to
represent "group spending by month over a multi-month span" — only a
single-period total, a category breakdown, a weekly trend, or a two-period
comparison. The model had no valid arguments to reach for, so it either
asked you, or improvised (querying specific calendar months that happened
to have no data).

**The fix, three parts:**

1. **New `query_kind`: `monthly_trend`** — groups spending by calendar
   month, returned as a `bar` chart. This is what "monthly spending" /
   "spending by month" naturally means, and didn't exist before.
2. **New range pattern: `last_N_months`** (e.g. `last_3_months`,
   `last_6_months`, `last_12_months`) — a proper multi-month span, parsed
   via regex in `DateRangeResolver`, alongside the existing single-period
   tokens.
3. **Explicit instruction against unnecessary clarifying questions** —
   the prompt now tells the model to default to `last_6_months` (or
   similar reasonable assumptions) rather than asking, unless the request
   is genuinely ambiguous about *intent*, not just a missing detail.

## Frontend

No changes needed. `chart.type` was always a plain string your Angular
code passes straight to Chart.js (`'bar'|'line'|'pie'`) — `monthly_trend`
just sends `"bar"` like `compare` already did. The response contract
(`replyText` / `render` / `chart`) is unchanged.

## What was verified this round

A full session reset meant I had to reconstruct this project from the
conversation rather than edit files directly — so every file was rebuilt
and re-verified from scratch, not just the changed ones:

- **Full-codebase compile check**: built stub versions of every Spring
  and Jackson class this project actually uses (correct method
  signatures, matching real Spring Boot 3.3 / Jackson APIs) and compiled
  all 18 real source files against them with `javac`. This is a stronger
  check than before — it catches wrong method signatures, type
  mismatches, and constructor mismatches across the whole app, not just
  per-file syntax. It caught one real bug before you ever saw this zip
  (a missing closing parenthesis in `ToolSchemas.java`).
- **`DateRangeResolver`'s new `last_N_months` logic**: tested standalone
  for 3/6/12-month spans and the "previous equivalent period" calculation
  used for comparisons — all correct against today's actual date.
- **Month label formatting**: specifically tested that it handles
  whatever JdbcTemplate hands back for a `DATE` column (which may be a
  `java.sql.Date`, not a `LocalDate`) without throwing — this was a real
  risk I caught and fixed by going through `toString()` → `parse()`
  rather than casting directly.
- **`humanizeRange`'s new regex branch**: tested for both `last_N_months`
  and `last_N_month` (singular) phrasing.

Still not verified: actual Spring Boot startup and real Anthropic API
responses, since this sandbox still can't reach Maven Central. If Maven
throws anything on your end, paste it and I'll fix the exact line.

## Test it

```
curl -X POST http://localhost:8080/api/chat -H "Content-Type: application/json" \
  -d '{"userId": 1, "message": "Give me a bar chart of monthly spendings"}'
```

Should go straight to an answer now (no clarifying question), with
`render: "chart"`, `chart.type: "bar"`, and monthly labels like `"Jun
2026"`.

---

# v7: Transactions, Categories, Budgets

Three new feature areas added for the org demo, alongside the existing
chat endpoint.

## New database table

`budgets` — one monthly limit per category per user (upsert, not
month-scoped rows; "spent so far" is always computed live against real
transactions, never stored/stale). Added to `db/schema.sql`.

## New endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/transactions` | Filtered list (category, date range, search) |
| GET | `/api/transactions/needs-review` | Just the flagged ones |
| PATCH | `/api/transactions/{id}/category` | Fix a needs-review item |
| GET | `/api/categories` | List with transaction + needs-review counts |
| PUT | `/api/categories/{id}` | Rename (blocked on shared/system categories) |
| POST | `/api/categories/merge` | Move every transaction from one category to another, delete the source |
| GET | `/api/budgets` | List with live spend, percent used, status |
| POST | `/api/budgets` | Create or update a category's limit |
| DELETE | `/api/budgets/{id}` | Remove a budget |

## What was verified

- **All new SQL tested directly against a seeded local Postgres** before
  being wrapped in Java — including the merge operation end-to-end
  (confirmed transactions correctly move to the target category and the
  source category is deleted), filter combinations for the transaction
  list, and the budget upsert + live-spend join.
- **Budget percent/status math tested standalone** — the 80%/100%
  warning/over thresholds at their exact boundaries, and the zero-limit
  case (no divide-by-zero).
- **Full 32-file backend compiled cleanly** against stub Spring/Jackson
  classes matching real method signatures (same technique used earlier in
  this project — it's caught real bugs before).
- **Not verified**: actual Spring Boot startup, since this sandbox still
  can't reach Maven Central. If Maven throws anything, paste it and I'll
  fix the exact line.

## Note on CORS

Added `PATCH` to the allowed methods in `WebConfig.java` (it only had
GET/POST/PUT/DELETE/OPTIONS before) — needed for the needs-review fix
endpoint. If you're merging this into an existing project rather than
using this zip wholesale, make sure that change carries over.

---

# v8: Duplicate detection, edit, delete

## Duplicate detection

Before inserting each transaction, `TransactionService` now checks for an
existing transaction with the same user, direction, amount, and exact
date. If found, the new transaction is still inserted (never silently
dropped) but flagged `needs_review = true`, and the fact passed to the
model's confirmation reply notes the possible duplicate so you're told
immediately, not left to discover it later in a wrong total. Tested
directly against Postgres (true-positive and true-negative cases) before
being wired into `TransactionService`.

## Edit and delete

- `PUT /api/transactions/{id}?userId=` — body: `{ amount, direction,
  description, categoryId, transactionDate }`
- `DELETE /api/transactions/{id}?userId=`

Both scoped to `user_id` in the SQL itself (not just the service layer),
so one user can never edit or delete another's data even if the ID is
guessed. Tested end-to-end directly against Postgres (insert → edit →
verify → delete → verify gone) before wrapping in Java.

## Verified

Full 32-file backend recompiled cleanly against the same stub
Spring/Jackson technique used throughout this project.

---

# v9: Import transactions from CSV / Excel

## New endpoint

`POST /api/transactions/import?userId=` — multipart file upload (`file`
field), accepts `.csv` or `.xlsx`/`.xls`.

## How rows are parsed

Column names are matched flexibly (case-insensitive), since real bank
exports vary:
- **Date**: `Date`, `Transaction Date`, `Txn Date`, `Value Date`
- **Description**: `Description`, `Narration`, `Details`, `Particulars`, `Remarks`
- **Amount**: either a single `Amount` column (optionally with a `Type`/
  `Direction` column, or a negative sign, to mean debit), OR separate
  `Debit`/`Withdrawal` and `Credit`/`Deposit` columns
- **Category** (optional): matched against your existing categories by
  name; no match → row is flagged `needs_review`, same as an
  under-confident chat entry

Date formats handled: `yyyy-MM-dd`, `dd/MM/yyyy`, `dd-MM-yyyy`,
`MM/dd/yyyy`, `d/M/yyyy`, `dd MMM yyyy`. Currency symbols and thousands
commas in amounts (`₹15,000`) are stripped automatically.

Rows that can't be parsed (bad date, no usable amount) are skipped and
reported by row number — the rest of the file still imports.

## Design choice: no LLM involved in import

Category matching during import is plain string matching (same
`CategoryMatcher` used elsewhere), not an AI call — a large import could
be hundreds of rows, and this keeps that free regardless of size,
consistent with how the rest of the app treats API credits. Unmatched
rows land in the same needs-review queue you already have a UI for.

## New dependency

`org.apache.poi:poi-ooxml` (5.2.5), added to `pom.xml`, for reading
`.xlsx` files — CSV parsing is hand-written (no dependency needed there).

## What was verified

- **CSV parser**: tested with real Java against quoted fields with
  embedded commas, escaped quotes, CRLF/CR/LF line endings, blank lines,
  and short rows — all correct.
- **Row normalization**: tested against every realistic column layout
  (single Amount, separate Debit/Credit, Amount+Type, negative-amount,
  currency symbols, six date formats) plus three intentional-failure
  cases (missing date column, empty debit/credit, unparseable date) — all
  correct.
- **Full import pipeline**: tested directly against Postgres — category
  matching (found/not-found), duplicate detection integration, and both
  insert paths (matched category vs. needs-review) all confirmed correct.
- **Full 33-file backend compiled cleanly**, including new stubs for
  `MultipartFile` and Apache POI's API — this caught one real gap (a
  missing `ResponseEntity.ok(body)` stub overload), fixed before packaging.
- **Not verified**: Apache POI's actual `.xlsx` parsing behavior at
  runtime, since I can't download the real library in this sandbox (no
  Maven Central access) — only stub-compiled against POI's API shape from
  memory. This is the one part of this feature I'd ask you to test first
  with a real Excel file, before the CSV path (which is fully verified).
- Also bumped `spring.servlet.multipart.max-file-size` from Spring's
  1MB default to 10MB in `application.properties` — otherwise a real
  bank statement export could fail with a confusing error.
