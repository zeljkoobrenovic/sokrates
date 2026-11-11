# Plan: Collapse zoomable circles/sunburst visuals to one template + one zip per family

## Goal

`html/visuals/` is now the biggest per-repo file-count contributor (~73 files) after the
`src/` work. Each file is a `${data}`-template shell + embedded JSON. Convert the two
highest-count families to **one shared template HTML + one zip of per-view JSON**, with the
template selecting its view via `?key=` and extracting it with fflate (the same client-side
unzip already used by `src/viewer.html`).

- **zoomable_circles**: 26 files → `zoomable_circles.html` + `zoomable_circles.zip`
- **zoomable_sunburst**: 15 files → `zoomable_sunburst.html` + `zoomable_sunburst.zip`

Net: **41 → 4 files**, plus the JSON is compressed. Reuses the established fflate pattern.
Visuals URLs are only linked from within the reports (confirmed), so free to restructure.

## Decisions (confirmed)

- One template per family + ONE family zip holding all that family's per-view JSON; template
  reads `?key=` and fflate-extracts that entry. Same mechanism as `src/viewer.html`.
- Scope now: **circles + sunburst only**. force-graph / treemap / bubble / racing / x3dom 3D
  are a later, separate step.

## Current reality (verified)

- All circles/sunburst files come from `VisualizationTemplate.renderZoomableCircles(items)` /
  `renderZoomableSunburst(items)` (`common/.../renderingutils/`), which substitutes `${data}`
  (a `List<VisualizationItem>` as JSON) into `vis_templates/zoomable_circles.html` /
  `zoomable_sunburst.html`. Data is embedded at generation time.
- Generation sites (all in `CommandLineInterface`): `generateFileStructureExplorers` (per
  scope main/test/generated/build/other), `addCommitZoomableCircles` (×5 time windows),
  `addContributorsZoomableCircles` (×5), `addRiskColoredZoomableCircles` (×5 metrics ×
  {coloring, coloring_categories} — circles only, no sunburst).
- The plain-circles `${data}` is at `zoomable_circles.html:52` (`"children": ${data}`); the
  render strips `,"children":[]` afterward. Sunburst is analogous.
- **Out of this family:** `zoomable_circles_all_files.html` uses a DIFFERENT template
  (`zoomable_circles_colored.html`, distinct fill logic) — it's a singleton; leave as its own
  file for now (not part of the circles zip).
- **Link sites**: ~40 `addNewTabLink("...","visuals/zoomable_circles_*.html"|"..._sunburst_*.html")`
  in `ReportFileExporter.addVisuals()`, plus the per-scope ones in `addScopeVisuals` (`:935/942`),
  plus the per-repository `Structure.html` iframe tab strip (CLI writes the backing files; the
  Structure template references them). The risk-colored circles are linked from FileSize/FileAge/
  FileChangeFrequency/Contributors sections.

## Target layout

```
visuals/
  zoomable_circles.html         # one shared template (fetches the zip, renders by ?key=)
  zoomable_circles.zip          # entries: main.json, test.json, ..., commits_30_main.json,
                                #          contributors_30_main.json, main_loc_coloring.json, ...
  zoomable_sunburst.html
  zoomable_sunburst.zip
  zoomable_circles_all_files.html   # unchanged (colored variant, singleton)
  ... (other families unchanged for now)
```

Each old filename's distinguishing suffix becomes the **zip entry key**:
`zoomable_circles_main.html` → `zoomable_circles.html?key=main` (entry `main.json`);
`zoomable_circles_commits_30_main.html` → `?key=commits_30_main`; etc.

## Implementation steps

### Step 1 — Generation: write zips instead of per-view HTML
- In `CommandLineInterface`, replace the per-view `FileUtils.write(... .html, renderZoomableCircles(items))`
  calls with: accumulate `key -> JSON(items)` into two maps (circles, sunburst) across all the
  generate* methods, then write `zoomable_circles.zip` / `zoomable_sunburst.zip` once
  (via `ZipUtils.stringToZipFile(File, String[][])`, already used for aspect zips) and write the
  two template HTML files once (from the new templates below).
- Keep producing the same `items` per key (no analysis change). The key is the old filename
  suffix (`main`, `test`, `commits_30_main`, `contributors_main`, `main_loc_coloring`, …).
- Add a `VisualizationTemplate` method to serialize items to the same JSON used today
  (reuse `JsonGenerator` + the `,"children":[]` strip) so zip entries match what the template
  expects.

### Step 2 — Templates: fetch + unzip + render by key
- New `vis_templates/zoomable_circles.html` (and sunburst): keep the CSS + d3 + render JS, but
  replace `"children": ${data}` with JS that, on load, reads `?key=`, `fetch()`es
  `zoomable_circles.zip`, fflate-unzips the `<key>.json` entry, and runs the existing render
  with that data. Load fflate from the same CDN as `viewer.html`. (The template is now static —
  written verbatim once, no `${data}` substitution at generation time.)
- Mirror the existing render logic exactly (pack layout, zoom, colors) so behaviour is identical.

### Step 3 — Links: point at `?key=`
- Update the ~40 link sites in `ReportFileExporter` (and `addScopeVisuals`) from
  `visuals/zoomable_circles_<suffix>.html` to `visuals/zoomable_circles.html?key=<suffix>`
  (and sunburst). Update `Structure.html` iframe srcs likewise.

### Step 4 — Verify
- Generate a report; confirm `visuals/` has `zoomable_circles.html`+`.zip` and
  `zoomable_sunburst.html`+`.zip` and NO `zoomable_circles_<suffix>.html` (except all_files);
  zip entry keys match every link's `?key=`.
- Headless-render a couple of views (puppeteer) to confirm the template fetches+unzips+draws,
  and that hidden-container/tab timing still works (Structure.html embeds these as iframes).

## Risks / trade-offs (on the record)

- **Whole-family data per load**: opening one circles view fetches the whole `zoomable_circles.zip`
  and extracts one entry. Circles/sunburst data is small structural JSON, so fine; the zip is
  fetched once and can be cached per session.
- **Must be served over HTTP** (fetch of zip) — already required by the source viewer.
- **all_files (colored)** stays a separate file (different template); documented so it isn't
  mistaken for a missed conversion.
- **Structure.html iframes**: these embed circles/sunburst by URL; the `?key=` change must be
  reflected there too or the per-repo Structure tab breaks. Explicit verify step.
- **~40 link sites** is the main edit surface; mechanical but must stay key-aligned with the
  zip entries (same alignment discipline as the fragment bundles).

## Test touchpoints

- A small unit test for the new generation helper (key→JSON map → zip entries present & named).
- Headless render of `zoomable_circles.html?key=main` against a generated zip (puppeteer/mmdc-style).
- Regenerate a repo; assert file-count delta and that every `?key=` has a matching zip entry.
