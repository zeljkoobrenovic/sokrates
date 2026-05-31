# Sokrates

**Know your code! The unexamined code is not worth maintaining!**

Sokrates is a source-code analysis tool — code spelunking inspired by grep, adding structure on top of regex source-code searches. It scans a code base, builds a JSON analysis configuration, and generates a suite of HTML reports that help you understand size, duplication, structure, dependencies, contributors, and trends.

It implements Željko Obrenović's "examined code" vision: a pragmatic, efficient way to understand complex source-code bases. It ships with both a command line interface and an interactive GUI code explorer.

For details and examples, visit [sokrates.dev](https://sokrates.dev).

## Prerequisites

* Java 17+
* Maven
* [Graphviz](https://graphviz.org/) (the `dot` program) — required to render dependency and visualization graphs. By default Sokrates calls the external `dot`; point to it with the `GRAPHVIZ_DOT` environment variable, or pass `-internalGraphviz` to use the bundled library instead.

## Build

```bash
mvn clean install
```

This produces two runnable fat jars:

* CLI — `cli/target/cli-1.0-jar-with-dependencies.jar`
* Interactive explorer (GUI) — `codeexplorer/target/codeexplorer-1.0-jar-with-dependencies.jar`

## Quick start (CLI)

The typical workflow is **init → generateReports**. Run both from the root of the code base you want to analyze:

```bash
# 1. Create the analysis configuration (writes _sokrates/config.json)
java -jar cli-1.0-jar-with-dependencies.jar init -srcRoot .

# 2. (optional) Edit _sokrates/config.json to refine scope, logical decompositions, concerns, goals

# 3. Generate the HTML reports (into _sokrates/reports/)
java -jar cli-1.0-jar-with-dependencies.jar generateReports
```

Open `_sokrates/reports/html/index.html` in a browser to view the results.

Run a command with `-help` to see its options:

```bash
java -jar cli-1.0-jar-with-dependencies.jar generateReports -help
```

### Commands

| Command | Description |
| --- | --- |
| `init` | Create a new analysis configuration (`config.json`) from standard + optional custom conventions |
| `generateReports` | Run the analysis and generate the HTML/JSON reports |
| `updateConfig` | Fill in missing fields of an existing configuration |
| `updateLandscape` | Create/update a landscape report that aggregates multiple analyses |
| `createConventionsFile` | Create an analysis conventions file (`analysis_conventions.json`) |
| `exportStandardConventions` | Export the standard conventions to `standard_analysis_conventions.json` |
| `extractGitHistory` | Extract git history into `git-history.txt` (consumed by history/contributor analyses) |
| `extractGitSubHistory` | Split a `git-history.txt` into smaller files by path prefix |
| `extractFiles` | Extract files matching a path regex into a separate folder, to analyze a subset |

Defaults: configuration is read from `<currentFolder>/_sokrates/config.json` and reports are written to `<currentFolder>/_sokrates/reports/`.

## Configuration

Sokrates is driven by two JSON config files — `_sokrates/config.json` (repository analysis) and
`_sokrates_landscape/config.json` (landscapes). See the **[Configuration Manual](docs/configuration.md)**
for a full reference of every key, with defaults and examples.

## Run the GUI explorer

```bash
java -jar codeexplorer-1.0-jar-with-dependencies.jar
```

## Docker

```bash
# Build the image
docker build -t sokrates .

# Run a command (e.g. init) against the current directory
docker run -v "$(pwd):/code" -w /code sokrates init
```

The image bundles Graphviz and sets `GRAPHVIZ_DOT=/usr/bin/dot`.

## Project structure

Sokrates is a Maven multi-module project. The dependency chain is `common → codeanalyzer → reports → cli → codeexplorer`. All code lives under the `nl.obren.sokrates` package. Each module has its own README:

| Module | Role |
| --- | --- |
| [`common`](common/README.md) | Foundation: JSON, IO, rendering and chart utilities |
| [`codeanalyzer`](codeanalyzer/README.md) | The analysis engine: configuration model, scoping, language analyzers, analyses |
| [`reports`](reports/README.md) | Turns analysis results into HTML reports and JSON data exports |
| [`cli`](cli/README.md) | Command line interface and git-history extraction |
| [`codeexplorer`](codeexplorer/README.md) | Swing GUI for interactive exploration |

## License

See [LICENSE](LICENSE). Sokrates is built by Željko Obrenović.
