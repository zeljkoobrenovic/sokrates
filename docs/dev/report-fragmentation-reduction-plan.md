# Implementation Plan: Reduce `generateReports` File Fragmentation

## Goal

`generateReports` emits 1000+ files per repository. The pain is **file count** (slow
sync/upload/copy of many tiny files — rsync, S3 sync, scp), not total size or write time.
Reduce the count dramatically while keeping **all data** and staying within Sokrates'
existing client-rendered-template idiom.

## Scope & constraints (decided)

- **`data/` is an external-tooling contract → untouched.** No changes to `data/*.json`,
  `data/text/*`, zips, or their paths.
- **`html/visuals/*` graph files → out of scope** (separate later phase).
- **`src/` is report-only → fully restructurable.**
- **Reports are always served over HTTP** (local server or hosted), never opened from
  `file://`. So client-side `fetch()` of zips/bundles is safe.
- **Compression must happen at generation time** (reports are copied as raw files; no
  on-the-wire server gzip to rely on).

## Current reality (verified against code)

`DataExporter.exportSourceFile()` (`reports/.../dataexporters/DataExporter.java:512`) writes
the `src/` tree in two gated blocks:

- `isSaveSourceFiles()` → `saveAspectJsonFiles(...)` per aspect (main/test/generated/
  buildAndDeployment/other), `DataExporter.java:806`. For each file in `referencedFiles`
  (≤ 9 top-N categories × `maxTopListSize` default 50, deduped → up to ~450 **full** files,
  potentially 10s–100s of MB) it writes **the raw file AND a `.html` wrapper**
  (`saveFileAsHtml`, `DataExporter.java:738`, template `CodeFragmentFile.html`, Ace editor).
- `isSaveCodeFragments()` → `saveUnitFragmentFiles` (`DataExporter.java:691`, writes only a
  `.html` wrapper per unit via `saveUnitAsHtml:703`, template `CodeFragmentUnit.html`,
  highlight.js) and `saveDuplicateFragmentFiles` (`DataExporter.java:758`, writes one raw
  text file per duplicate, multiple blocks concatenated, no wrapper).

Fragment loops are small (≤ ~50–150 items, snippet-sized). The aspect cache is the large one.

**Five link sites** point into `src/`:
1. `UtilsReportUtils.java:27` — unit's file link → `../src/main/<path>.html`
2. `UtilsReportUtils.java:33` — unit fragment link → `../src/fragments/<type>/<type>_<n>.<ext>.html`
3. `FilesReportUtils.java:51-52` — file-table link → `../src/main/<path>.html`
4. `DuplicationReportGenerator.java:77` — duplicate link → `../src/fragments/<type>/<type>_<n>.<ext>` (raw)
5. `FilesExplorerGenerators.java:70` — `FileExport.sourceFileLink` → `../src/<aspect>/<path>.html`
6. `UnitsExplorerGenerators.java:96` — unit explorer link → `../src/fragments/<type>/<type>_<n>.<ext>.html`

(`ZipUtils` already exists for server-side zip writing — `stringToZipFile(File, String[][])`.)

## Target `src/` layout

| Content | Today | After |
|---|---|---|
| Aspect full-file cache | `src/<aspect>/<path>` + `<path>.html` (≤900 files, big) | **`src/<aspect>.zip`** (one per aspect, entries keyed by relative path) |
| Unit fragments | `src/fragments/<type>/<type>_<n>.<ext>.html` | **`src/fragments/<type>.json`** (array) |
| Duplicate fragments | `src/fragments/<type>/<type>_<n>.<ext>` | **`src/fragments/<type>.json`** (array, multi-block) |
| Per-file `.html` wrappers | one per file/unit | **deleted** |
| Viewer | (two templates: Ace + highlight.js) | **one `src/viewer.html`** (highlight.js + fflate) |

Net: ~900 `src/` files → ~5 zips + ~5 small JSON bundles + 1 `viewer.html`.

### JSON bundle shapes (`src/fragments/*.json`)
- Units (`longest_unit.json`, `most_complex_units.json`):
  `[{ "name", "file", "from", "to", "loc", "mccabe", "ext", "code" }]`
- Duplicates (`longest_duplicates.json`, `most_frequent_duplicates.json`, `unit_duplicates.json`):
  `[{ "ext", "blocks": [{ "file", "from", "to", "code" }] }]`

Index into the array is the existing 1-based `count`/`index` used by the link sites.

### Aspect zip
`src/main.zip` etc., entry name = `sourceFile.getRelativePath()`, content = full file text.
Built with the existing `ZipUtils.stringToZipFile(File, String[][])`.

## The shared viewer (`src/viewer.html`)

One client-rendered template (new resource under `reports/src/main/resources/templates/`,
written verbatim to `src/viewer.html`). Loads **highlight.js** (the lighter, already-used
highlighter) and **fflate** (tiny ~10KB unzip) from CDN — matching the existing pattern of
templates loading JS from CDNs (`cloudfront`, `zeljkoobrenovic.com`).

URL params:
- `?aspect=main&file=<relPath>&from=&to=` → `fetch('main.zip')`, fflate-extract that one
  entry, highlight, scroll to `from:to` if present.
- `?bundle=fragments/longest_unit.json&i=<n>` → fetch bundle, render item `n` (name, file,
  line range, loc/mccabe, highlighted code). For duplicate bundles, render each block in
  `blocks[]` in sequence (multi-block mode), each labelled `file [from:to]`.

Caches fetched zips/bundles in a JS object so repeated views in one session don't re-fetch.

## Implementation steps (each independently shippable & testable)

### Step 1 — Shared viewer + drop unit/file `.html` wrappers (pure win, lowest risk)
1. Add `reports/src/main/resources/templates/viewer.html`.
2. In `DataExporter.exportSourceFile()` write `viewer.html` once into the `src/` cache folder
   (alongside the existing `saveStructureFile()` call).
3. `saveAspectJsonFiles` (`:806`): stop calling `saveFileAsHtml`; build the per-aspect zip
   instead of writing individual raw files. (Decision A below: keep raw files too, or zip-only.)
4. `saveUnitFragmentFiles`/`saveUnitAsHtml`: stop writing `.html` wrappers; collect units into
   a bundle list (written in Step 2). For Step 1 alone, can keep raw snippet write if we want
   to ship viewer-for-files first; cleaner to do Steps 1–2 together for units.
5. Repoint link sites 1, 3, 5 (file links) → `../src/viewer.html?aspect=<a>&file=<path>`.
6. Delete `CodeFragmentFile.html` (and `CodeFragmentUnit.html` after Step 2). Drop the now-unused
   `saveFileAsHtml`/`saveUnitAsHtml`/`HtmlTemplateUtils` usages.

### Step 2 — Bundle unit fragments → JSON
1. New small exporter (e.g. `FragmentBundleExporter`) producing the units JSON array from
   `UnitInfo` (`name`=shortName, `file`=relativePath, `from`/`to`=start/end line, `loc`, `mccabe`,
   `ext`, `code`=body). Write `src/fragments/<type>.json`.
2. Replace `saveUnitFragmentFiles` bodies (`:691`, called at `:531-532`) with the bundle writer.
3. Repoint link sites 2, 6 (unit fragment links) → `../src/viewer.html?bundle=fragments/<type>.json&i=<index>`.

### Step 3 — Bundle duplicate fragments → JSON (+ multi-block viewer)
1. Extend the bundle exporter for duplicates from `DuplicationInstance.getDuplicatedFileBlocks()`
   (each block → `{file, from, to, code}`; reuse the existing line-range slicing in
   `saveDuplicateFragmentFiles:773-782`). Write `src/fragments/<type>.json`.
2. Replace `saveDuplicateFragmentFiles` (`:758`, called `:535-537`).
3. Add multi-block rendering to `viewer.html`.
4. Repoint link site 4 (`DuplicationReportGenerator.java:77`) →
   `../src/viewer.html?bundle=fragments/<type>.json&i=<count>`.

### Step 4 — Verify & document
1. Run a `generateReports` on a sample repo; confirm `src/` file count collapses and every
   "view" link resolves (units table, file size/age tables, duplication tables, files & units
   explorers).
2. Update `docs/configuration.md` if any config key semantics change (likely none —
   `saveSourceFiles`/`saveCodeFragments` toggles keep their meaning).
3. Update `CLAUDE.md` "Two HTML rendering mechanisms" section to describe the viewer + bundles.

## Open decisions to confirm before coding

**A. Aspect cache: zip-only, or zip + keep raw files?**
The viewer reads the zip, so raw files are redundant *for viewing*. Memory note says only
`data/` is the tooling contract, so `src/` raw files can go → **zip-only** (max count
reduction). Confirm nothing external reads `src/<aspect>/<path>` directly.

**B. Ship incrementally or as one PR?**
Steps are independent; recommend three commits (1, 2, 3) on the current
`add-tests-and-improvements` branch, or a dedicated branch.

**C. fflate delivery.** CDN `<script>` (matches existing template pattern, simplest) vs.
vendoring a copy into resources (works offline, no CDN dependency). Existing templates use
CDN, so default to CDN unless offline robustness is wanted.

## Risks / trade-offs (on the record)

- A "view" click now fetches a zip/bundle and indexes in, vs. opening one tiny file. Fine for
  HTTP-served reports; the in-session cache avoids refetching. This is the only behavioral change.
- Whole-file viewing via zip requires the report be HTTP-served (confirmed always true).
- fflate is a new client dependency (CDN); aligns with existing CDN-loaded JS in templates.
- `ZipUtils.stringToZipFile` loads entry content as `String` — fine; aspect files are text.

## Test touchpoints

- Existing `reports` module tests (run `mvn -pl reports -am test`). Add a focused test for the
  new `FragmentBundleExporter` JSON shape and for aspect-zip entry naming.
- Manual: generate reports for a medium repo, grep the `src/` tree file count before/after,
  click through each of the 6 link types.
