# Plan: "Agent Context Cost" report (rules of thumb)

Status: superseded. After several redesigns the separate report was judged not useful and was removed on
2026-10-04. Only its table survives, as the File Size report's "Large Files That Change Often" section
(`FileReadsForChanges`). Earlier status: implemented 2026-10-04 on branch sokrates/agent-context-cost (uncommitted) with the recommended choices: name "Agent Context Cost", no dollars, payback section kept.

## Outcome (2026-10-04)

The report was removed. Its "Files Read Most for Changes" table moved into the File Size report as
"Large Files That Change Often". Everything below is history.

## Last report design (2026-10-04)

The metric is the size of the files an agent opens per change, not a yearly total. The headline reads
"A typical change here touches a file of ~N lines (~X tokens to read) and edits ~M lines in it: agents
read about R lines for every line they change", followed by the share of changes in files over 2,000
lines and the split saving. The bars are "code" vs "changes (1y)" per file-size band. The table lists
the top files by lines × changes, with tokens to read, changes, lines edited per change, and lines read
per line changed. The co-change column and the mass-commit filter were dropped.

## Simplified (2026-10-04, superseded)

The multi-rule page was hard to understand, so it was cut to one model: **reading = file size ×
changes**. The page now has one headline (the total reading, the share in files over 2,000 lines, and
the saving if those were split), one bar (lines of code vs reading in five size bands) plus per-component
bars, and one table of the top files. Dropped: the per-commit reading, the working sets (co-change
partners are now a context column only), the payback magnitudes, and the AI co-author split. The
sections below describe the earlier designs. Follow-up: every commit counts for the file reads
(the 30-file mass-commit filter applies to the co-change column only), the metric is called "file
reads", and the headline is a thought experiment ("Had agents made the past year's changes …").

## Revision (2026-10-04)

After review the page was restyled like the other risk reports: five file-size bands
(350 / 1,000 / 2,000 / 5,000 lines) and five commit-reading bands (1k / 2k / 10k / 50k lines) on the
risk colours, stacked bars for code vs. changes, per logical component and per commit (all vs AI
co-authored). The headline tiles and rule callouts were dropped, and the rules of thumb moved into the
about section, with each section keeping its rule as a subtitle. The split by AI co-authored commits was
removed again: the report does not use co-author data.

## Goal

A per-repository report that estimates how much an AI coding agent has to *read* to maintain the
codebase, framed as **rules of thumb** (bands, rounded ranges, orders of magnitude) to avoid a
false-precision feel. It answers: where do agent token costs concentrate, and which refactorings
(file splits) would pay back.

Inputs are limited to data we trust:

- file size (main LOC per file);
- change history: which main files each commit touched, and files changed together
  (the same 365-day `filePairsChangedTogether` data as the Temporal Dependencies report).

Deliberately **not** used: heuristic static dependencies (regex-based, unreliable), McCabe and
duplication (no evidence linking them to agent cost — see research). The report says so in a
"what this does not cover" note.

No dollar amounts by default (prices change fast and suggest precision). Tokens = LOC × 7–14,
shown to one significant figure ("~40k–80k tokens").

## Research basis (summary)

- **Edwards-Alexander, "The Economic Benefit of Refactoring"** (martinfowler.com, 2026-07-30):
  17,155-line Rust file split in 15 steps (largest file → 3,695 lines, total LOC ~unchanged);
  input tokens for a fixed change 159,564 → 27,360 (−83%), output ~flat (1,705 → 2,113). Tokens
  stayed flat until the largest file shrank, then dropped off a cliff. Refactoring cost upper
  bound ~5M tokens → payback ≈ 38 changes. Sanity check: 17,155 LOC × ~10 tokens/line ≈ 170k.
- **Read windows**: Claude Code Read ~2,000 lines / ~25k tokens; Cursor 250 lines; SWE-agent
  100-line window beat whole-file view (18.0% vs 12.7% on SWE-bench Lite, arXiv 2405.15793).
  Spotify "Portal" (2026): blocking >350-line reads cut Claude Code tokens ~90% (vendor-reported).
- **Coupling / files touched**: SWE-bench Pro (arXiv 2509.16941): success declines sharply with
  files touched; Sonnet 4 failures 57% context overflow, 34% endless file reading. "Working Set /
  Coherence Debt" (arXiv 2608.16630): unrelated code embedded *in required files* cut success to
  1/6; placement matters more than volume.
- **Code quality**: SonarSource minimal pairs (arXiv 2605.20049, 660 Claude Code trials): clean
  code −7% input tokens, −34% file revisits, same pass rate; extracting methods into more files
  can *raise* reads. CodeScene (arXiv 2601.02200, 2608.18645): low Code Health +44% input tokens
  (single-shot Java/C++); refactoring-break advantage not significant for frontier Claude models.
- **Noise**: 2.5–30× token variance on the same task (arXiv 2604.22750); failed runs ~4× cost
  (SWE-Effi, 2509.09853); input:output ≈ 50–200:1; cache reads 85–99% of input.
- **Gaps**: no study regresses agent tokens on repository static metrics; no empirical work on
  duplication's cost to agents; no McCabe-of-existing-unit vs agent-cost study. Effects are
  non-linear: large gains only at extremes (files far beyond one read window).

## Report sections (one rule each)

Each section: the rule in a callout, a "why we believe it" line (source + evidence strength),
then the numbers.

1. **"An agent reads what it changes."** Main code banded by read window: ≤350 lines
   (comfortable), 350–2,000 (one read), 2,000–5,000 (several reads + re-reads), >5,000
   (monolith). Per band: share of LOC **and share of the past year's file changes**. Headline:
   "X% of changes land in files larger than one read window."
2. **"A commit tells you the minimum reading."** For every commit in the past 365 days: current
   LOC of the main files it touched, banded ≤2k / 2k–10k / 10k–50k / >50k lines (≈ 20k / 100k /
   500k tokens). A lower bound with no coupling assumption. Split by AI co-authored commits when
   Sokrates detects any.
3. **"Changes travel together."** Per changed file, co-change partners = files that changed with
   it in ≥30% of its commits in the window. Working set = file + partners (likely extra reading
   beyond section 2).
4. **"Fix the hotspots first."** Top 20 main files by changes-in-the-past-year × working-set LOC:
   file (source-viewer link), LOC, read-window band, changes/yr, top 3 partners with %, working
   set as a token range, and a "split candidate" flag (>2,000 LOC and changed often).
5. **"Payback is counted in changes."** Per split candidate, a rule-of-thumb line: splitting to
   read-window size saves ~N tokens per change → pays back a refactoring after roughly tens /
   hundreds of changes (orders of magnitude), anchored on Fowler's ~5M refactor / ~132k saved.

**Reliability filter:** commits touching >30 main files are ignored for both coupling and read
sets (mass renames/reformats create fake coupling); the report states how many were ignored.

Available only with main code and git history; placed after Temporal Dependencies in the sidebar.

## Implementation steps

1. `reports/core/AgentContextSummary` — pure, like `HealthSummary`, from `CodeAnalysisResults`:
   commit → main-files map from the main files' `FileModificationHistory` within 365 days;
   partners from `getFilePairsChangedTogether()` (window = `maxTemporalDependenciesDepthDays`,
   default 365), counting only shared commits that pass the large-commit filter. The partner
   share must use **in-window** commit counts: `commitsCountFile1/2` on a pair are all-time.
   Constants for bands, 7–14 tokens/line, 30% share, 30-file limit — no config keys in v1.
2. `AgentContextSummaryTest`: bands, large-commit filter, in-window partner share, rounding,
   no history → no report.
3. `AgentContextReportGenerator` → `AgentContext.html`: `RichTextReport` with `startDataTable`,
   escaping text primitives, `viewerFileHref` links; add hotspot files to `ReferencedFiles.of`.
4. Wiring: register in `BasicSourceCodeReportGenerator`; `REPORT_ENTRIES` row
   (`a.mainExists && a.history`) + `NAVIGATION_ICONS` in `ReportFileExporter`; navigation set in
   `ReportsCommands`.
5. Verify: `analyze` on Sokrates itself + one large repository (plausibility); Playwright pass
   (light/dark/colour-blind); full `mvn install` incl. `RichTextReportSinkEscapingTest` and
   `ReportHtmlEscapingTest`.
6. Docs: CHANGELOG entry, CLAUDE.md "Per-repository report notes", sources listed in the report.

Later (not v1): Highlights tile ("% of changes in files above the read window"), `data/` export,
landscape roll-up, config keys if asked for, calibration against real agent token telemetry.

## Open decisions (recommendation first)

1. Name: **Agent Context Cost** / AI Change Cost / Agent Readiness.
2. Dollars: **none** / one illustrative line with a configurable price.
3. Section 5 (payback): **keep** / drop (it rests on a single article).
