# codeanalyzer

The analysis engine of Sokrates. It reads an analysis configuration, classifies the source files, runs language-specific and cross-cutting analyses, and produces a `CodeAnalysisResults` object that the [`reports`](../reports/README.md) module renders.

Package root: `nl.obren.sokrates.sourcecode`. Depends on [`common`](../common/README.md).

## How analysis works

1. **Configuration** — `core/CodeConfiguration` is the central serialized model, read from / written to `config.json` (default `<currentFolder>/_sokrates/config.json`). It declares file `extensions`, `ignore` filters, scope aspects (`main`, `test`, `generated`, `buildAndDeployment`, `other`), `logicalDecompositions`, `concernGroups`, `goalsAndControls`, and `tagRules`. Most analysis behavior is driven by this JSON, not by code changes. Default concerns, tag rules, and goals are generated in code in `CodeConfiguration` — extend those methods rather than hardcoding into a JSON template.

2. **Scoping** — `scoping/ScopeCreator` plus `Convention` / `ConventionUtils` apply standard + custom conventions to classify files into aspects. An *aspect* (`aspects/`) is a named set of source files defined by path/content filters; aspects are the unit decompositions and concerns are built from.

3. **Language analyzers** — `lang/LanguageAnalyzerFactory` maps file extensions to a `LanguageAnalyzer` subclass under `lang/<language>/` (40+ languages). Unknown extensions fall back to `DefaultLanguageAnalyzer`. Each analyzer implements:
   * `cleanForLinesOfCodeCalculations` / `cleanForDuplicationCalculations` — strip comments / blank lines
   * `extractUnits` — find functions/methods (for unit-size and conditional-complexity metrics)
   * `extractDependencies` — heuristic, regex-based dependency extraction
   * `getFeaturesDescription` — declares which analyses the language supports

   To add a language: create a `LanguageAnalyzer` subclass and register its extensions in `LanguageAnalyzerFactory`. The extraction is heuristic/regex-based by design — match the existing style rather than attempting full parsing.

4. **Orchestration** — `analysis/CodeAnalyzer.analyze(...)` runs a sequence of `Analyzer` subclasses in `analysis/files/` (`BasicsAnalyzer`, `DuplicationAnalyzer`, `FileSizeAnalyzer`, `UnitsAnalyzer`, `LogicalDecompositionAnalyzer`, `ConcernsAnalyzer`, `ControlsAnalyzer`, `ContributorsAnalyzer`, `FileHistoryAnalyzer`), each populating part of `CodeAnalysisResults` (`analysis/results/`). `CodeAnalyzerSettings` toggles expensive analyses (duplication, correlations, complex analyses).

## Adding a new analysis

Adding an analysis usually means three changes that stay in lockstep:

1. a new `Analyzer` subclass in `analysis/files/`,
2. a results field on `CodeAnalysisResults`,
3. a matching report generator in the [`reports`](../reports/README.md) module.

## Build / test

```bash
mvn -pl codeanalyzer -am install
mvn -pl codeanalyzer test
mvn -pl codeanalyzer test -Dtest=JavaAnalyzerTest      # single test class
```
