# Plan: compressed-inline-JSON for the remaining ${data}-template visuals

## Goal

The `${data}`-template visuals that still embed their data inline (one self-contained HTML
file each) ship the JSON **raw**. Keep them self-contained (no separate zip, works from
`file://`) but embed the JSON **deflate+base64-compressed** and inflate it in-browser with
fflate before rendering. JSON deflates ~5–10×; base64 adds ~33% back → still a large net
size win, especially for the bigger views.

In scope (the remaining inline-`${data}` templates):
`zoomable_circles_colored.html` (→ `zoomable_circles_all_files.html`), `bubble_chart.html`,
`treemap.html`, `force_2d.html`, `force_3d.html`, `bar_chart_races.html`.

Out of scope: zoomable_circles/sunburst families (already template+zip), and the x3dom 3D
views (`units_3d_*`, `files_3d`) — those embed X3D markup, not JSON (separate, harder case).

## Decisions (confirmed)

- Technique: **deflate + base64 inline, fflate-inflate in browser**. Stays one self-contained
  file (no HTTP fetch needed).
- Scope: **all `${data}`-template inline visuals** (the six above).

## Mechanism (central, minimal per-template change)

Today `VisualizationTemplate` substitutes `${data}` with raw JSON at a single site per
template (`"children": ${data}` / `const gData = ${data}` / `const data = ${data};`).

Change it so:
1. The Java render methods compute `base64(deflate(json))` and substitute `${data}` with the
   JS expression **`sokratesInflate("<base64>")`** (so the existing `${data}` site becomes a
   decode call — the template's render code is otherwise unchanged).
2. A shared head block (fflate CDN `@0.8.2` + the `sokratesInflate` helper) is injected via a
   new placeholder `${sokrates-inflate-lib}` added once to each template's `<head>`.
   `sokratesInflate(b64)` = base64-decode → `fflate.unzlibSync` (zlib/deflate) →
   `fflate.strFromU8` → `JSON.parse`.

This concentrates the logic in `VisualizationTemplate` + one small head snippet per template;
the d3/render bodies don't change.

### Java side
- Add `VisualizationTemplate.deflateBase64(String json)` using `java.util.zip.Deflater`
  (zlib) + `java.util.Base64`.
- Add an injected library constant (the `<script src=fflate>` + `function sokratesInflate(...)`).
- In `render(...)`, `render2DForceGraph`, `render3DForceGraph`, `renderRacingCharts`,
  `renderZoomableCirclesColored`: replace `${data}` with `sokratesInflate("...")` and
  `${sokrates-inflate-lib}` with the library block. Keep the `,"children":[]` strip BEFORE
  compressing (it operates on the JSON string).

### Template side
- Add `${sokrates-inflate-lib}` in `<head>` of the six templates. Leave the `${data}` site as
  is (it now receives `sokratesInflate("...")`).

## Steps

1. `VisualizationTemplate`: add `deflateBase64`, the inflate-lib constant, and a helper that
   wraps a JSON string as `sokratesInflate("<b64>")`. Update the render methods to use them.
2. Add `${sokrates-inflate-lib}` to the six templates' heads.
3. Verify: generate a report; confirm the six files no longer contain a raw JSON array/object
   at the `${data}` site (instead `sokratesInflate("...")`), are smaller, and render correctly
   in a browser (headless: circles/bubble/treemap draw; force graph draws; racing chart loads).
   Confirm fflate present and `file://` open still works (no fetch).

## Risks / trade-offs

- **Base64 + a JS inflate step** on load (negligible for these sizes; fflate is fast).
- **fflate must load** (CDN) for the viz to render — same CDN dependency already accepted for
  the source viewer and circles/sunburst. Unlike those, this still works from `file://` (no
  fetch; the data is inline).
- **Compression ratio vs base64 overhead**: tiny payloads (a few components) may not shrink
  much net; the win scales with data size (all_files, large force graphs, racing). Acceptable —
  correctness identical, and large views are where it matters.
- Keep the existing `,"children":[]` strip applied to the JSON *before* compressing so decoded
  data matches today exactly.

## Test touchpoints

- Unit test: `deflateBase64` round-trips (Java deflate → java Inflater → equals original), and
  `sokratesInflate` wrapper produces the expected `sokratesInflate("...")` text.
- Headless render of one of each shape (circles-colored, bubble/treemap, force_2d, racing) from
  a generated report; assert SVG/canvas content drawn and no JS errors, opened via file://.
