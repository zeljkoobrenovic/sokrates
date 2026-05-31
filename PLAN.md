# Sokrates — Improvement Plan

A prioritized list of improvements identified from a code review of the Sokrates
codebase (Maven multi-module project: `common`, `codeanalyzer`, `reports`, `cli`,
`codeexplorer`; ~472 main source files, ~135 test files).

Each item cites concrete locations so it can be picked up independently. Items are
grouped by theme and tagged with rough effort (S/M/L) and impact. Status tags:
`[x]` done, `[~]` in progress, `[ ]` not started.

---

## Recently completed (branch `add-tests-and-improvements`)

- **`cli` module tests** added (was 0). (§4)
- **`reports` golden-file tests** + `DataImageUtilsTest` + `RepositoryExportTest`
  added (was 1 test, no golden files). (§4)
- **Landscape reports rewritten as client-rendered, searchable pages.** A family of
  large static landscape reports was replaced by small embedded-JSON templates rendered
  in the browser, each with search + click-to-sort + show-more paging (see the
  "Two HTML rendering mechanisms" note in `CLAUDE.md`):
  - **Repositories** — the ~900-line `LandscapeRepositoriesReport` (a `repositories.html`
    that could reach ~77 MB of inlined SVG) was deleted; `repositories.html` is now the
    `repositories-report.html` template (~3 MB for 800+ repos), folding in the old
    `repositories-explorer.html`. Data: `RepositoryExport` / `RepositoryReportData`.
  - **Contributors / teams** — `contributors.html`/`teams.html`/`bots.html` (~5.4 MB)
    are now `contributors-report.html` (Recent / All time / Bots tabs, ~330 KB), embedded
    via iframes with `?tab=`. Data: `ContributorReportExport`. The old 1000-row list cap
    was removed (paging handles scale; a 3,986-contributor landscape now shows all).
  - **Individual contributor / team reports** — the 700+ `contributors/<email>.html`
    pages are now `contributor-report.html` (header + per-extension chart + Per Year/
    Month/Week activity grids re-gridded client-side from commit dates + Members table),
    ~3–4× smaller; repositories and members are searchable, and members are only linked
    when their individual report was actually generated. Data: `ContributorIndividualReportExport`.
  - **Visualisation templates hardened** — `force_2d.html`/`force_3d.html` gained a
    searchable/toggleable node list (sorted by size) and `bar_chart_races.html` now
    handles empty data / failed library load; all three pin their CDN library versions.
  - Partially relieves the `LandscapeReportGenerator`/`LandscapeReportContributorsTab`
    god-class pressure in §5 (rendering moved to templates + data exporters). Adds
    `DataImageUtilsTest`, `RepositoryExportTest`, `ContributorReportExportTest`,
    `ContributorIndividualReportExportTest`.

---

## 1. Error handling & logging

The project standardizes on Apache commons-logging (`Log`/`LogFactory`, used in 67
files) but there are **~85 `printStackTrace()` calls in main code** that bypass it.
Most print-and-continue, hiding failures from logs and from callers.

- [ ] **Replace `printStackTrace()` with proper logging** (and re-throw or handle
  where appropriate). Representative sites:
  - `cli/.../CommandLineInterface.java:564` — broad `catch (Exception)` around
    `generateReports` prints and swallows; a failed analysis looks like success.
  - `cli/.../CommandLineInterface.java:132` — `ParseException` both `LOG.info`'d
    and `printStackTrace`'d (duplicate/conflicting reporting).
  - `cli/.../git/GitHistoryExtractor.java:74,81` — per-commit IO failures swallowed;
    extraction reports success on partial output.
  - `codeanalyzer/.../sourcecode/SourceFile.java:101` — `InvalidPathException`
    swallowed after fallback.
  - `codeanalyzer/.../scoping/custom/CustomConventionsHelper.java` (6 sites) —
    config read/write exceptions swallowed, method returns `null` silently.
  - *Effort: M · Impact: High (observability, correct exit codes)*
- [ ] **Narrow broad `catch (Exception|Throwable)` blocks** (~10 in main code) so
  unexpected failures surface instead of being absorbed.
  - *Effort: S–M · Impact: Medium*
- [ ] **Decide on a single logging façade.** commons-logging, slf4j-api,
  slf4j-simple, and log4j-core are all on the classpath (root `pom.xml`). Pick one
  (slf4j + a single binding is the common choice) and route the rest through it to
  avoid ambiguous/duplicate logging config.
  - *Effort: M · Impact: Medium*

## 2. Security & robustness

- [ ] **Zip-slip in `UnzipToTempFilesUtil.unzip()`** (`common/.../utils/UnzipToTempFilesUtil.java:22`).
  `new File(destDirectory, entry.getName())` is used with no check that the resolved
  path stays inside `destDirectory`. *Currently the only caller is `Templates.java`,
  which unpacks bundled (trusted) template resources, so practical risk is low today*
  — but this is a reusable utility and the pattern is a latent CWE-22. Add a
  canonical-path containment check before writing.
  - *Effort: S · Impact: Medium (low exposure today, high if reused on untrusted zips)*
- [ ] **ReDoS / unbounded work from user-supplied regex.** Config-supplied patterns
  (`SourceFileFilter` path/content patterns, CLI `-pattern`) are compiled and run with
  no complexity bound or timeout.
  - `common/.../utils/RegexUtils.java` — `compiledPatterns` is an **unbounded static
    cache** (memory growth) and matching has no timeout.
  - `codeanalyzer/.../sourcecode/SourceFileFilter.java:47` — `Pattern.compile()` is
    called **inside the per-line loop** in `matchesAnyLine` (recompiles for every line
    of every file). Hoist compilation out of the loop; cache compiled patterns; bound
    the cache (e.g. LRU). Consider a guard for catastrophic patterns.
  - *Effort: S (hoist compile) – M (timeouts/cache bound) · Impact: Medium*
- [ ] **Path handling in `CodeConfiguration.getAbsoluteSrcRoot()`**
  (`codeanalyzer/.../core/CodeConfiguration.java:~117`) uses manual `startsWith("..")`
  / `substring(2)` string surgery instead of canonicalization. Use
  `getCanonicalPath()` and validate.
  - *Effort: S · Impact: Low–Medium*
- [ ] **Jackson deserialization is permissive** (`common/.../io/JsonMapper.java`):
  `FAIL_ON_INVALID_SUBTYPE` / `FAIL_ON_UNRESOLVED_OBJECT_IDS` disabled. No polymorphic
  default typing is enabled (good — no gadget risk), but silent type errors can mask a
  malformed `config.json`. Consider failing loudly or warning on these.
  - *Effort: S · Impact: Low*

## 3. Dependency & build hygiene

- [ ] **Add a `<dependencyManagement>` section** to the root `pom.xml`. Versions are
  currently declared directly in `<dependencies>` and re-declared across module poms,
  making upgrades error-prone.
  - *Effort: S · Impact: Medium*
- [ ] **Align the Log4j versions** — `log4j-api` is `2.25.2` while `log4j-core` is
  `2.25.4` (`pom.xml`). Pin both to the same version.
  - *Effort: S · Impact: Low*
- [ ] **Resolve Java-version inconsistency.** `pom.xml` targets Java 17;
  `buildspec.yml` builds on `corretto21`; `dockerfile` runs on `temurin-17`. Pick a
  target, make all three consistent, and add the **Maven Enforcer plugin** to fail
  builds on the wrong JDK.
  - *Effort: S · Impact: Medium*
- [ ] **Add static-analysis / coverage tooling** (none present today): JaCoCo
  (coverage), SpotBugs and/or Checkstyle. Wire into the build so regressions are
  visible.
  - *Effort: M · Impact: Medium*

## 4. Testing & CI

Test distribution is very uneven (concentrated in `codeanalyzer`):

| Module | main | test |
| --- | --- | --- |
| codeanalyzer | ~289 | ~123 |
| common | ~36 | ~8 |
| codeexplorer | ~37 | ~3 |
| reports | ~104 | ~6 |
| cli | ~6 | ~3 |

- [x] **Add tests for the `cli` module.** Done — `cli/src/test` now has tests for the
  command-line entry points (command dispatch / arg parsing).
  - *Effort: M · Impact: High*
- [~] **Add tests for `reports`.** In progress — golden-file tests now exist under
  `reports/src/test/resources/golden` (report generators compared against checked-in
  expected output), plus unit tests for `DataImageUtils` and the landscape
  `RepositoryExport` data model. Coverage of the remaining ~20 generators is still thin.
  - *Effort: L · Impact: High*
- [ ] **Finish the JUnit 4 → 5 migration.** ~109 tests use JUnit 4 (`org.junit`),
  ~15 use JUnit 5 (`org.junit.jupiter`); both engines (`jupiter` + `vintage`) are
  pulled in. Migrate to one style and drop `junit-vintage-engine`.
  - *Effort: M · Impact: Low–Medium*
- [ ] **Add a GitHub Actions CI workflow.** Only `buildspec.yml` (AWS CodeBuild)
  exists; there are no PR checks. Note the `dockerfile` builds with `-DskipTests`, so
  the container image is never validated by tests — CI should run `mvn install` (tests
  on) on PRs.
  - *Effort: S · Impact: Medium*

## 5. Architecture & maintainability

- [ ] **Break up the god classes.** Several files mix orchestration, HTML rendering,
  data aggregation, and styling:
  - `reports/.../landscape/statichtml/LandscapeReportGenerator.java` (~1836 lines)
  - `reports/.../core/ReportFileExporter.java` (~1228 lines)
  - `reports/.../dataexporters/DataExporter.java` (~959 lines)
  - `cli/.../CommandLineInterface.java` (~905 lines)
  Extract rendering helpers, per-tab/per-section builders, and command handlers.
  - *Effort: L · Impact: Medium (testability, readability)*
- [ ] **De-duplicate landscape report UI helpers.** `addInfoBlockWithColor`,
  `addLangInfoBlockExtra`, `addSmallInfoBlock`, `addIFrame(s)` and several
  `add*InfoBlock*` wrappers are duplicated between
  `LandscapeReportGenerator.java` and `LandscapeReportContributorsTab.java`
  (~200 lines). Extract a shared component/util.
  - *Effort: M · Impact: Medium*
- [ ] **Centralize hardcoded CSS/colors/dimensions.** Color constants exist
  (`LandscapeReportGenerator.java:107+`) but inline literals (`"#343434"`,
  `"lightgrey"`, inline `style="..."` strings, magic widths/heights) are used
  alongside them. Consolidate into a styling/theme helper.
  - *Effort: M · Impact: Low–Medium*
- [ ] **Reconsider mutable static state.**
  - `ReportFileExporter.htmlReportsSubFolder` is a mutated `static` field
    (not thread-safe; `exportHtml` writes it).
  - `LanguageAnalyzerFactory` is a static singleton with a non-final, mutable
    `analyzersMap`.
  - `CommandLineInterface.helpMode` is static control-flow state that persists across
    invocations in one JVM (problematic for embedding/testing).
  - *Effort: M · Impact: Medium*

## 6. Language analyzer registry

- [ ] **Make language registration data-driven.** `LanguageAnalyzerFactory` (~637
  lines) maps ~400 extensions to analyzer classes via inline `analyzersMap.put(...)` in
  the constructor. Consider externalizing the extension→analyzer mapping to a resource
  file (and instantiating via the existing factory), so adding/adjusting language
  support doesn't require editing a 600-line constructor.
  - *Effort: M · Impact: Low–Medium*
- [ ] **Improve the reflection error.** `aClass.newInstance()` failures throw
  `new RuntimeException("Error")` with no context. Include the extension and class
  name, and prefer `getDeclaredConstructor().newInstance()` (`newInstance()` is
  deprecated).
  - *Effort: S · Impact: Low*

---

## Suggested sequencing

1. **Quick, high-value wins:** logging cleanup on critical paths (§1), hoist regex
   compile out of the per-line loop (§2), `dependencyManagement` + Log4j alignment +
   Java-version/enforcer (§3), GitHub Actions CI (§4).
2. **Safety net:** add `cli` tests and `reports` golden-file tests (§4) before large
   refactors.
3. **Larger refactors:** break up god classes and de-duplicate landscape UI (§5),
   data-drive the language registry (§6).

> Note: severities for the security items reflect *current* exposure (e.g. zip-slip is
> only reachable via trusted bundled templates today). Re-evaluate if those utilities
> are ever pointed at untrusted input.
