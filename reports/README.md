# reports

Turns the `CodeAnalysisResults` produced by [`codeanalyzer`](../codeanalyzer/README.md) into human-readable HTML reports and machine-readable JSON data exports. Also builds landscape reports that aggregate many analyses into a portfolio view.

Package root: `nl.obren.sokrates.reports`. Depends on [`common`](../common/README.md) and [`codeanalyzer`](../codeanalyzer/README.md).

## What's here

* **generators/statichtml** — one generator per HTML report; each takes `CodeAnalysisResults` and emits a report. Examples: `OverviewReportGenerator`, `DuplicationReportGenerator`, `FileSizeReportGenerator`, `LogicalComponentsReportGenerator`, `ConcernsReportGenerator`, `ContributorsReportGenerator`, `FindingsReportGenerator`, `MetricsListReportGenerator`, `ControlsReportGenerator`, `TrendReportGenerator`, plus graph/visualization exporters (`ForceGraphExporter`, `VisualizationTools`).
* **dataexporters** — JSON data exports (dependencies, units, trends, files, duplication).
* **landscape** — aggregates multiple project analyses into a landscape (portfolio-level) report.
* **charts / utils / core** — shared report-rendering helpers.

Graph-based reports invoke Graphviz; see the root README for the `dot` / `GRAPHVIZ_DOT` / `-internalGraphviz` setup.

## Adding a report

A new report is typically: a new generator in `generators/statichtml/` that consumes a results field on `CodeAnalysisResults` (and, for a new analysis, a new `Analyzer` in `codeanalyzer`).

## Build / test

```bash
mvn -pl reports -am install
mvn -pl reports test
```
