# Sokrates Configuration Manual

Sokrates is driven almost entirely by two JSON configuration files. Most behaviour is
controlled by editing these files.

| File | Purpose | Created by |
| --- | --- | --- |
| `_sokrates/config.json` | **Repository analysis** — how a single code base is scoped, analysed, decomposed, tagged, and what goals/controls apply. | `sokrates init` |
| `_sokrates_landscape/config.json` | **Landscape** — how many repository analyses are aggregated into a portfolio report. | `sokrates updateLandscape` |
| `analysis_conventions.json` | **Custom scoping conventions** (optional) — reusable rules fed into `init` to control how files are classified, which extensions are scanned, and the defaults baked into the generated `_sokrates/config.json`. | `sokrates createConventionsFile` / `exportStandardConventions` |

Both files are read with a tolerant JSON parser: **unknown keys are ignored**, and any key
you omit falls back to its default. You can therefore keep your config minimal and add only
the keys you want to change. All keys are camelCase and match the field names exactly.

The typical flow is:

```bash
sokrates init -srcRoot <path>        # create _sokrates/config.json
# edit _sokrates/config.json
sokrates generateReports             # analyse + emit HTML reports

# then, in a folder containing many analysed repositories:
sokrates updateLandscape             # create/update _sokrates_landscape/config.json + report
```

---

# Part 1 — Repository analysis: `_sokrates/config.json`

Created in the `_sokrates` folder by `sokrates init`. Defines a single repository's analysis.
Backed by `CodeConfiguration` (`codeanalyzer/.../sourcecode/core/CodeConfiguration.java`).

## Top-level keys

| Key | Type | Default | Meaning |
| --- | --- | --- | --- |
| `metadata` | object | `{}` | Name, description, tooltip, logo and links shown on the report. |
| `summary` | string[] | `[]` | Bullet-point key findings shown on the report index. |
| `srcRoot` | string | `".."` | Relative path from the config file to the source code root. |
| `extensions` | string[] | detected | File extensions to analyse (others are skipped). |
| `ignore` | filter[] | `[]` | Path/content rules for files to exclude entirely. |
| `main` / `test` / `generated` / `buildAndDeployment` / `other` | aspect | generated | The five **scope aspects** that classify every file. |
| `logicalDecompositions` | object[] | one `primary` | How `main` files are grouped into components for dependency analysis. |
| `concernGroups` | object[] | one `general` (TODOs) | Cross-cutting "features of interest" matched by path/content. |
| `goalsAndControls` | object[] | one default goal | Metric thresholds (traffic lights) tied to goals. |
| `trendAnalysis` | object | see below | Saving/comparing historical snapshots. |
| `fileHistoryAnalysis` | object | see below | Git-history (contributors) analysis options. |
| `analysis` | object | see below | Thresholds and switches controlling the analysis itself. |
| `tagRules` | object[] | default tags | Tag the repository by file-path patterns. |

### `metadata`

```json
"metadata": {
  "name": "My Service",
  "description": "Payments service",
  "tooltip": "",
  "logoLink": "",
  "links": [ { "label": "Repo", "href": "https://github.com/org/my-service" } ]
}
```

### Scope aspects (`main`, `test`, `generated`, `buildAndDeployment`, `other`)

Every file is classified into exactly one scope. Each aspect is a named set of files defined
by `sourceFileFilters` (regex on path and/or content) or an explicit `files` list:

```json
"test": {
  "name": "test",
  "sourceFileFilters": [
    { "pathPattern": ".*/test/.*", "contentPattern": "", "exception": false, "note": "" }
  ],
  "files": []
}
```

A **`SourceFileFilter`** has: `pathPattern` (regex on the file path), `contentPattern` (regex on
a line of code), `exception` (`true` flips it into an *exclude* rule), and an optional `note`.
The same filter shape is reused throughout the config (`ignore`, decomposition filters, concerns).

### `logicalDecompositions`

Groups `main` files into components and renders their dependencies. The default decomposition
`primary` groups by the first folder level:

```json
"logicalDecompositions": [
  {
    "name": "primary",
    "scope": "main",
    "componentsFolderDepth": 1,         // group by Nth folder level
    "components": [],                   // or define components explicitly via filters
    "includeRemainingFiles": true,
    "dependenciesFinder": { "useBuiltInDependencyFinders": true, "rules": [], "metaRules": [] },
    "renderingOptions": { "orientation": "TB", "maxNumberOfDependencies": 100 }
  }
]
```

Use `components` (each a named aspect with filters) to define components explicitly, or
`metaComponents` to derive component names from path/content. `dependenciesFinder.rules` add
explicit regex-based dependency links beyond the language built-ins.

### `concernGroups`

Concerns ("features of interest") classify files that match a pattern, independent of scope.
The default group flags TODOs/FIXMEs:

```json
"concernGroups": [
  { "name": "general",
    "concerns": [
      { "name": "TODOs", "sourceFileFilters": [ { "contentPattern": ".*(TODO|FIXME)( |:|\\t).*" } ] }
    ] }
]
```

### `goalsAndControls`

A goal groups one or more **controls**: each control names a Sokrates `metric` and a
`desiredRange` (`min`, `max`, `tolerance` — numeric strings); the report shows a green/yellow/red
light per control. The default goal keeps size/duplication/file-size/complexity in check:

```json
"goalsAndControls": [
  { "goal": "Keep the system simple and easy to change",
    "controls": [
      { "metric": "LINES_OF_CODE_MAIN", "desiredRange": { "min": "0", "max": "200000", "tolerance": "20000" } },
      { "metric": "DUPLICATION_PERCENTAGE", "desiredRange": { "min": "0", "max": "5", "tolerance": "1" } }
    ] }
]
```

### `analysis`

Switches and risk thresholds for the analysis itself. Common keys:

| Key | Default | Meaning |
| --- | --- | --- |
| `skipDuplication` | `false` | Skip duplication analysis. |
| `skipDependencies` | `false` | Skip component dependency analysis. |
| `saveSourceFiles` / `saveCodeFragments` | `true` | Save copies/snippets linked from reports. |
| `maxFileSizeBytes` / `maxLines` / `maxLineLength` | `1000000` / `10000` / `1000` | Skip files exceeding these. |
| `locDuplicationThreshold` | `10000000` | Skip duplication when main LOC exceeds this. |
| `minDuplicationBlockLoc` | `6` | Minimum duplicated block size (lines). |
| `fileSizeThresholds` | `{low:100, medium:200, high:500, veryHigh:1000}` | Risk bands for file size (LOC). |
| `unitSizeThresholds` | `{low:10, medium:20, high:50, veryHigh:100}` | Risk bands for unit size. |
| `conditionalComplexityThresholds` | `{low:5, medium:10, high:25, veryHigh:50}` | Risk bands for unit complexity. |
| `fileAgeThresholds` / `fileUpdateFrequencyThresholds` | (see source) | Risk bands for history metrics. |

Each `*Thresholds` is an object `{ "low", "medium", "high", "veryHigh" }`. `analyzerOverrides`
can force a specific language analyzer for files matching given filters.

### `fileHistoryAnalysis`

Drives contributor/commit analysis from an exported git history:

```json
"fileHistoryAnalysis": {
  "importPath": "../_git_history.json",
  "bots": [".*\\[bot\\].*", ".*[-]bot[@].*"],
  "ignoreContributors": [],
  "anonymizeContributors": false,
  "transformContributorEmails": [ { "op": "replace", "params": ["[@].*", ""] } ]
}
```

`transformContributorEmails` applies string operations (`replace`, `extract`, `remove`,
`trim`, `lowercase`, …) to normalise contributor identities.

### `trendAnalysis`

```json
"trendAnalysis": { "referenceAnalysesFolder": "history", "saveHistory": false, "frequency": "weekly", "maxReferencePointsForAnalysis": 20 }
```

### `tagRules`

Tag the repository by file-path patterns (used to recognise technologies):

```json
"tagRules": [ { "tag": "Docker", "color": "", "pathPatterns": [".*Dockerfile.*"], "excludePathPatterns": [] } ]
```

---

# Part 2 — Landscape: `_sokrates_landscape/config.json`

Created/updated by `sokrates updateLandscape` in a folder that contains one or more analysed
repositories (each with a `data/analysisResults.json`). Backed by `LandscapeConfiguration`
(`codeanalyzer/.../sourcecode/landscape/LandscapeConfiguration.java`).

`updateLandscape` discovers the repositories (and any folder sub-landscapes) and stores that
discovered index in a companion **`info.json`**; the discovered lists are *not* written into
`config.json`, so you only ever edit the human-authored settings below.

## Top-level keys

| Key | Type | Default | Meaning |
| --- | --- | --- | --- |
| `metadata` | object | `{}` | Landscape name, description, logo, links. |
| `virtualLandscapes` | object | empty | Define sub-landscapes by repository-name patterns (see below). |
| `analysisRoot` | string | `"."` | Relative path to the folder holding the repository analyses. |
| `repositoryReportsUrlPrefix` | string | `"../"` | Prefix used to link from the landscape to repository reports. |
| `parentUrl` | string | `""` | Optional: clicking the landscape title navigates here. |
| `breadcrumbs` | link[] | `[]` | Breadcrumb links reflecting the hierarchy. |
| `extensionThresholdLoc` | int | `0` | Min main LOC for an extension to be listed. |
| `repositoryThresholdLocMain` | int | `0` | Min main LOC for a repository to be included. |
| `repositoryThresholdContributors` | int | `1` | Min unique contributors for a repository to be included. |
| `contributorThresholdCommits` | int | `1` | Min commits for a contributor to be counted. |
| `ignoreRepositoriesLastUpdatedBefore` | string | `""` | `YYYY-MM-DD`; exclude repositories not updated after this date. |
| `commitsMaxYears` | int | `10` | Years of commit history to display. |
| `significantContributorMinCommitDaysPerYear` | int | `10` | Commit-days/year to count as a "significant" contributor. |
| `anonymizeContributors` | boolean | `false` | Replace contributor identities with anonymous IDs. |
| `showRepositoryControls` | boolean | `true` | Show goal/control status per repository. |
| `repositoriesShortListLimit` | int | `100` | Rows in the short repository preview. |
| `repositoriesListLimit` | int | `1000` | Rows in the full repository list. |
| `repositoriesHistoryLimit` | int | `30` | Years of per-repository history shown. |
| `contributorsListLimit` | int | `1000` | Rows in the contributor list. |
| `contributorLinkTemplate` | string | `""` | Template for an external contributor page; `${contributorid}` is substituted. |
| `contributorAvatarLinkTemplate` | string | `""` | Template for a contributor avatar image; `${contributorid}` is substituted. |
| `ignoreContributors` | string[] | `[]` | Regex patterns of contributors to drop. |
| `bots` | string[] | bot defaults | Regex patterns identifying bot accounts. |
| `tagContributors` | object[] | `[]` | Tag contributors by email patterns (e.g. `extern`). |
| `ignoreExtensions` | string[] | `[]` | Extensions to ignore landscape-wide. |
| `includeOnlyOneRepositoryWithSameName` | boolean | `true` | Deduplicate repositories sharing a name. |
| `mergeExtensions` | object[] | `[]` | Merge a secondary extension into a primary one. |
| `transformContributorEmails` | object[] | `[]` | Normalise contributor identities (same op format as repo config). |
| `showExtensionsOnFirstTab` | boolean | `true` | Show the extensions block on the Overview tab. |
| `showContributorsTrendsOnFirstTab` | boolean | `true` | Show contributor/commit trends on the Overview tab. |
| `maxSublandscapeDepth` | int | `0` | Max sub-landscape nesting depth shown (`0` = all). |
| `iFramesAtStart` / `iFrames` | object[] | `[]` | Embedded web frames at the start/end of the Overview tab. |
| `iFramesRepositoriesAtStart` / `iFramesRepositories` | object[] | `[]` | …of the Repositories tab. |
| `iFramesContributorsAtStart` / `iFramesContributors` | object[] | `[]` | …of the Contributors tab. |
| `customTabs` | object[] | `[]` | Extra tabs, each holding a list of embedded frames. |
| `customHtmlReportHeaderFragment` | string | `""` | Raw HTML injected into the report `<head>` (e.g. analytics). |

> Note: `subLandscapes` and `repositories` are populated automatically (kept in `info.json`),
> not edited by hand.

### `tagContributors`, `mergeExtensions`, `transformContributorEmails`

```json
"tagContributors": [ { "name": "extern", "patterns": [".*([-]|_)(ext|extern[0-9]*)@.*"] } ],
"mergeExtensions": [ { "primary": "tf", "secondary": "tfvars" } ],
"transformContributorEmails": [ { "op": "replace", "params": ["[@].*", ""] } ]
```

### Embedded frames and custom tabs

An iFrame entry is a `WebFrameLink`: `{ "title", "src", "style", "scrolling", "moreInfoLink" }`.
A `customTab` is `{ "name", "iFrames": [ … ] }`.

### `virtualLandscapes`

Virtual landscapes let you define sub-landscapes **by repository-name regex patterns**, without
moving report folders. Each defined landscape gets a full landscape report under
`_sokrates_landscape/landscapes/<name>/_sokrates_landscape/`, and a **Remainder** landscape
collects repositories matched by none. They appear in the parent's "Sub-landscapes" tab, and the
sub-landscape zoomable-circles/sunburst visuals group repositories into one circle per virtual
landscape (plus Remainder) rather than by folder path. A repository may belong to several virtual
landscapes. The feature is inert when `landscapes` is empty, and coexists with folder-based
sub-landscapes.

```json
"virtualLandscapes": {
  "remainderLandscapeMetadata": { "name": "Remainder" },
  "landscapes": [
    {
      "metadata": { "name": "Datadog", "description": "Datadog-related code" },
      "includeRepoNamePatterns": [".*datadog.*"],
      "excludeRepoNamePatterns": []
    }
  ]
}
```

A repository is a member when its name matches **any** include pattern and **no** exclude
pattern.

**Nesting (unlimited depth).** A virtual landscape may itself define `virtualLandscapes`, which
partition *its own* repositories into nested virtual landscapes (each again with its own
Remainder). Nested landscapes are generated under
`…/landscapes/<parent>/_sokrates_landscape/landscapes/<child>/_sokrates_landscape/` and appear in
their parent virtual landscape's own "Sub-landscapes" tab. There is no depth limit.

```json
"virtualLandscapes": {
  "landscapes": [
    {
      "metadata": { "name": "Datadog" },
      "includeRepoNamePatterns": [".*datadog.*"],
      "virtualLandscapes": {
        "landscapes": [
          { "metadata": { "name": "Agents" }, "includeRepoNamePatterns": [".*agent.*"] }
        ]
      }
    }
  ]
}
```

## Companion files in `_sokrates_landscape/`

These optional files sit beside `config.json` and are created (empty) on first run. Edit them
to enrich the landscape report; they are re-read on every `updateLandscape`.

| File | Holds |
| --- | --- |
| `config-tags.json` | Repository tags — classify/group repositories (by name, extension, or path). |
| `config-teams.json` | Teams — map contributors to teams by email patterns. |
| `config-people.json` | People — per-contributor profiles (display name, avatar, links). |
| `info.json` | **Auto-generated** index of discovered repositories and sub-landscapes — do not edit. |

### `config-tags.json` — repository tags

A JSON **array of tag groups**. Each group has a `name`, optional `description`/`color`, and a
list of `repositoryTags`. A repository gets a tag if it matches the tag's rules; tags drive the
"Tags" tab and the per-repository tag badges.

A **tag group** (`TagGroup`): `name`, `description`, `color`, `repositoryTags[]`.

A **repository tag** (`RepositoryTag`) matches a repository when any rule below matches (and no
exclude rule does):

| Key | Type | Meaning |
| --- | --- | --- |
| `tag` | string | The tag label shown in the report. |
| `patterns` | string[] | Regex on the repository **name** to include. |
| `excludePatterns` | string[] | Regex on the name to exclude. |
| `mainExtensions` | string[] | Match when one of these is the repository's dominant extension. |
| `anyExtensions` | string[] | Match when the repository contains any of these extensions. |
| `excludeExtensions` | string[] | Exclude when any of these extensions is present. |
| `pathPatterns` | string[] | Regex on file paths within the repository to include. |
| `excludePathPatterns` | string[] | Regex on file paths to exclude. |
| `imageLink` | string | Optional icon URL shown next to the tag. |

```json
[
  {
    "name": "Technology",
    "description": "Primary technology",
    "color": "#dddddd",
    "repositoryTags": [
      {
        "tag": "Java",
        "patterns": [],
        "excludePatterns": [],
        "mainExtensions": ["java"],
        "anyExtensions": [],
        "excludeExtensions": [],
        "pathPatterns": [],
        "excludePathPatterns": [],
        "imageLink": "https://.../java.png"
      },
      {
        "tag": "Terraform",
        "anyExtensions": ["tf"]
      }
    ]
  }
]
```

### `config-teams.json` — teams

A JSON **object** with a `teams` array. Each `TeamConfig` maps contributors to a team by
matching their email against regex `emailPatterns`. Teams get their own report (a `teams.html`
landscape alongside `contributors.html`).

| Key | Type | Meaning |
| --- | --- | --- |
| `name` | string | Team name. |
| `description` | string | Optional description. |
| `emailPatterns` | string[] | Regex patterns matched against contributor emails. |

```json
{
  "teams": [
    { "name": "Payments", "description": "", "emailPatterns": [".*@payments\\.example\\.com"] },
    { "name": "Platform",  "description": "", "emailPatterns": ["alice@example\\.com", "bob@example\\.com"] }
  ]
}
```

### `config-people.json` — people

A JSON **object** with a `people` array. Each `PersonConfig` enriches a contributor (identified
by `emailPatterns`) with a display name, an avatar image, and profile links shown on their
individual contributor report.

| Key | Type | Meaning |
| --- | --- | --- |
| `name` | string | Display name. |
| `image` | string | Avatar image URL (overrides `contributorAvatarLinkTemplate`). |
| `links` | link[] | Profile links (`{ "label", "href" }`) shown on the contributor page. |
| `link` | string | Legacy single link (prefer `links`). |
| `emailPatterns` | string[] | Regex patterns matched against the contributor's email. |

```json
{
  "people": [
    {
      "name": "Alice Example",
      "image": "https://.../alice.png",
      "links": [ { "label": "GitHub", "href": "https://github.com/alice" } ],
      "emailPatterns": ["alice@example\\.com", ".*alice.*@example\\.com"]
    }
  ]
}
```

> The same `bots`, `ignoreContributors`, `anonymizeContributors`, and `transformContributorEmails`
> options on the landscape `config.json` (Part 2) also shape how contributors are detected and
> normalised before teams/people matching is applied.

## Folder layout

```text
analysis-root/
├── _sokrates_landscape/
│   ├── config.json            # landscape settings (this manual, Part 2)
│   ├── config-tags.json       # optional repository tags
│   ├── config-teams.json      # optional teams
│   ├── config-people.json     # optional people profiles
│   ├── info.json              # auto-generated repository/sub-landscape index
│   ├── index.html             # generated landscape report
│   ├── landscapes/            # generated virtual landscapes (if configured)
│   ├── contributors/          # generated per-contributor reports
│   ├── data/                  # generated JSON exports
│   └── visuals/               # generated charts
├── repo-a/data/analysisResults.json   # a repository analysis (from `generateReports`)
├── repo-b/data/analysisResults.json
└── team-x/_sokrates_landscape/         # a folder-based sub-landscape (optional)
```

---

# Part 3 — Custom scoping conventions: `analysis_conventions.json`

`_sokrates/config.json` (Part 1) is produced by `sokrates init`, which scans your source tree
and applies a large set of **standard conventions** — rules that decide which file extensions
are source code, and which files are tests, generated, build-and-deployment, "other", or
ignored. Those standard conventions are baked into Sokrates (`ScopingConventions.java`) and
are based on GitHub's [linguist](https://github.com/github/linguist) heuristics.

A **conventions file** lets you override or extend that classification step *before* the
`config.json` is written, and to set the defaults baked into the generated config. It is
optional: omit it and `init` uses only the standard conventions. Pass it with `-conventionsFile`:

```bash
sokrates init -srcRoot <path> -conventionsFile analysis_conventions.json
```

Backed by `CustomScopingConventions` (`codeanalyzer/.../sourcecode/scoping/custom/`).

## When to use it vs. editing `config.json`

| | `analysis_conventions.json` | `_sokrates/config.json` |
| --- | --- | --- |
| Applied | at `init` time, before the config is generated | at `generateReports` time |
| Scope | **reusable** across many repositories (a portfolio standard) | a single repository |
| Granularity | matched against the files actually present, then materialised into the config | the literal, already-resolved rules |

Use a conventions file when you have a house style you want every repository's `init` to
follow (e.g. always treat `*.gen.go` as generated, always ignore a vendored folder, always
flag a set of concerns). For a one-off tweak to a single repository, edit its `config.json`
directly. The two compose: custom conventions are *added on top of* the standard ones unless
you opt out (see `ignoreStandardScopingConventions` below).

## Creating one

```bash
sokrates createConventionsFile      # writes an empty analysis_conventions.json (all defaults)
sokrates exportStandardConventions  # writes standard_analysis_conventions.json (the full
                                    #   built-in standard rules, as a starting point to edit)
```

`createConventionsFile` is the usual starting point — you add only the keys you want. Use
`exportStandardConventions` when you want to see (and selectively change) the complete set of
built-in rules.

## How conventions are applied at `init`

`ScopeCreator.createScopeFromConventions(...)` builds the config in this order:

1. **Extensions** — the source tree is scanned and each extension is kept or dropped by
   `extensions.onlyInclude` / `extensions.alwaysExclude` (below), falling back to Sokrates's
   list of known source-code extensions.
2. **Standard scoping** — unless `ignoreStandardScopingConventions` is `true`, every standard
   ignore/test/generated/build/other rule that matches a file actually present is added to the
   corresponding aspect.
3. **Custom scoping** — your custom `*FilesConventions` are matched against the present files
   and appended to the same aspects (a rule already added by the standard pass is not
   duplicated). Concerns, contributor/bot filters, tag rules, goals/controls, analysis and
   file-history settings, logo, and component-folder depth are applied as described below.

A convention only lands in the generated `config.json` if it **matches at least one file** in
the scanned source tree — the output stays specific to that repository.

## Keys

| Key | Type | Default | Meaning |
| --- | --- | --- | --- |
| `extensions` | object | `{}` | Extension allow/deny list applied before scoping (see below). |
| `ignoreStandardScopingConventions` | bool | `false` | If `true`, skip the built-in scoping rules entirely and use **only** the custom `*FilesConventions`. |
| `removeStandardConcerns` | bool | `false` | If `true`, drop the default concern groups before adding custom `concerns`. |
| `logoLink` | string | `""` | Logo URL written into the generated config's `metadata.logoLink`. |
| `ignoredFilesConventions` | filter[] | `[]` | Custom rules that classify files as **ignored** (excluded entirely). |
| `testFilesConventions` | filter[] | `[]` | Custom rules that classify files into the **test** scope. |
| `generatedFilesConventions` | filter[] | `[]` | Custom rules that classify files into the **generated** scope. |
| `buildAndDeploymentFilesConventions` | filter[] | `[]` | Custom rules that classify files into the **buildAndDeployment** scope. |
| `otherFilesConventions` | filter[] | `[]` | Custom rules that classify files into the **other** scope. |
| `concerns` | concern[] | `[]` | Concerns appended to the first concern group (a `general` group is created if none exists). Same shape as a single concern in Part 1's `concernGroups`. |
| `ignoreContributors` | string[] | `[]` | Regexes added to `fileHistoryAnalysis.ignoreContributors`. |
| `bots` | string[] | `[]` | Regexes added to `fileHistoryAnalysis.bots`. |
| `componentsFolderDepth` | int | `1` | Folder depth used to derive components in the primary logical decomposition. |
| `minComponentsCount` | int | `0` | If `> 0`, overrides `componentsFolderDepth` by searching for a depth that yields at least this many components. |
| `ignoreStandardControls` | bool | `false` | If `true`, clear the default goals/controls before adding custom `goalsAndControls`. |
| `goalsAndControls` | object[] | `[]` | Custom goals/controls (same shape as Part 1's `goalsAndControls`), added to the config. |
| `fileHistoryAnalysis` | object | defaults | Replaces the generated config's `fileHistoryAnalysis` block (same shape as Part 1). |
| `analysis` | object | defaults | Replaces the generated config's `analysis` block (same shape as Part 1). |
| `tagRules` | object[] | `[]` | If non-empty, **replaces** the default `tagRules` in the generated config. |

Any file aspect rule with no files matching it is silently skipped, so unused conventions
never bloat the generated config.

### `extensions`

Controls which file extensions are even considered, *before* any scoping happens:

```json
"extensions": {
  "onlyInclude": ["java", "kt"],
  "alwaysExclude": ["min.js", "map"]
}
```

- `onlyInclude` — if non-empty, **only** these extensions are analysed; everything else is
  dropped (and `alwaysExclude` is ignored). A whitelist.
- `alwaysExclude` — extensions to drop. A blacklist, applied only when `onlyInclude` is empty.

Comparison is case-insensitive. With both empty, Sokrates falls back to its built-in list of
known source-code extensions.

### Scoping convention lists (`*FilesConventions`)

Each entry is a **`SourceFileFilter`** — the same shape used everywhere else in the config
(`pathPattern`, `contentPattern`, optional `exception`, optional `note`). For conventions, a
`contentPattern` may additionally be bounded by `maxLinesForContentSearch` (how many leading
lines of the file to scan when matching content — useful for "generated" banner comments).

```json
"generatedFilesConventions": [
  { "pathPattern": ".*[.]gen[.]go", "contentPattern": "", "note": "Codegen output" },
  { "pathPattern": ".*[.]go", "contentPattern": "// Code generated .* DO NOT EDIT[.]", "note": "Go codegen banner" }
],
"ignoredFilesConventions": [
  { "pathPattern": ".*/thirdparty/.*", "contentPattern": "", "note": "Vendored code" }
],
"testFilesConventions": [
  { "pathPattern": ".*/it/.*", "contentPattern": "", "note": "Integration tests" }
]
```

A file matches a convention when its path matches `pathPattern` **and** (if set) some line
matches `contentPattern`. Leave `pathPattern` empty to match purely on content.

### Full example

```json
{
  "extensions": { "alwaysExclude": ["min.js"] },
  "ignoreStandardScopingConventions": false,
  "removeStandardConcerns": false,
  "logoLink": "https://example.com/logo.png",
  "testFilesConventions": [
    { "pathPattern": ".*/it/.*", "contentPattern": "", "note": "Integration tests" }
  ],
  "generatedFilesConventions": [
    { "pathPattern": ".*[.]gen[.]go", "contentPattern": "", "note": "Codegen output" }
  ],
  "ignoredFilesConventions": [
    { "pathPattern": ".*/thirdparty/.*", "contentPattern": "", "note": "Vendored code" }
  ],
  "concerns": [
    { "name": "TODOs", "sourceFileFilters": [ { "contentPattern": ".*(TODO|FIXME)( |:|\\t).*" } ] }
  ],
  "bots": [ ".*\\[bot\\].*" ],
  "ignoreContributors": [ ".*-ci@example.com" ],
  "componentsFolderDepth": 2,
  "tagRules": [
    { "tag": "backend", "pathPatterns": [".*/server/.*"] }
  ]
}
```

This file is reusable: point every repository's `sokrates init` at it
(`-conventionsFile analysis_conventions.json`) to get a consistent classification across a
whole portfolio.
