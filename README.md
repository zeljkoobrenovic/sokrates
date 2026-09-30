# Sokrates

**Know your code! The unexamined code is not worth maintaining!**

Sokrates is a source-code analysis tool — code spelunking inspired by grep, adding structure on top of regex source-code searches. It scans a code base, builds a JSON analysis configuration, and generates a suite of HTML reports that help you understand size, duplication, structure, dependencies, contributors, and trends.

It implements Željko Obrenović's "examined code" vision: a pragmatic, efficient way to understand complex source-code bases. It ships with both a command line interface and an interactive GUI code explorer.

For details and examples, visit [sokrates.dev](https://sokrates.dev).

Sokrates is free open-source project, with a commercial friendly [MIT license](LICENSE). You can **[sponsor the work on Sokrates](https://github.com/sponsors/zeljkoobrenovic)** via GitHub [sponsors program](https://github.com/sponsors/zeljkoobrenovic). 

## Quick start (no install: Docker)

The easiest way to try Sokrates is the prebuilt image — no Java or Maven needed. Run it from the root of the code base you want to analyze:

```bash
docker run --rm -v "$(pwd):/code" ghcr.io/zeljkoobrenovic/sokrates analyze
```

That single command extracts the git history, creates the analysis configuration (`_sokrates/config.json`) and generates the reports into `_sokrates/reports/`. Then open `_sokrates/reports/index.html`. See [Docker](#docker) for details (file ownership, other commands).

## Prerequisites (building from source)

* Java 17+
* Maven

No external tools are needed to generate reports — the git history is read with JGit, and dependency and visualization graphs are rendered in the browser (Mermaid.js and d3, loaded from a CDN), so there is no git, Graphviz or `dot` dependency.

## Build

```bash
mvn clean install
```

This produces two runnable fat jars:

* CLI — `cli/target/cli-1.0-jar-with-dependencies.jar`
* Interactive explorer (GUI) — `codeexplorer/target/codeexplorer-1.0-jar-with-dependencies.jar`

## Quick start (CLI)

The one-shot `analyze` command does everything needed for a first report. Run it from the root of the code base you want to analyze:

```bash
java -jar cli-1.0-jar-with-dependencies.jar analyze
```

It runs three steps, each of which is also available as a separate command:

1. `extractGitHistory` — extracts the git history into `git-history.txt` (skipped when the folder is not a git repository, or with `-skipGitHistory`; without it the commit, contributor and trend reports are empty)
2. `init` — creates the analysis configuration `_sokrates/config.json` (only if it does not exist yet, so your edits survive re-runs)
3. `generateReports` — runs the analysis and generates the HTML reports into `_sokrates/reports/`

To analyze a repository you have not cloned yet, `analyzeGitRepo` clones it first and then runs the same three steps:

```bash
java -jar cli-1.0-jar-with-dependencies.jar analyzeGitRepo -url https://github.com/junit-team/junit4
# → ./junit4/ (the clone) with the reports in ./junit4/_sokrates/reports/
```

The typical iterative workflow is therefore **analyze → edit `_sokrates/config.json` (scope, logical decompositions, concerns, goals) → analyze again**. The individual commands are:

```bash
# 1. Extract the git history (writes git-history.txt)
java -jar cli-1.0-jar-with-dependencies.jar extractGitHistory

# 2. Create the analysis configuration (writes _sokrates/config.json)
java -jar cli-1.0-jar-with-dependencies.jar init

# 3. (optional) Edit _sokrates/config.json to refine scope, logical decompositions, concerns, goals

# 4. Generate the HTML reports (into _sokrates/reports/)
java -jar cli-1.0-jar-with-dependencies.jar generateReports
```

View the results by opening `_sokrates/reports/index.html` (it redirects to `html/index.html`). The report home page embeds an interactive **Structure** view of zoomable circles (circle size = lines of code); its default **All Files** tab groups every file by folder and color-codes each file by scope (main, test, build & deployment, generated, other).

> **Opening the reports.** To keep them compact, source snippets, data exports and several visualizations are packaged into zip/bundle archives — but these are **embedded inline** in the HTML and unpacked in the browser, so the reports open straight from disk (`file://`); just double-click `index.html`. They do need internet access for the CDN-hosted rendering libraries (Mermaid.js, d3, highlight.js). If your browser restricts something over `file://`, serve them over HTTP instead — e.g. `cd _sokrates/reports && python3 -m http.server`, then open <http://localhost:8000/>.

Run a command with `-help` to see its options:

```bash
java -jar cli-1.0-jar-with-dependencies.jar generateReports -help
```

### Commands

| Command | Description |
| --- | --- |
| `analyzeGitRepo` | Clone a repository from its URL (`-url`; JGit, no git binary) into `-destFolder` (default: a folder named after the URL) and run `analyze` on it. `-branch`, `-depth` (shallow clone), and the `analyze` options. An existing clone is fetched and reset to the remote branch. Private HTTPS repos: set `SOKRATES_GIT_TOKEN` (and optionally `SOKRATES_GIT_USER`) |
| `analyze` | One-shot analysis: `extractGitHistory` (if the root is a git repository) + `init` (if no config exists yet) + `generateReports`. Options: `-srcRoot`, `-confFile`, `-outputFolder`, `-conventionsFile`, `-name`, `-description`, `-skipGitHistory`, `-date`, `-timeout` |
| `init` | Create a new analysis configuration (`config.json`) from standard + optional custom conventions |
| `generateReports` | Run the analysis and generate the HTML/JSON reports |
| `updateConfig` | Fill in missing fields of an existing configuration |
| `addCustomTab` | Add a custom iframe tab (`-label`, `-iframeLink`) to the report config; a tab with the same label is overwritten |
| `analyzeLandscape` | Create/update a landscape report that aggregates multiple analyses (the landscape counterpart of `analyze`) |
| `updateLandscape` | Older name of `analyzeLandscape`, kept for existing scripts; identical behavior and options |
| `updateLandscapePeopleConfigByUserName` | Build/update `config-people.json` by grouping contributor emails sharing a display name (userName) under one entry (additive — appends new emails only) |
| `updatePeopleConfigByUserName` | Single-repository version: build/update `_sokrates/config-people.json` from the repo's `git-history.txt` (run after `extractGitHistory`; no `generateReports` needed) |
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

### Searching the landscape repositories & contributors lists

The landscape's repositories and contributors reports have a search box that takes `;`-separated
terms. Besides plain name/email terms, you can filter by language:

| Term | Matches |
| --- | --- |
| `mainLang:cs,java` | items whose **main** language is C# or Java |
| `includesLang:go` | repositories/contributors that use Go **anywhere**, even if it is not the main language |
| `includesLang:main:tf` | repositories with Terraform in the **main** scope only (repos only; scope ∈ `main`/`test`/`build`/`generated`/`other`) |
| `team:platform` | contributors whose team name contains "platform" (contributors only) |

Language terms combine (AND) with name terms; commas mean OR. Clicking a language icon in a row, or a
language badge on the Overview tab, opens the matching filtered list. The whole query can also be put in
the URL fragment, e.g. `repositories.html#mainLang:cs,java;customer`.

## Run the GUI explorer

```bash
java -jar codeexplorer-1.0-jar-with-dependencies.jar
```

## Docker

A prebuilt image is published to the GitHub Container Registry on every push to `master` (`latest`) and on version tags (`vX.Y.Z` → `X.Y.Z`), for `linux/amd64` and `linux/arm64`. Mount the code base at `/code` (the image's working directory) and pass any CLI command; with no command it runs `analyze`:

```bash
# One-shot analysis of the current directory (history + config + reports)
docker run --rm -v "$(pwd):/code" ghcr.io/zeljkoobrenovic/sokrates analyze

# Any other command works the same way
docker run --rm -v "$(pwd):/code" ghcr.io/zeljkoobrenovic/sokrates generateReports -help
docker run --rm -v "$(pwd):/code" ghcr.io/zeljkoobrenovic/sokrates analyzeLandscape -analysisRoot .

# Pin a version instead of latest
docker run --rm -v "$(pwd):/code" ghcr.io/zeljkoobrenovic/sokrates:1.0.0 analyze
```

On Linux the container writes as root, so the generated `_sokrates/` folder would be owned by root; add `--user "$(id -u):$(id -g)"` to keep your own ownership (Docker Desktop on macOS/Windows maps ownership automatically).

To build the image yourself:

```bash
docker build -t sokrates -f dockerfile .
docker run --rm -v "$(pwd):/code" sokrates analyze
```

The image is a plain JRE — the git history is read with JGit and graphs render in the browser, so no git, Graphviz or other native tooling is needed. The build is defined in [`dockerfile`](dockerfile) and published by [`.github/workflows/docker-publish.yml`](.github/workflows/docker-publish.yml).

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

