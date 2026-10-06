# Release notes

User-visible behaviour changes, newest first. `:latest` of the Docker image and the `master` branch carry
everything listed under the most recent date.

## 2026-10-06

### Ease of change: a soft weakest-link cap

The weakest-link cap was a hard clamp at the weakest code sub-score + 4, so every repository with one zero sub-score —
typically no test code at all, also very large files or heavy duplication — got exactly 4.0, whatever the rest of its
code looked like (in one 122-repository landscape, 44 had a Human score of exactly 4.0, mostly small samples without
tests). Now only half of what the mean rises above that level counts: the weak spot still pulls the total down
clearly, but repositories keep their differences (in that landscape: none at 4.0 for people, 5 for AI; median 4.9 /
4.4). Scores change on the next analysis; "capped by" reads "held down by". A custom framework can set `capStrength`
(0–1, default 0.5; 1 = the old hard cap).

### Ease of Change* chart: an axis for the bulk of the repositories

The chart's size axis no longer starts at the decade below the smallest repository, where a few tiny repositories left
much of the chart empty. It now starts near the bulk of the repositories (the 10th percentile by size, less about half a
decade) and still reaches the largest, with 1-2-5 ticks (2K, 5K, 10K, …); smaller repositories are drawn as triangles at
the left edge, with their size on hover.

### "Ease of change" instead of "maintainability score"

The Human and AI maintainability scores are now labelled **ease of change** — how easily people and AI agents can
understand and change the code — because "maintainability" read as a verdict on code quality, while the ratings also
(deliberately) reward less code: other things equal, a smaller code base is easier and cheaper to change. At a Glance
shows the grade only — the A–E scale with the current grade large, labelled "ease of change for people*" / "for
AI*" (the number is in the hover text and on Highlights); the Highlights tiles are both "Ease of change*" with the audience in
the caption ("for people · biggest drag: …", "for AI · ~N lines read per change"), the Highlights card is "Ease of change*" and the landscape tab "Ease of Change*" (Human / AI toggle and
columns). The landscape sidebar now lists it right after Overview, as the tab bar does
(it had stayed after Metrics). Unchanged: the numbers, the configuration key `analysis.maintainabilityScores`, the
landscape option `showMaintainabilityScores` and the metric ids `MAINTAINABILITY_SCORE_HUMAN` / `_AI`, so existing
goals and controls keep working. The "Changes in large files" tile is now "Large-file changes", so its title fits.

### Maintainability scores show their coverage

A score left without some of its sub-scores (their analysis did not run: no units for a language, no git history,
duplication skipped) now says so: "measured on 9 of 10 sub-scores (not measured: Duplication)" on the Highlights card
and tile, "(9/10)" on the At a Glance block, and in the landscape's Scores* tab a "9/10" mark in the table and a hollow,
dashed dot on the chart. Fully measured scores look as before; analyses from before this show no coverage.

### Landscape: Repositories › Scores*

A new Scores* tab right after Overview (also in the sidebar; renamed Ease of Change*, see above). A chart plots every repository as a dot: lines of main
code (log scale) across, its Human or AI maintainability score (toggle) up, colored by grade over the A–E bands; hover
shows the score and its biggest drags, a click opens the repository's Highlights. Below it, a sortable table lists the
Human and AI scores, main lines of code and lines read per change. Both follow the search box. The score columns moved
here from the Metrics tab. Shown when the scores are on (`showMaintainabilityScores`) and at least one repository has
them.

### Why each maintainability sub-score matters

On Highlights, every sub-score row of the Maintainability scores card opens on a click and explains why that
sub-score matters for people and for AI agents (the column's own audience first). A custom framework can write its
own texts per sub-score (`whyHuman`, `whyAi`); the rows are closed by default, so the card looks as before.

### Your own maintainability score framework

`analysis.maintainabilityScores` takes a score framework of your own: with the explicit `"useCustomFramework": true`
(default `false`), `customFramework` sets which sub-scores count — built-in ones by key, or any analysis metric by
id — with their anchors, labels and Human and AI weights, plus the weakest-link margin, the sub-scores that may not cap
the total and the grade thresholds. The Highlights card and the landscape's score tooltips note when a repository uses
its own framework. Without the switch nothing changes; unusable entries are skipped with a warning in the log.

### Estimate assumptions from config.json; en-US decimal fields

A repository's `config.json` can set the starting assumptions of the At a Glance "Rule-of-thumb estimates" and the
AI Cost Estimator in `analysis.estimateAssumptions`. They are applied only with the explicit `"enabled": true` (default
`false`, so existing configurations change nothing). Viewers can still edit the values in the page; with configured
values their edits are remembered per configuration, so a changed config shows up. Landscapes keep the built-in
defaults. The decimal assumption fields on both pages are now read and shown in en-US format (`0.5`, `10,000`), as
in the rest of the reports, whatever the browser's language; an invalid value is outlined in red.

### Human and AI maintainability scores*

Every repository gets two scores from 0 to 10 (grades A–E): how easy the code is for **people** and for **AI coding
agents** to understand and change. They weigh the same ten sub-scores differently: volume, duplication, unit size and
complexity, file size and complexity, test code, change entropy (how scattered a past-year commit is
over components), context per change (the lines of main code a commit touched, what an agent reads) and knowledge
spread (who makes half of the commits). People are weighted more on complex logic and knowledge held by few, and AI
more on large files, scattered changes, lines read, duplicates and tests. The total is a weighted geometric mean, capped
at the weakest code sub-score + 4. At a Glance shows both scores next to the size cards (linking to the breakdown), the
Highlights tab shows two tiles and a breakdown ordered by *drag* (what each
sub-score costs the total), the landscape's Repositories › Metrics has two sortable columns, and the scores are metrics
(`MAINTAINABILITY_SCORE_HUMAN`, `MAINTAINABILITY_SCORE_AI`, `AI_CONTEXT_LINES_PER_CHANGE`) for goals and controls.
Weights can be set in `analysis.maintainabilityScores` (docs/configuration.md); `show: false` there, or
`showMaintainabilityScores: false` in a landscape config, hides the scores but keeps them in the data. Nothing else changes; existing
analyses show the scores after their next `generateReports`, and a landscape shows "-" for repositories analyzed before.

### AI Cost Estimator*: an educational tool

The estimator page opens with "An educational tool, not an exact estimate: see how AI coding costs build up and where
you can control them.", and its sidebar subtitle (repository and landscape) says
the same.
All the page's texts (intros, How it works, assumption explanations, table descriptions, notes) were tightened.

### AI Cost Estimator*: "base" AI cost

The headline cards read "Estimated base AI cost" (and the chart "Estimated base cost per month/year"), and the note under
the intro says the amounts are a base to see where the levers for using AI effectively are, not a budget: the agent's
tokens only, without people's time, discovery, planning, review, integration or infrastructure.

### AI Cost Estimator*: refactoring savings

The Historical tab has a **Refactoring savings** card: what files no larger than a "max file size" (default 400
lines; its own Assumptions group) would have saved. Every file a session reads counts at most that many lines (a
1,200-line file counts 400, a 330-line file 330; same files, same commits). That saves read tokens, and a
**carry-over** share (default 25–75%) of the rest of the cost is cut by the same share of reads (fewer steps, less tool
output), so the saving lies between the read tokens alone and the whole cost cut by the read share. The card shows the
share saved (P10–P90), the amount, how many edited files are larger, and the lines read before and after; the cost of
the refactoring is not included. For Sokrates at 400 lines: about 12%. The repositories' data now carries each edited
file's read size per session and the current size of the edited files, so existing analyses show it after they are
re-analyzed.

### AI Cost Estimator*: uncertainty in the naive rebuild

The Naive Rebuild has an **uncertainty** level: how sure we are that what gets built delivers the intended value. *Low*
(the default, 0–10% of built work redone) is a migration of the same functionality; *medium* (25–50%), *high* (50–75%)
and *very high* (75–90%) redo a share of the built work: changed after feedback, thrown away, rebuilt. A redo can miss
again, so the work built is the kept code ÷ (1 − the share redone), in "Redoing" sessions that read what they replace;
fixes now scale with everything built. The level sets an editable "work redone" range ("custom" when edited), and its
explanation shows how much the history redid in hindsight (lines added ÷ lines today; Sokrates: about 37%, like
medium), from the analyzed scopes' total lines now stored in `aiCostEstimator.json`.

### AI Cost Estimator*: Historical and Naive Rebuild tabs

The AI Cost Estimator (repository and landscape) has two tabs. **Historical** is the estimate so far, from the git
history. **Naive Rebuild** estimates what an agent would spend writing the main code again the way a good team works:
small, frequent commits of code with its tests, fixes after debugging, and refactoring along the way. Its one measured
input is the lines of main code; test code (by default as much as main code), build & other code (5–15% of main),
session size, reads, fixes per 1,000 lines and the share rewritten are assumptions in their own "Naive rebuild" group.
Every session goes through the same token model as the history. The tab shows the cost (P10–P90) next to the history's,
per 1,000 lines of main code, by kind of work (building, fixing, refactoring), by code (main, test, build & other) and,
in a landscape, by repository; its Model diagram shows the rebuild. The analysis data (`aiCostEstimator.json`) now also
carries the lines of main code, and a landscape counts every repository in the rebuild, also those analyzed by an older
version. A repository without git history opens on the Naive Rebuild tab.

### AI Cost Estimator*: a simpler model, shown as a diagram

Every assumption now has a one-line explanation under it in the Assumptions panel, and "How it works" is shorter.

The model is simpler: every agent step re-sends the context, so a session costs about steps × average context (mostly at
cache prices) plus its output. Its assumptions went from 14 ranges to 10: one read amplification instead of one per task
type (the task type now only groups the results), no separate exploration reads, one write overhead for every changed
line, and "output per step" instead of reasoning. Those barely moved the total (each by 1–6% across its range). One
assumption was added where git hides real work: a debugging factor (1.5–4×) multiplies the steps of fix tasks (a small
fix often follows reproduce, run and inspect loops), and tool output per step now goes up to 3,000 tokens (test runs,
build logs, stack traces). Together these raise this repository's estimate by about half, mostly in fix tasks.

The AI Cost Estimator (repository and landscape) has a "Model" button next to "Assumptions" that opens its model as a
left-to-right diagram (at its natural size, the panel scrolls sideways; the arrows say what happens along them): how the git
history becomes tasks and sessions, how the session inputs (lines read, edited and new lines, files) combine with every
assumption into read, write, input and output tokens and the cost, and how the simulation turns that into the P10–P90
results. The numbers in it follow the current filter and assumptions. It is drawn with Mermaid from cdn.jsdelivr.net,
like the reports' other diagrams. "How it works" ends with a further-reading link to Martin Fowler's site.

### Landscape: AI Cost Estimator*

Landscapes have an "AI Cost Estimator*" page too (sidebar: Insights): the same estimate over the tasks of all
repositories, with a repository column, a "By repository" table and the repository in the search and the CSV. Every
analysis (also with `-dataOnly`) now stores its estimator data as `aiCostEstimator.json` in `data/data.zip`, which the
landscape reads; repositories analyzed by an older version have none and are left out until they are re-analyzed. Big
landscapes keep the newest 50,000 tasks.

## 2026-10-05

### Rule-of-thumb estimates: AI token sections removed

The "AI token reads" and "AI write tokens" sections of the rule-of-thumb estimates (repository and landscape At a
Glance) are gone: AI token costs now have their own page, the AI Cost Estimator. The block keeps the rebuild value
and the maintenance effort (and, in a landscape, the repositories choice); saved values of the removed inputs are
simply ignored.

The AI Cost Estimator counts only the analyzed scopes (main, test, build & deployment, other; generated code, files in
no scope and unanalyzed extensions are left out, while deleted files of analyzed extensions still count), and it reads
each edited file's total lines, comments and blank lines included, as an agent reads the whole file (it used the lines
of code before, which leave those out). The task-type and task tables (and the tasks CSV, `read_lines`) show these
lines read next to the churned lines, the model's two line inputs. The page is now labelled "AI Cost Estimator*": the
asterisk marks it as an experimental heuristic.

The AI Cost Estimator's top is leaner: a filter, a period choice (with a custom range) and an "Assumptions" button that
opens the prices and priors in groups.

### AI Cost Estimator (new Explorers page)

The repository report has a new **AI Cost Estimator** page (sidebar: Explorers): what the history would have cost if
an AI coding agent had written it. Commits are grouped into tasks (by ticket id such as ABC-123 when the messages carry
them, otherwise consecutive commits of one author at most a day apart touching overlapping files), tasks are split into
sessions of at most 10 files or 400 changed lines, reads come from the current size of the edited files (at most 2,000
lines each, times a read amplification per task type), writes from the churn (new files cheaper per line than edits,
deleted files free), plus agent steps, tool output, reasoning and prompt caching. Every assumption is an editable range;
a seeded simulation gives P10–P90 for the total, per task type, per month, per 1,000 churned lines and per
developer-day (with a reference to compare to, default $13). Bot commits, commits touching more than 300 files,
lockfiles, vendored and generated paths and single file changes over 3,000 lines are left out. The task list can be
downloaded as CSV (with the commit shas) for a replay pilot. Nothing changes in the existing reports or data; the page is
written next to the commits explorer (`explorers/ai-cost-estimator.html`) and only needs the git history.

### Rule-of-thumb estimates: AI write tokens

The rule-of-thumb estimates (repository and landscape At a Glance) have a new "AI write tokens" section: it prices the
churn (lines added + deleted) of the past 30 days, 3 months or year (the default) as if an AI agent wrote every changed
line, with the same tokens per line, write tokens at 3 times the read token price, and a hidden loop factor (20 by
default) for what an agent writes but does not keep (reasoning, drafts, retries, rewrites). It shows the tokens, the cost
for the period and per month, and a per-scope table; it is hidden when there is no git history.

### Landscape At a Glance: rule-of-thumb estimates

The landscape's At a Glance page ends with the same "Rule-of-thumb estimates" section, over the summed lines of code of
its repositories. A "repositories" choice selects which repositories count, by their latest commit: active in the past
30 days, 3 months, 6 months, past year (the default), 2 years, or all repositories (repositories without git history
count only there).

### At a Glance: rule-of-thumb estimates

At the bottom of a repository's At a Glance page, a new "Rule-of-thumb estimates" section (closed by default) turns the
lines of code into rough estimates you can adjust: the rebuild effort and value (about 10,000 lines of main code per
man-year, a cost per man-year), the yearly maintenance effort (about 15% of the rebuild value per year), both from the
main code only, and the AI token reads of all code (tokens per line, price per million input tokens, context windows,
full reads per month), in total and per scope (main, test, build & deployment, generated, other) in a table.
While closed, it shows the headline numbers in one line. Every assumption is an input; your values are remembered in the browser for every report, with a reset to the defaults.
These are rules of thumb, not measurements.

### Commits explorer: churn vs. read size timeline

The commits explorer has a new "show timeline" button (after "show files map"; off by default, and the
choice is remembered). Just above the commit table, the timeline shows, per day, week or month (depending on the period shown), the
churn (lines added + deleted) and the read size* of the listed commits, as two rows of bars on one time
axis with the latest period on the left. Each row has its own scale, since the read size is usually much
larger than the churn; hovering over a period shows both numbers and their ratio, and clicking it shows
only that period's commits. The timeline follows the search, the date range and "hide bots".

### Commits explorer: filter by date

The commits explorer can now show only the commits of a period: pick a "from" and/or "to" date (both
inclusive) under the search box, or a preset (past 30, 90 or 180 days, past year; these end at the
latest commit in the history, not today). The date range works together with the search and "hide
bots", and "select all matching" selects only the commits in the range.

### Commits explorer: read size per commit

The commits explorer has a new "read size*" column: the lines of code that the files a commit changed
have now, summed. It is an indication (hence the asterisk): roughly what reading those files takes, for
example as the token reads of an AI agent working on such a change. A file that is deleted, or not
analyzed now, counts 0. The column sorts like the others; hover over it for the explanation.

### Landscape AI Insights link to the repository reports

In the landscape's AI Insights tab, a finding, a scanner and a repository now open the repository report's
own AI Insights pages: a finding opens its scanner's page in the report, with the finding expanded. Before,
they opened the standalone sokrates-skills explorer (`reports/ai-insights/index.html`). Repository reports
generated before the report rendered the findings itself keep the old links. A finding id with a space now
links correctly (it was encoded as "+", which the explorer did not decode).

### Report sidebar: sections open and close

In the repository and landscape reports, each section of the sidebar (At a Glance, Explorers, Analyses,
AI Insights, …) now opens and closes with a click on its title. All sections are open by default; the
closed ones are remembered in the browser and stay closed on every report page. The section of the
page you are on is always shown, and the sidebar search also finds pages in closed sections.

### AI Insights in the repository report

When a repository's `reports/ai-insights/` holds findings of the sokrates-skills AI scanners, the
repository report now shows them itself: the sidebar gets an "AI Insights" group (Overview, Attention
Items and the basic scanners) and an "AI Deep Dives" group (the evaluative scanners), between the
analyses and Index. Each item opens a page in the report's style that follows the light/dark theme, with
the same search, filters and evidence as the sokrates-skills explorer, but without a second menu. With
`-postAnalysis` / `-ai`, the reports are generated once more after the agent changed the findings, so the
new findings show up in the same run. For `analyzeGitRepo` and landscapes, the findings of earlier runs
are kept: before, a re-run that skipped the agent (unchanged repository) replaced `reports/` and lost them.

What stays the same: sokrates-skills still writes its standalone `reports/ai-insights/index.html`. What to check: a custom tab added with
`addCustomTab -label "AI Insights*" -iframeLink "../ai-insights/index.html"` (as the skills used to
suggest) is left out while the report shows the findings itself; you can remove it from `customTabs` in
`config.json`.

### Landscape: "At a Glance" and a new "Structure" page

In the landscape report's sidebar, Overview is now called At a Glance, as in the repository report, and
a new Structure page follows it. Structure holds the "Repositories Size Distribution" and the
repositories circles chart (size = main lines of code, color = main language), which moved there from
At a Glance. Activity now comes before Repositories, and Sub-Landscapes has a new icon so it doesn't
share one with Structure. Links to `index.html#overview` still open At a Glance; the configured
`iFramesAtStart` / `iFrames` stay on At a Glance.

### Files and units explorers show files like the Highlights tab

In the files and units explorers, a file now shows as on the Highlights tab: the file name first, with
the folder (ending in "/") in smaller grey text underneath, and the full path on hover. Before, the
folder came first and the two explorers styled the name differently. The files explorer's name link
no longer has the "↗" marker; the unit name links in the units explorer keep theirs.

### Temporal dependencies: very large commits are left out, and the analysis uses much less memory

Commits that touch more than 100 main files are no longer counted in the temporal (changed-together)
file dependencies. A commit with N files produces N×(N−1)/2 file pairs, so one mass edit, such as
reformatting or a license header update, could add millions of pairs and gigabytes of memory while
saying little about which files are coupled. The limit is the new `analysis` setting
`maxFilesPerCommitForTemporalDependencies` (default `100`; `0` or less turns it off). The log says
how many commits were left out per window.

Independently of the limit, building the file pairs and the file dependency force graphs now needs far
less memory (in a test with 4.5 million pairs, peak heap fell from about 6.4 GB to about 1.1 GB), and
pairs with the same number of shared commits are listed in a fixed order instead of a different one
on each run.

What stays the same: the windows, the reports, the `temporal_dependencies*.txt` exports and the force
graphs (still the first 10,000 links, with the same counts). What to check: in a repository with
commits that touch more than 100 main files, the pair counts and the component dependencies in the
temporal views drop. Set `maxFilesPerCommitForTemporalDependencies` to `0` to get the old numbers.

## 2026-10-04

### Commits explorer: the columns on the right stay visible; one-line page titles

In the commits explorer, the message, author and co-authors columns now share the width the other
columns leave, cut with "…" and shown in full on hover. Next to the files map, the files and lines
columns used to scroll out of view. In repository and landscape reports, the page title in the header
(for example a long name on the Overview / At a Glance) stays on one line, ending with "…" and shown
in full on hover.

### Units explorer: long file and unit names are cut

The units explorer gets the same treatment as the files explorer: file, folder and unit names longer
than their column (420px) end with "…" and show in full on hover. Before, folders were cut at 40
characters and unit names at 50, while file names were not cut at all.

### Files explorer: long file names are cut

In the files explorer, a file or folder name longer than the column (420px) ends with "…", and the full
name shows on hover, so a very long name no longer pushes the columns on the right out of view. The
folder line now also shows for short folders: before, it was empty unless the folder was over 50
characters.

### The Overview shows the configured logo

The header of the repository's At a Glance and of the landscape's Overview now shows the configured
`metadata.logoLink` next to the configured name. Without a logo, or when the image does not load, the
page keeps its usual icon. The sidebar's icons do not change.

### Hide and show the sidebar

On a wide screen, the sidebar of repository and landscape reports can be hidden with the small button
at its top right; the menu button at the top left brings it back, and so do ⌘K / Ctrl+K and `/`.
The choice is remembered in the browser for all reports. On narrow screens the sidebar stays a drawer,
as before.

### Landscape sidebar: the repositories and contributors lists' tabs

Under **Repositories**, the landscape sidebar lists the tabs of the repositories list: Overview, Churn,
Commits Trend, Contributors Trend, History, Metrics and, when configured, Features of Interest. Each
opens just that list (`repositories.html?tab=…`), full height, without the rest of the Repositories tab.
The links can be bookmarked (`index.html#repositories-list/churnTrend`), and `repositories.html?tab=…`
also works on its own. **Contributors** has the same kind of sub-items for the contributors list:
Recently Active, All Time and, when there are bots, Bots (`contributors-report.html?tab=…`).

### The Overview is titled with the configured name

The Overview of a repository report ("At a Glance" in the sidebar) and of a landscape report now shows
the configured `metadata.name` as its page title, and as the browser tab title. The name line above the
header is hidden there so it doesn't repeat. Without a name, the title stays "At a Glance" / "Overview".
The sidebar labels don't change. Going back in the browser to the report's own URL (no `#tab`) now
reopens the Overview instead of staying on the last tab.

### Landscape reports get the sidebar

The landscape report now has the same left sidebar as the repository reports. Its tabs are sidebar
items in groups: Landscape (Overview, Sub-Landscapes, Repositories, Activity, custom tabs), People
(Contributors, Teams, Topology), Insights (AI Insights) and Index (Data). Each group shows only the
tabs that exist. There is no search field, since there are only a few tabs. The landscape's name sits above the
page header and the page title follows the open tab. The counts that were in the tab labels
("Repositories (12)") are now in the page subtitle. The landscape's links moved into the Overview tab,
and the "generated by" line moved into the sidebar footer. The tab URLs (`index.html#repositories`,
…) are unchanged, so existing links keep working.

### Search filters the sidebar, no popup

The **Search** field at the top of the report sidebar now filters the sidebar's own items in place as
you type: groups without a match are hidden, Enter opens the first match, and Esc clears. ⌘K / Ctrl+K
or `/` still put the cursor in it. The popup command palette, which also searched the current page's
tabs and section titles and offered theme, colour and print commands, is gone. Those commands remain
on the theme button, the colour-blind checkbox and the browser's print.

### File Size report: large files that change often

The **File Size** report has a new section, **Large Files That Change Often**, after the longest files.
To change a file, an AI coding agent (or a person) has to read it, so a big file that changes often is
paid for on every change, while a big file nobody touches costs nothing. The table lists the 20 main
files read most for the changes of the past year (lines × changes, every commit counted) with:
- their lines and the tokens to read them (lines × 7 to 14, rounded);
- their changes;
- the lines edited per change (the file's lines added and deleted divided by its commits over the whole
  history);
- the lines read per line changed.

**Highlights** gets two matching parts:
- a **Changes in large files** tile: the share of those changes that touched a file over 2,000 lines
  (good below 5%, watch below 15%);
- a short **Large files that change often** list below "Where to look first", with up to 5 such files.
  It appears only when there are such files, as otherwise it would repeat the hotspots.

The section, tile and list are configured in `analysis.fileReadsForChanges`: `enabled`, `windowDays`
(365), `maxFiles` (20), `largeFileLines` (2000), and `tokensPerLineMin`/`tokensPerLineMax` (7/14). New configurations get these keys
from `init`; older ones use the defaults (`updateConfig` adds them). The section is omitted without git
history. Sortable report tables now honour a `data-sort` attribute
on a cell, so rounded values sort by the underlying number.

(A separate Agent Context Cost report, briefly on `master`, was removed in favour of this section.)

### New File Complexity report

A **File Complexity** report (`FileComplexity.html`, in the sidebar under File Size) classifies main
code files by the **sum of the McCabe indexes of their units**, weighted by lines of code, in five
bands: 1-25, 26-50, 51-125, 126-250 and 251+ (five times the unit complexity bands, as a file is a
group of units). It shows the overall profile, the profile per extension and per logical component,
the 50 most complex files (with complexity per 100 lines and the most complex unit, to tell a large but
plain file from a dense one), and two circle views coloured by file complexity. The profile is also in
`analysisResults.json` (`filesAnalysisResults.overallFileComplexityDistribution`, per extension, per
logical decomposition) and in the metrics list (`*_FILE_COMPLEXITY_COUNT` / `*_FILE_COMPLEXITY_LOC`).

Only files with units are classified: files of languages without unit analysis, or without
functions, have no complexity to measure and are left out instead of counted as simple.

What to check: the bands come from the new `analysis.fileComplexityThresholds`. The older
`analysis.fileConditionalComplexityThresholds` key, which `init` wrote with the unit bands but nothing
ever read, is now ignored; you can delete it from your `config.json`. To change the bands, add
`fileComplexityThresholds`.

## 2026-10-02

### Path patterns match the path below the source root (behaviour change)

A filter's `pathPattern` — in `ignore`, the scope rules (`test`, `generated`, `buildAndDeployment`,
`other`), logical decomposition filters and concerns — is now matched against the file's path **below
the source root, with a leading `/`**: `/src/app/service.py`. Before, it was matched against the whole
path as Sokrates loaded it, source root included.

Why: with an absolute source root (Docker's `/code`, any absolute `-srcRoot`, or a configuration file
given by an absolute path) the folders *above* the repository were part of every match. A repository
checked out under a `tests`, `docs`, `build` or `generated` folder had all its files classified by that
folder's name, and an `ignore` rule such as `.*/docs/.*` could silently empty the analysis.

What stays the same: every pattern written the documented way, starting with `.*` and naming folders
inside the repository (`.*/[Tt]ests/.*`, `.*[.]sql`, `.*/src/main/.*`), matches exactly as before,
including on top-level folders thanks to the leading `/`.

What to check: a pattern that deliberately named a folder above the repository — for example
`.*/code/src/.*` written against Docker's `/code` — no longer matches; drop the prefix
(`.*/src/.*`). Folder-depth components are unaffected (their generated filters start at the folder
now). The `sokrates-skills` configuration scripts (`preview_config.py` and the proposal scripts) follow
the new rule from the same date.

### `-ai` asks for a basic scan by default

The default prompt of `-ai claude|codex|gemini` now asks the sokrates-skills entry skill for a *basic* scan
(the six descriptive scanners that answer "what is this codebase"), validated and rendered, and tells the
agent not to change source files. Before it asked for a full scan, all seventeen scanners, which takes
hours per repository and is rarely what a landscape run needs. `-aiPrompt "run a full scan"` restores the
old behaviour; `-aiPrompt` takes any request.

### `installSkills`: the AI skills installed by Sokrates itself

`sokrates installSkills` clones the [sokrates-skills](https://github.com/zeljkoobrenovic/sokrates-skills)
repository into `~/.sokrates/skills/` (JGit, no git binary) and links every skill into the agents' skills
folders, `~/.claude/skills` for Claude Code and `~/.agents/skills` for Codex, Gemini CLI, Cursor and
Copilot. `-project` installs into the current project's `.claude/skills` and `.agents/skills` instead,
`-target <folder>` anywhere else, `-copy` copies instead of linking, `-source`/`-ref` pick another
repository, branch or tag. Re-running updates. An `-ai claude|codex|gemini` run whose agent does not have
the skills now says so in the log. Nothing changes for existing setups made with the skills' own `install.sh`.

### Dependencies declared for what the code uses

Build and dependency hygiene, found by running the sokrates-skills tech-stack scanner on Sokrates itself.
No report, data format or command changes.

- **Jackson is declared directly** (`jackson-databind` 2.19.1, the version that was resolved before). The
  Jersey media module that carried it transitively is gone; nothing in Sokrates used Jersey or Jakarta
  REST, so the fat jars no longer ship them. Anything that depended on Jersey classes being on the
  classpath of the CLI jar (nothing in Sokrates did) must declare them itself.
- **One Log4j version** for `log4j-api` and `log4j-core` (2.25.5). `slf4j-simple` is replaced by
  `log4j-slf4j2-impl`, so JGit's SLF4J logging goes through the same Log4j backend and
  `log4j2.properties` instead of a second, separately configured logger.
- **Java 17 API enforced at compile time** with `maven.compiler.release`, so a build on a newer JDK (the
  CodeBuild job uses 21) cannot link against APIs missing on a 17 runtime. The bytecode level is unchanged.
- `commons-cli` is declared in the `cli` module, where it is used, instead of `reports`; the
  `maven-assembly-plugin` version is pinned (3.7.1) so both fat jars are assembled the same way everywhere.
- **The 3D views load a pinned x3dom** (1.8.3 from jsDelivr) instead of the unversioned
  `examples.x3dom.org` demo URL. Reports generated earlier keep the old URL until regenerated.

### The analysis output is always ignored

`init` writes the ignore rules for `_sokrates/`, `_sokrates_landscape/`, the git export text files and
`sokrates_*.json` into every new `config.json`, also when nothing matches them yet (on a first analysis
the output does not exist). Configurations written earlier get the same rules in memory when
`generateReports` loads them; the file itself is not modified. Before, a second run of a fresh
configuration counted the first run's reports as main code.

### `generateReports -confFile _sokrates/config.json` analyzes the repository

A configuration file given by a relative path with no grandparent (`_sokrates/config.json` run from the
repository root) resolved the default `srcRoot` `..` against the working directory's parent and analyzed
that folder. It now resolves to the repository, and the resolved root stays relative when the
configuration path is relative.
