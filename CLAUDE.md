# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Sokrates is a source-code analysis tool ("code spelunking, inspired by grep, adding structure on top of regex source code searches"). It scans a codebase, builds a JSON analysis configuration, and generates a suite of HTML reports (overview, duplication, file size, logical decomposition, dependencies, contributors/git history, concerns, findings, metrics, controls, trends). It ships as both a CLI and a Swing GUI explorer. See [sokrates.dev](https://sokrates.dev).

## Build & test

Java 17, Maven multi-module build.

```bash
mvn clean install                              # build all modules + run tests, produces fat jars
mvn install -DskipTests                        # build without tests (as Dockerfile does)
mvn -pl codeanalyzer -am install               # build one module and its dependencies
mvn -pl codeanalyzer test                      # run tests for a single module
mvn -pl codeanalyzer test -Dtest=JavaAnalyzerTest                     # single test class
mvn -pl codeanalyzer test -Dtest=JavaAnalyzerTest#testExtractUnits    # single test method
```

Build artifacts (fat jars via maven-assembly + jar-with-dependencies):
- `cli/target/cli-1.0-jar-with-dependencies.jar` — CLI (main class `nl.obren.sokrates.cli.CommandLineInterface`)
- `codeexplorer/target/codeexplorer-1.0-jar-with-dependencies.jar` — Swing GUI (main class `nl.obren.sokrates.codeexplorer.CodeExplorerLauncher`)

Tests use JUnit Jupiter + Vintage (mixed JUnit 4/5). The parent pom forces `-Duser.country=US -Duser.language=us` via `argLine` — locale-sensitive formatting tests depend on this.

No external runtime dependencies: dependency/visualization graphs are rendered **client-side with Mermaid.js** (loaded from CDN), so report generation no longer shells out to an external process. (Graphviz/`dot` was previously required; it has been removed — see "Dependency graphs" below.)

## Running the CLI

```bash
java -jar cli/target/cli-1.0-jar-with-dependencies.jar <command> [args]
```

Key commands (defined in `cli/.../Commands.java`, dispatched in `CommandLineInterface.java`):
- `init` — create `config.json` analysis configuration for a codebase (`-srcRoot`, `-confFile`, `-conventionsFile`)
- `generateReports` — run analysis and emit HTML reports (`-confFile`, `-outputFolder`; `-skipDuplication`, `-skipComplexAnalyses`, etc.)
- `updateConfig` — fill in missing fields of an existing config
- `updateLandscape` — aggregate multiple analyses into a landscape report
- `extractGitHistory` / `extractGitSubHistory` — produce `git-history.txt` consumed by history analyses
- `createConventionsFile` / `exportStandardConventions` — work with analysis conventions

Typical flow: `init` → edit `config.json` → `generateReports`.

## Module architecture

The Maven module dependency chain is `common → codeanalyzer → reports → cli → codeexplorer`. All code lives under the `nl.obren.sokrates` package.

- **common** — foundation: JSON (Jackson via Jersey), rendering utilities, chart/3D-force/x3d rendering helpers, IO. No Sokrates-specific domain logic.
- **codeanalyzer** — the analysis engine. Defines the configuration model, scopes the codebase, runs language-specific and cross-cutting analyses, produces a `CodeAnalysisResults` object.
- **reports** — consumes `CodeAnalysisResults` and renders HTML reports + exports data (JSON). Also builds landscape reports aggregating many analyses.
- **cli** — command-line entry point and git history extraction.
- **codeexplorer** — Swing GUI front-end; depends on cli, codeanalyzer, common.

## How analysis works (the big picture)

1. **Configuration** — `CodeConfiguration` (`codeanalyzer/.../sourcecode/core/`) is the central serialized model, read from/written to `config.json` (`CodeConfigurationUtils.DEFAULT_CONFIGURATION_FILE_NAME`). It declares: file `extensions`, `ignore` filters, scope aspects (`main`, `test`, `generated`, `buildAndDeployment`, `other`), `logicalDecompositions`, `concernGroups`, `goalsAndControls`, and `tagRules`. Most analysis behavior is driven by this JSON, not by code changes. The user-facing reference for both `_sokrates/config.json` and `_sokrates_landscape/config.json` (every key, defaults, examples, and the companion `config-tags`/`config-teams`/`config-people.json` files) is **[docs/configuration.md](docs/configuration.md)** — update it when changing these config models.

2. **Scoping** — `scoping/ScopeCreator` plus `Convention`/`ConventionUtils` apply standard + custom conventions to classify files into aspects. An *aspect* (`sourcecode/aspects/`) is a named set of source files defined by path/content filters; aspects are the unit that decompositions and concerns are built from.

3. **Language analyzers** — `LanguageAnalyzerFactory` maps file extensions to a `LanguageAnalyzer` subclass (`sourcecode/lang/<language>/`). 40+ languages are supported. Each analyzer implements:
   - `cleanForLinesOfCodeCalculations` / `cleanForDuplicationCalculations` — strip comments/blank lines
   - `extractUnits` — find functions/methods (for unit-size and conditional-complexity metrics)
   - `extractDependencies` — heuristic, regex-based dependency extraction
   - `getFeaturesDescription` — declares which analyses the language supports
   To add language support: create a `LanguageAnalyzer` subclass and register its extensions in `LanguageAnalyzerFactory`. Unknown extensions fall back to `DefaultLanguageAnalyzer`.

4. **Analysis orchestration** — `analysis/CodeAnalyzer.analyze(...)` runs a sequence of `Analyzer` subclasses in `analysis/files/` (`BasicsAnalyzer`, `DuplicationAnalyzer`, `FileSizeAnalyzer`, `UnitsAnalyzer`, `LogicalDecompositionAnalyzer`, `ConcernsAnalyzer`, `ControlsAnalyzer`, `ContributorsAnalyzer`, `FileHistoryAnalyzer`), each populating part of `CodeAnalysisResults` (`analysis/results/`). `CodeAnalyzerSettings` toggles expensive analyses (duplication, correlations, complex analyses).

5. **Reporting** — generators in `reports/.../generators/statichtml/` each take `CodeAnalysisResults` and emit an HTML report; `dataexporters/` emit JSON. `landscape/` aggregates multiple project results into a portfolio-level report.

## Landscapes & sub-landscapes

A **landscape** aggregates many repository reports into a portfolio view. `updateLandscape` (CLI) runs `LandscapeAnalysisCommands.generateReport(analysisRoot, configFile)`: `LandscapeAnalyzer.analyze(configFile)` reads each repository's `analysisResults.json` (located via `analysisRoot` + each `SokratesRepositoryLink.analysisResultsPath`) into a `LandscapeAnalysisResults` (`List<RepositoryAnalysisResults>`); then `LandscapeReportGenerator(results, tagGroups, folder, reportsFolder).report()` writes `index.html` + tabs + `contributors/` + `visuals/` + `data/` into the `_sokrates_landscape/` folder. A repository's identity is its `metadata.name`.

Two ways to create **sub-landscapes** (shown in the parent's "Sub-landscapes" tab, which reads each child's pre-generated `data/landscapeAnalysisResults.json` + `config.json` and links via `repositoryReportsUrlPrefix + indexFilePath`):
- **Folder-based** — move repository report folders into sub-directories that each have their own `_sokrates_landscape/`. Discovered by scanning for `_sokrates_landscape/index.html` (`LandscapeAnalysisInitiator`, `LandscapeAnalysisUtils.findAllSokratesLandscapeConfigFiles`).
- **Virtual landscapes** — defined by repository-name regex patterns in the parent `config.json` (`virtualLandscapes`: `landscapes[]` with `metadata`/`includeRepoNamePatterns`/`excludeRepoNamePatterns`, plus `remainderLandscapeMetadata`); no folder moves. `VirtualLandscapeBuilder` partitions the parent's already-loaded repositories (multi-membership allowed; the **Remainder** landscape collects unmatched repos) into child `LandscapeAnalysisResults`, and `LandscapeAnalysisCommands.generateVirtualLandscapes` renders a full report per virtual landscape into `_sokrates_landscape/landscapes/<name>/_sokrates_landscape/`, registering each as a **virtual** `SubLandscapeLink` (resolved relative to the landscape folder, no URL prefix). The child's contributor/team topology graphs read pre-computed people-dependency data, so after setting the child config `generateVirtualLandscapes` calls `LandscapeAnalyzer.updatePeopleDependencies(child)` (public static, reused from the analyzer) — otherwise those graphs are empty. The sub-landscape zoomable-circles/sunburst visuals also group repos by virtual landscape (via `buildVirtualLandscapeCircles` in `LandscapeReportGenerator`) instead of by folder path when virtual landscapes are configured. The generated `landscapes/` tree is excluded from folder discovery via `LandscapeAnalysisUtils.isInGeneratedVirtualLandscape` so virtual landscapes aren't picked up twice. A `VirtualLandscapeConfig` may itself carry a nested `virtualLandscapes` (**unlimited depth**): `generateVirtualLandscapes` recurses, partitioning each virtual landscape's own repositories and rendering them under `…/landscapes/<parent>/_sokrates_landscape/landscapes/<child>/_sokrates_landscape/`; `childConfiguration(root, metadata, nested, depth)` climbs `depth * 3` `../` levels (each nesting descends 3 folders) so repo links still resolve to the shared repository folders, and registers the nested links on the child's config so they show in the child's own Sub-landscapes tab. The feature is inert when `virtualLandscapes.landscapes` is empty (backward compatible). Both kinds coexist.

## Two HTML rendering mechanisms

Reports are produced in two distinct ways — know which one you're touching:

1. **Server-rendered (`RichTextReport`)** — most reports. Java code builds the HTML imperatively via the `RichTextReport` builder API (`startTable`/`addTableCell`/`startTabContentSection`/…) in `reports/core/`. Tabs, tables, and inline SVG are all emitted as strings from Java. Shared chrome (CSS, tab JS, the report header `<head>`) lives in `reports/core/ReportConstants.java`.

2. **Client-rendered templates** — static HTML shells with `${...}` placeholders that get the data embedded as JSON and do all rendering in the browser. `common/.../renderingutils/ExplorerTemplate.render(...)` substitutes `${data}` plus any extra placeholders (`${langIcons}`, `${features}`, `${options}`, …). This keeps very large landscapes small and adds client-side **search + click-to-sort + show-more paging** in the browser. **The data is embedded compressed**: `${data}` becomes a `sokratesInflate("<deflate+base64 JSON>")` call and a `${sokrates-inflate-lib}` head block (fflate + the `sokratesInflate` helper, shared with `VisualizationTemplate`) is injected, so the page inflates the inline JSON in-browser. Each report stays self-contained (works from `file://`, no fetch). When validating template JS under node (below), `${data}` is now a function call, not a JSON literal — stub it as `sokratesInflate = () => <fixture>`. Templates live in two folders:
   - `common/src/main/resources/templates/` — the tabular landscape reports: `repositories-report.html` (→ `repositories.html`), `files-explorer.html`, `contributors-report.html` (Recent/All/Bots tabs → `contributors.html`/`teams.html` via iframes with `?tab=`), `contributor-report.html` (the per-contributor/per-team individual report, → `contributors/<email>.html`), and the slim `repository-explorer.html`. The per-repository report's **Files** and **Units** tabs also use this mechanism: `files-explorer.html` and `units-explorer.html` are generated by `FilesExplorerGenerators`/`UnitsExplorerGenerators` (`reports/.../generators/explorers/`) into `<reports>/explorers/` from `FileExport`/`UnitExport` DTOs, and embedded as iframes by `ReportFileExporter`.
   - `common/src/main/resources/vis_templates/` — visualisations rendered with third-party JS pinned to a CDN version: `force_2d.html`/`force_3d.html` (force-graph; node list is searchable/toggleable), `bar_chart_races.html` (d3 racing chart), and the d3 pack-layout `zoomable_circles.html`/`zoomable_sunburst.html`. Most use `${data}` substitution (via `VisualizationTemplate`) — the data is embedded into the HTML at generation time, but **compressed**: `${data}` is replaced with `sokratesInflate("<deflate+base64 JSON>")` and a `${sokrates-inflate-lib}` head block (fflate + the `sokratesInflate` helper) is injected, so the page inflates the inline data in-browser before rendering. This keeps each file self-contained (works from `file://`, no fetch) while shrinking it (e.g. the all-files circles dropped ~83KB→~12KB). Applies to `bubble_chart`, `treemap`, `force_2d`, `force_3d`, `bar_chart_races`, and `zoomable_circles_colored` (→ `zoomable_circles_all_files.html`). `VisualizationTemplate.deflateBase64` (zlib `Deflater`) + `embedCompressed` are the central plumbing; the empty-children strip happens on the JSON before compressing. **Exception — the zoomable circles/sunburst families do NOT embed data**: to avoid one HTML file per view (~40+/repo), `zoomable_circles.html`/`zoomable_sunburst.html` are STATIC templates (no `${data}`); each repo gets a single shared template + one `zoomable_circles.zip`/`zoomable_sunburst.zip` holding one JSON entry per view (keyed by the old filename suffix: `main`, `commits_30_main`, `main_loc_coloring`, …). The page reads `?key=` and fflate-extracts that entry (same client-side unzip as `src/viewer.html`; requires HTTP serving). CLI's `generateVisuals` accumulates `key→JSON` (via `VisualizationTemplate.zoomableItemsJson`) and writes the zip + template via `writeZoomableFamily`; report links use `visuals/zoomable_circles.html?key=<suffix>`. `zoomable_circles_colored.html` is a variant of `zoomable_circles.html` whose fill logic inherits the nearest ancestor's explicit `color`, falling back to the depth-based gradient for uncolored nodes — rendered via `VisualizationTemplate.renderZoomableCirclesColored`; it still embeds data and stays one standalone file (`zoomable_circles_all_files.html`). The `sub_landscapes_zoomable_*` files in landscape reports are a separate family (still per-view). The **x3dom 3D views** (`units_3d_*`, `files_3d`) are built programmatically by `X3DomExporter` (not a `vis_templates` file): rather than emit one `<Transform><Shape><Box>` per node (~1.3MB for large repos), it embeds the per-node data (`color`+`size`, grouped by component) as compressed inline JSON (`SOKRATES_3D_DATA`, via the same `deflateBase64` + `sokratesInflate` helpers) and a JS `buildComponentBoxes` rebuilds the identical X3D DOM in-browser using the same grid layout math (`sqrtRows`, boustrophedon column order, `MARGIN_RATION`) — keep the Java and JS layout in sync. Dropped these files ~1.3MB→~5KB.

   The per-repository **Structure** report (`reports/.../templates/Structure.html`, shown as an iframe on the report home page) is a tab strip of these circle/sunburst visuals. The CLI's `generateVisuals` (`CommandLineInterface`) writes the backing `visuals/zoomable_circles_*.html` files: `zoomable_circles_<scope>.html` per scope (main/test/build/generated/other), the risk-colored `..._<metric>_coloring[_categories].html` for the main scope, and `zoomable_circles_all_files.html` — the default "All Files" tab, which merges every scope's files into one folder-structure tree (`PathStringsToTreeStructure.toVisualizationItems(Map<SourceFile,String>)`) and color-codes each **file leaf** by its scope (folders stay on the depth gradient). The scope-color constants in `CommandLineInterface` (`SCOPE_COLOR_*`) and the legend swatches in `Structure.html` must stay in sync.

   The per-row JSON shapes are DTOs under `reports/.../landscape/data/` (e.g. `RepositoryExport`/`RepositoryReportData`, `ContributorReportExport`, `ContributorIndividualReportExport`). `LandscapeDataExport` and `LandscapeReportContributorsTab`/`LandscapeIndividualContributorsReports` build the data and render the templates. **The template (JS render logic) and its DTO move together** — change both when adding a field. These reports do *not* apply server-side list caps (paging handles scale); don't reintroduce a `getContributorsListLimit`-style truncation. Validate template JS by extracting the `<script>` blocks, stubbing the placeholders, and running `vm.compileFunction` (or a small DOM stub) under node before regenerating a landscape.

## Source-code cache: the `src/` tree (one shared viewer, zips + bundles)

The per-repository report's `<reports>/src/` folder holds the source code referenced by the reports. To avoid emitting 1000+ tiny files per repo (slow to sync/upload), it is packaged for **file count**, not per-file granularity, and rendered by a **single client-side viewer** rather than one HTML page per snippet. `DataExporter.exportSourceFile()` (`reports/.../dataexporters/DataExporter.java`) writes:

- **`src/<aspect>.zip`** — one zip per scope (main/test/generated/buildAndDeployment/other), gated by `analysis.saveSourceFiles`. Entries are keyed by `SourceFile.getRelativePath()` (the same value the viewer's `file=` param uses); content is the full file. Only files in `getReferencedFiles()` (the ≤9 top-N lists, capped by `maxTopListSize`) are included. Written via `ZipUtils.stringToZipFile`. **Zip-only — no raw files are kept** (only `data/*` is an external-tooling contract; `src/` is report-only and freely restructurable).
- **`src/fragments/<type>.json`** — one JSON bundle per fragment list, gated by `analysis.saveCodeFragments`. Unit lists (`longest_unit`, `most_complex_units`) → array of `FragmentExport` (`{name,file,from,to,loc,mccabe,ext,code}`, `reports/.../dataexporters/units/`). Duplicate lists (`longest_duplicates`, `most_frequent_duplicates`, `unit_duplicates`) → array of `DuplicateFragmentExport` (`{ext, blocks:[{file,from,to,code}]}`, `reports/.../dataexporters/duplication/`). Array order matches the source list order; reports link to a 1-based index.
- **`src/viewer.html`** — one shared client-rendered viewer (highlight.js v11 + `highlightjs-line-numbers.js` + fflate, all CDN-pinned). `?aspect=<a>&file=<path>[&from=&to=]` extracts one entry from `<a>.zip` and highlights it; `?bundle=fragments/<type>.json&i=<1-based>` renders that bundle item (units via `renderUnit`, duplicates via multi-block `renderDuplicate`). Line numbers start at the fragment's real `from` line.

**The viewer's render JS and these DTOs are a contract — the JSON field names must match.** When adding a field, change the DTO and `viewer.html` together (mirrors the landscape template/DTO rule above). Five report link sites point at the viewer: `UtilsReportUtils` (unit file link + unit fragment link), `FilesReportUtils` (file-table link), `DuplicationReportGenerator` (duplicate "view"), and `FilesExplorerGenerators`/`UnitsExplorerGenerators` (explorer links). A link's index must stay aligned with its bundle's array order; the duplication table caps displayed rows at `MAX_TABLE_ROWS_COUNT` but the bundle holds all (indices within the cap still align). **Reports must be served over HTTP** (local server or hosted) — the viewer uses `fetch()` for the zip/bundle, which `file://` blocks. Validate `viewer.html` JS the same way as other template JS (extract `<script>`, stub, run under node).

## Dependency graphs (Mermaid, client-side — no Graphviz)

Dependency/visualization graphs (component, file-temporal, duplication-between-components, contributor-shared-files, and all landscape graphs) are rendered **client-side with Mermaid.js**, not server-side Graphviz. There is no external `dot` process and no per-graph `.svg`/`.dot.txt` files.

- **Producer**: `GraphvizDependencyRenderer.getMermaidContent(allComponents, deps, groups)` (`reports/.../utils/`) emits a Mermaid `flowchart` (the legacy `getGraphvizContent` DOT producer is unused by reports). Configured by the same setters (`setTypeGraph`/`setTypeDigraph` → undirected `---` vs directed `-->`, `setOrientation` → `flowchart TB/LR/…`, `setArrowColor`/`setCyclicArrowColor`, `setDefaultNodeFillColor`, `setMaxNumberOfDependencies`, `setReverseDirection`). Nodes get **synthetic ids** (`n0`,`n1`,…) with the real name in the `["label"]` (Mermaid ids can't hold arbitrary chars; quotes escaped to `&quot;`). Edges are emitted in count-desc order and each gets a `linkStyle <index> stroke:…,stroke-width:…px` line — **`linkStyle` targets edges by definition-order index, so edge ordering is load-bearing** (covered by `GraphvizDependencyRendererMermaidTest`). Graphviz X11 colour names are mapped to CSS via `toCssColor` (e.g. `deepskyblue2`→`#00b2ee`).
- **Embedding**: callers still call `RichTextReport.addGraphvizFigure(id, desc, content)`; the content is now Mermaid. `ReportRenderer.mermaidBlock(id, def)` emits the fragment as `<pre class="mermaid">…</pre>` (verbatim — **not** through `minimize()`, which would corrupt the layout-sensitive text), preceded by a hidden `<script type="text/plain" id="mermaid-source-<id>">` holding the same definition. **No `.mmd` files are written to disk**: the "download .mmd" link (the `addDownloadLinks` helpers + the `downloadMermaid(id)` JS in `REPORTS_HTML_HEADER`) builds the file in the browser from that embedded source as a `Blob` — mermaid.js overwrites the `<pre>` with SVG on render, so the source must be stashed in the script tag to survive. Mermaid.js is loaded once via `ReportConstants.REPORTS_HTML_HEADER` with **`startOnLoad: false`** — a diagram laid out inside a hidden container (a collapsed `<details>` show-more block or an inactive tab) has zero size and Mermaid 10 throws "Syntax error in text". So rendering is **lazy and visibility-gated**: `renderMermaidIn(container)` runs only the visible, not-yet-processed `pre.mermaid`, called on `DOMContentLoaded`, on each `<details>` `toggle`, and on tab open (`openTab`). When adding a new collapsible container type, make sure its diagrams get a `renderMermaidIn` trigger. Standalone graph pages (previously `.svg`) are written as self-contained Mermaid HTML via `VisualizationTools.standaloneMermaidPage`.
- **Reports must be served over/opened in a browser with CDN access** for mermaid.js (same constraint as the source viewer). Force-graph/d3 visuals are a separate mechanism and were untouched. The Swing/JavaFX explorer's inline graph preview was dropped (it required Graphviz); the explorer shows the dependency text + matrix instead.

## Conventions when modifying analysis behavior

- The regex-driven, heuristic nature of dependency/unit extraction means analyzers are approximate by design — match the existing heuristic style rather than attempting full parsing.
- Adding a new analysis usually means: new `Analyzer` subclass + a results field on `CodeAnalysisResults` + a matching report generator. The three stay in lockstep.
- Defaults (default concerns, default tag rules, default goals/controls) are generated in code in `CodeConfiguration` — extend those methods rather than hardcoding into the JSON template.
