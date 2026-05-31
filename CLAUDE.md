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

External runtime dependency: **Graphviz** (`dot`) is required to render dependency/visualization graphs. The Docker image installs it and sets `GRAPHVIZ_DOT=/usr/bin/dot`.

## Running the CLI

```bash
java -jar cli/target/cli-1.0-jar-with-dependencies.jar <command> [args]
```

Key commands (defined in `cli/.../Commands.java`, dispatched in `CommandLineInterface.java`):
- `init` — create `config.json` analysis configuration for a codebase (`-srcRoot`, `-confFile`, `-conventionsFile`)
- `generateReports` — run analysis and emit HTML reports (`-confFile`, `-outputFolder`; `-skipDuplication`, `-skipComplexAnalyses`, `-internalGraphviz`, etc.)
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
- **Virtual landscapes** — defined by repository-name regex patterns in the parent `config.json` (`virtualLandscapes`: `landscapes[]` with `metadata`/`includeRepoNamePatterns`/`excludeRepoNamePatterns`, plus `remainderLandscapeMetadata`); no folder moves. `VirtualLandscapeBuilder` partitions the parent's already-loaded repositories (multi-membership allowed; the **Remainder** landscape collects unmatched repos) into child `LandscapeAnalysisResults`, and `LandscapeAnalysisCommands.generateVirtualLandscapes` renders a full report per virtual landscape into `_sokrates_landscape/landscapes/<name>/_sokrates_landscape/`, registering each as a **virtual** `SubLandscapeLink` (resolved relative to the landscape folder, no URL prefix). The generated `landscapes/` tree is excluded from folder discovery via `LandscapeAnalysisUtils.isInGeneratedVirtualLandscape` so virtual landscapes aren't picked up twice. A `VirtualLandscapeConfig` may itself carry a nested `virtualLandscapes` (**unlimited depth**): `generateVirtualLandscapes` recurses, partitioning each virtual landscape's own repositories and rendering them under `…/landscapes/<parent>/_sokrates_landscape/landscapes/<child>/_sokrates_landscape/`; `childConfiguration(root, metadata, nested, depth)` climbs `depth * 3` `../` levels (each nesting descends 3 folders) so repo links still resolve to the shared repository folders, and registers the nested links on the child's config so they show in the child's own Sub-landscapes tab. The feature is inert when `virtualLandscapes.landscapes` is empty (backward compatible). Both kinds coexist.

## Two HTML rendering mechanisms

Reports are produced in two distinct ways — know which one you're touching:

1. **Server-rendered (`RichTextReport`)** — most reports. Java code builds the HTML imperatively via the `RichTextReport` builder API (`startTable`/`addTableCell`/`startTabContentSection`/…) in `reports/core/`. Tabs, tables, and inline SVG are all emitted as strings from Java. Shared chrome (CSS, tab JS, the report header `<head>`) lives in `reports/core/ReportConstants.java`.

2. **Client-rendered templates** — static HTML shells with `${...}` placeholders that get the data embedded as JSON and do all rendering in the browser. `common/.../renderingutils/ExplorerTemplate.render(...)` substitutes `${data}` with the compact JSON serialization of a value object (Jackson, via `JsonGenerator`) plus any extra placeholders (`${langIcons}`, `${features}`, `${options}`, …). This keeps very large landscapes small and adds client-side **search + click-to-sort + show-more paging** in the browser. Templates live in two folders:
   - `common/src/main/resources/templates/` — the tabular landscape reports: `repositories-report.html` (→ `repositories.html`), `files-explorer.html`, `contributors-report.html` (Recent/All/Bots tabs → `contributors.html`/`teams.html` via iframes with `?tab=`), `contributor-report.html` (the per-contributor/per-team individual report, → `contributors/<email>.html`), and the slim `repository-explorer.html`.
   - `common/src/main/resources/vis_templates/` — visualisations rendered with third-party JS pinned to a CDN version: `force_2d.html`/`force_3d.html` (force-graph; node list is searchable/toggleable) and `bar_chart_races.html` (d3 racing chart). These also use `${data}` substitution (via `VisualizationTemplate`).

   The per-row JSON shapes are DTOs under `reports/.../landscape/data/` (e.g. `RepositoryExport`/`RepositoryReportData`, `ContributorReportExport`, `ContributorIndividualReportExport`). `LandscapeDataExport` and `LandscapeReportContributorsTab`/`LandscapeIndividualContributorsReports` build the data and render the templates. **The template (JS render logic) and its DTO move together** — change both when adding a field. These reports do *not* apply server-side list caps (paging handles scale); don't reintroduce a `getContributorsListLimit`-style truncation. Validate template JS by extracting the `<script>` blocks, stubbing the placeholders, and running `vm.compileFunction` (or a small DOM stub) under node before regenerating a landscape.

## Conventions when modifying analysis behavior

- The regex-driven, heuristic nature of dependency/unit extraction means analyzers are approximate by design — match the existing heuristic style rather than attempting full parsing.
- Adding a new analysis usually means: new `Analyzer` subclass + a results field on `CodeAnalysisResults` + a matching report generator. The three stay in lockstep.
- Defaults (default concerns, default tag rules, default goals/controls) are generated in code in `CodeConfiguration` — extend those methods rather than hardcoding into the JSON template.
