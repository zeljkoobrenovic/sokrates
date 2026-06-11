# Sokrates

**Know your code! The unexamined code is not worth maintaining!**

Sokrates is a source-code analysis tool — code spelunking inspired by grep, adding structure on top of regex source-code searches. It scans a code base, builds a JSON analysis configuration, and generates a suite of HTML reports that help you understand size, duplication, structure, dependencies, contributors, and trends.

It implements Željko Obrenović's "examined code" vision: a pragmatic, efficient way to understand complex source-code bases. It ships with both a command line interface and an interactive GUI code explorer.

For details and examples, visit [sokrates.dev](https://sokrates.dev).

## Prerequisites

* Java 17+
* Maven

No external tools are needed to generate reports — dependency and visualization graphs are rendered in the browser (Mermaid.js and d3, loaded from a CDN), so there is no Graphviz/`dot` dependency.

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

View the results by opening `_sokrates/reports/index.html` (it redirects to `html/index.html`). The report home page embeds an interactive **Structure** view of zoomable circles (circle size = lines of code); its default **All Files** tab groups every file by folder and color-codes each file by scope (main, test, build & deployment, generated, other).

> **Serve the reports over HTTP.** To keep the report compact, source snippets, data exports and several visualizations are packaged into zip/bundle files that the pages fetch and unpack in the browser, so the reports must be served over HTTP rather than opened straight from disk (`file://`) — e.g. `cd _sokrates/reports && python3 -m http.server`, then open <http://localhost:8000/>. They also need internet access for the CDN-hosted rendering libraries.

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

The image is a plain JRE — no Graphviz or other native tooling is needed (graphs render in the browser).

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

