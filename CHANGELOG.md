# Release notes

User-visible behaviour changes, newest first. `:latest` of the Docker image and the `master` branch carry
everything listed under the most recent date.

## 2026-10-05

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
