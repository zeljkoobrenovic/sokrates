# codeexplorer

The interactive Swing GUI for Sokrates. It lets you browse a code base, edit the analysis configuration, run analyses, and preview reports interactively, rather than through the CLI.

Package root: `nl.obren.sokrates.codeexplorer`. Depends on [`common`](../common/README.md), [`codeanalyzer`](../codeanalyzer/README.md), [`reports`](../reports/README.md), and [`cli`](../cli/README.md) — so it sits at the top of the module dependency chain.

Main class: `nl.obren.sokrates.codeexplorer.CodeExplorerLauncher`.

## Run

```bash
java -jar codeexplorer/target/codeexplorer-1.0-jar-with-dependencies.jar
```

## What's here

UI panels organized by analysis area, mirroring the analyses in [`codeanalyzer`](../codeanalyzer/README.md):

* `codebrowser` — file/aspect tree browser
* `configuration` — view/edit the analysis configuration
* `dependencies`, `units`, `duplication`, `search`, `charts`, `preview`, `newanalysis`, `console`

This is presentation only — analysis and report generation are delegated to the lower modules.

## Build / test

```bash
mvn -pl codeexplorer -am install
mvn -pl codeexplorer test
```
