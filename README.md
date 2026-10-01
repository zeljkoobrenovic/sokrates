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

The report is titled after the repository, not the folder: when the folder has a git `origin` remote, `analyze` fills the configuration's name (`owner/repo`, e.g. `junit-team/junit4`, so same-named repositories of different owners stay apart in a landscape), logo (GitHub owner avatar), description (from the GitHub API, best effort; set `SOKRATES_OFFLINE=1` to skip the lookup) and a link to the repository, unless you set them yourself (`-name`, `-description`, `-logoLink`, `-addLink`, or edits in `config.json`). This matters in Docker, where every code base is mounted at `/code`.

### A landscape of many repositories in one command

List the repositories in a text file, one git URL per line (blank lines and `#` comments are ignored), and run `analyzeLandscape -urls` in the folder that should hold the landscape:

```bash
cat > repos.txt <<'EOF'
# repositories of the landscape
https://github.com/junit-team/junit4
https://github.com/junit-team/junit-framework
https://github.com/hamcrest/JavaHamcrest
EOF

java -jar cli-1.0-jar-with-dependencies.jar analyzeLandscape -urls repos.txt
# or, without Java:
docker run --rm -v "$(pwd):/code" ghcr.io/zeljkoobrenovic/sokrates analyzeLandscape -urls repos.txt

open _sokrates_landscape/index.html
```

Every URL is cloned and analyzed (the `analyzeGitRepo` step: only the analysis is kept, under `<owner>/<repo>/`; a repository that cannot be cloned is logged and skipped), then the landscape report is built over all of them:

```
./
  _sokrates_landscape/index.html        # the landscape report
  junit-team/junit4/config.json
  junit-team/junit4/reports/...
  junit-team/junit-framework/...
  hamcrest/JavaHamcrest/...
```

A few URLs can also be passed inline with `-url` (repeatable); `-dataOnly` keeps just each repository's `data.zip` (all a landscape needs) and the landscape's own `data.zip` (all a parent landscape needs) — much smaller when you only want the data, `-depth <n>` makes the clones shallow, `-analysisRoot` puts the landscape elsewhere, and `SOKRATES_GIT_TOKEN` authenticates private repositories. Re-run the same command to refresh: the kept `config.json` of each repository and the landscape configuration are reused, so your tuning survives. A repository you remove from the list, or one that no longer exists, keeps its old analysis (and stays in the landscape) until you add `-prune`, which deletes it; only analyses the tool produced carry the `source.json` marker that makes them prunable, so analyses you placed by hand are safe. Without URLs, `analyzeLandscape` just aggregates the analyses already under the root.

### Whole GitHub organizations

`analyzeGitHubOrg` does the listing for you: give it organization (or user) logins and it asks the GitHub API for their repositories, filters them, analyzes each one and builds a landscape per organization, named, described, linked and branded from the organization's GitHub profile:

```bash
java -jar cli-1.0-jar-with-dependencies.jar analyzeGitHubOrg -org junit-team -org hamcrest -pushedWithinDays 365
# or: docker run --rm -v "$(pwd):/code" ghcr.io/zeljkoobrenovic/sokrates analyzeGitHubOrg -org junit-team -org hamcrest -pushedWithinDays 365

open junit-team/_sokrates_landscape/index.html   # one landscape per organization ...
open _sokrates_landscape/index.html              # ... and a parent landscape over them
```

```
./
  _sokrates_landscape/index.html              # parent landscape (when there are several organizations)
  junit-team/_sokrates_landscape/index.html   # the organization's landscape
  junit-team/repos.txt                        # the selected repositories (re-usable with analyzeLandscape -urls)
  junit-team/junit4/config.json + reports/
  junit-team/junit-framework/...
  hamcrest/...
```

`analyzeGitLabGroup` is the same for GitLab groups, with their subgroups, on gitlab.com or a self-hosted instance (`-group gitlab-org/ci-cd`, or a group URL such as `-group https://gitlab.example.com/acme/tools`, which also names the instance; `-gitlabUrl` sets it explicitly). Projects are kept in `<group path>/<project path>`, so subgroup folders are preserved.

Forks and archived repositories are skipped unless you pass `-includeForks` / `-includeArchived`; `-pushedWithinDays <n>`, `-includeRepoNamePattern` / `-excludeRepoNamePattern <regex>` and `-maxRepos <n>` narrow the selection further. Cloning a large organization takes a while, so `-listOnly` first writes just `<org>/repos.txt` to review the selection, and `-dataOnly` keeps only the data. Re-runs reuse every kept `config.json` and your edits of the landscape metadata; `-prune` removes the analyses of repositories that are no longer selected. For private repositories (and the higher API rate limit) set `SOKRATES_GIT_TOKEN`.

### Run an AI agent (or any script) after each analysis

`-postAnalysis "<command>"` runs a shell command in the analyzed source tree right after the analysis — for `analyzeGitRepo`, `analyzeLandscape -urls` and the organization commands inside the clone, before it is deleted — so an AI coding agent can read the source and the fresh `_sokrates/` analysis; whatever it writes under `_sokrates/` is kept with the analysis. `-ai claude|codex|gemini` is a preset for that agent's headless command running `-aiPrompt` (default `run a full scan`, the [sokrates-skills](https://github.com/zeljkoobrenovic/sokrates-skills) full scan); the agent CLI and the skills must be installed on the machine. The command sees `SOKRATES_REPO_URL`, `SOKRATES_REPO_NAME`, `SOKRATES_SRC_ROOT`, `SOKRATES_ANALYSIS_FOLDER`, `SOKRATES_REPORTS_FOLDER` and `SOKRATES_OUTPUT_FOLDER`; a failing command is logged and the analysis is kept.

```bash
java -jar cli-1.0-jar-with-dependencies.jar analyzeGitHubOrg -org junit-team -ai claude
java -jar cli-1.0-jar-with-dependencies.jar analyzeLandscape -urls repos.txt -ai codex -aiPrompt "run a tech stack scan"
java -jar cli-1.0-jar-with-dependencies.jar analyze -postAnalysis 'gemini -p "check the Sokrates configuration of this repository" --yolo'
```

The hook is incremental: each run is recorded in `_sokrates/post-analysis.json` (command, head commit, date, exit code) and a repository whose head commit and command are unchanged since a successful run is skipped, so a nightly landscape only rescans what moved; `-aiForce` runs it anyway and `-aiMaxRepos <n>` bounds the runs per invocation (the next invocation continues with the repositories not yet done).

The Docker image holds no agent CLI; use the JAR, or build an image on top of it with your agent installed.

To analyze a repository you have not cloned yet, `analyzeGitRepo` clones it first and then runs the same three steps:

```bash
java -jar cli-1.0-jar-with-dependencies.jar analyzeGitRepo -url https://github.com/junit-team/junit4
# → ./junit-team/junit4/config.json + ./junit-team/junit4/reports/ (the clone itself is not kept)
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
| `analyzeGitRepo` | Clone a repository from its URL (`-url`; JGit, no git binary) into a temporary folder, run `analyze` on it, and keep only the analysis (`config.json` + `reports/`) in `-destFolder` (default `<owner>/<repo>`, e.g. `junit-team/junit4`); the clone is deleted. Re-runs clone again and reuse the kept config. `-branch`, `-depth` (shallow clone), and the `analyze` options. Private HTTPS repos: set `SOKRATES_GIT_TOKEN` (and optionally `SOKRATES_GIT_USER`). The output layout is what `analyzeLandscape` expects |
| `analyze` | One-shot analysis: `extractGitHistory` (if the root is a git repository) + `init` (if no config exists yet) + `generateReports`. Options: `-srcRoot`, `-confFile`, `-outputFolder`, `-conventionsFile`, `-name`, `-description`, `-skipGitHistory`, `-date`, `-timeout` |
| `init` | Create a new analysis configuration (`config.json`) from standard + optional custom conventions |
| _`-postAnalysis`, `-ai`, `-aiPrompt`, `-aiMaxRepos`, `-aiForce`_ | On `analyze`, `analyzeGitRepo`, `analyzeLandscape` and the organization commands: run a shell command (or the `claude`/`codex`/`gemini` headless preset with a prompt) in the source tree after each analysis, while a clone still exists; its output under `_sokrates/` is kept. Incremental: skipped when the head commit and command are unchanged since a successful run (`-aiForce` overrides), at most `-aiMaxRepos` runs per invocation |
| `generateReports` | Run the analysis and generate the HTML/JSON reports. `-dataOnly` (also on `analyze`, `analyzeGitRepo` and `analyzeLandscape`) stores only `reports/data/data.zip` — the file landscapes read — and no HTML, explorers, visuals, source viewer or index page; on `analyzeLandscape`/`updateLandscape` it also keeps only the landscape's `_sokrates_landscape/data/data.zip` next to its config files |
| `updateConfig` | Fill in missing fields of an existing configuration |
| `addCustomTab` | Add a custom iframe tab (`-label`, `-iframeLink`) to the report config; a tab with the same label is overwritten |
| `analyzeLandscape` | Create/update a landscape report that aggregates multiple analyses (the landscape counterpart of `analyze`). With `-url <git url>` (repeatable) and/or `-urls <file>` (one URL per line, `#` comments) it first runs the `analyzeGitRepo` step for each repository into `<analysisRoot>/<owner>/<repo>` (a failing clone is logged and skipped), then builds the landscape. `-prune` deletes the kept analyses of repositories that are no longer listed or no longer exist (only analyses this tool produced, marked by a `source.json`; hand-placed ones are never touched); without it they are listed as stale. `-depth` and `-conventionsFile` apply to those analyses |
| `updateLandscape` | Older name of `analyzeLandscape`, kept for existing scripts; identical behavior and options |
| `analyzeGitHubOrg` | Analyze whole GitHub organizations (or users): `-org <login>` (repeatable) / `-orgs <file>`. Lists each organization's repositories with the GitHub API, filters them (forks and archived repos are excluded unless `-includeForks` / `-includeArchived`; `-pushedWithinDays <n>`, `-includeRepoNamePattern` / `-excludeRepoNamePattern <regex>`, `-maxRepos <n>`), writes the selection to `<org>/repos.txt`, analyzes each repository into `<org>/<repo>` and builds a landscape per organization in `<org>/_sokrates_landscape`, titled, described, linked and branded from the GitHub profile (blank fields only). Several organizations get a parent landscape on top. `-listOnly` is a dry run (just `repos.txt`), `-prune` deletes the tool's own analyses of repositories no longer selected or no longer existing; `-depth`, `-dataOnly`, `-conventionsFile` apply as in `analyzeLandscape`. `SOKRATES_GIT_TOKEN` for private repositories and the higher API rate limit |
| `analyzeGitLabGroup` | The GitLab counterpart: `-group <path>` (repeatable; `gitlab-org/ci-cd`, a URL, or a username) / `-groups <file>`, `-gitlabUrl` for a self-hosted instance (or give the group as a URL). Lists the projects of the group and its subgroups, applies the same filters and switches, keeps each project in `<group path>/<project path>` (subgroup folders kept) and builds `<group path>/_sokrates_landscape` from the group's profile. `SOKRATES_GIT_TOKEN` is sent as `PRIVATE-TOKEN` |
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

`docker run` reuses the image already on your machine and never checks for a newer one, so after a Sokrates update (a command reported as unknown is the usual symptom) run `docker pull ghcr.io/zeljkoobrenovic/sokrates` once, or add `--pull always` to the run command.

The JVM in the image may use 75% of the memory Docker gives the container. A big repository that still ends in `OutOfMemoryError: Java heap space` needs more: raise the memory of the Docker VM (Docker Desktop → Settings → Resources; Colima `colima start --memory 8`) or pass an explicit heap size, `-e JAVA_TOOL_OPTIONS=-Xmx8g`, which overrides the percentage.

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

