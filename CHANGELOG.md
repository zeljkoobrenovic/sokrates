# Release notes

User-visible behaviour changes, newest first. `:latest` of the Docker image and the `master` branch carry
everything listed under the most recent date.

## 2026-10-04

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
