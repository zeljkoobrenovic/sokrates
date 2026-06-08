# Implementation Plan: Replace Graphviz with Mermaid.js

## Goal

Eliminate the dependency on the external `dot` process for **report generation**. Graphviz
diagrams (component/file/contributor/landscape dependency graphs) are rendered server-side
today (`dot` → inline `.svg` + `.dot.txt` files in `html/visuals/`). Replace them with
**Mermaid.js**: embed the diagram as text and let the browser render it. No external process,
no per-graph SVG files written at generation time.

## Decisions (confirmed)

- **Swing GUI** (`codeexplorer/DependenciesPane`): **drop/stub** its inline graph view so
  `GraphvizUtil`/`GraphvizSettings` can be deleted entirely.
- **Output form**: embed Mermaid text rendered in-browser; offer a **`.mmd` download** instead
  of `.svg`/`.dot.txt`. Fully client-side.
- **Layout fidelity**: Mermaid for **all** graph types; looser/non-deterministic layout is an
  accepted trade for removing the external process. No phasing, no hybrid.

## Current reality (verified)

- **No Maven dependency** — Graphviz is only the external `dot` binary (`GraphvizSettings`
  locates it; `GraphvizUtil.runDot` shells out via `ProcessBuilder`).
- **One DOT producer**: `GraphvizDependencyRenderer.getGraphvizContent(allComponents,
  componentDependencies, groups)` (`reports/.../utils/`). Configured via setters: `type`
  (digraph/graph), `arrow` (`->`/`--`), `orientation` (rankdir TB/LR/…), `arrowColor`,
  `cyclicArrowColor`, `defaultNodeFillColor`, `maxNumberOfDependencies`, `reverseDirection`.
  Features used: box nodes, node fill, edge labels, edge thickness (penwidth ∝ count), edge
  color + alpha, cyclic-edge recolor, `subgraph cluster_N` for `ComponentGroup`s. NOT used:
  tooltips, URLs, custom shapes, HTML labels, ranksep/nodesep.
- **Stable caller API**: `RichTextReport.addGraphvizFigure(id, description, code)` /
  `addHiddenGraphvizFigure(...)` wrap the string in a `RichTextFragment` of
  `Type.GRAPHVIZ`. ~10 call sites across LogicalComponents/Duplication/Contributors/
  FileTemporalDependencies report generators + landscape tabs.
- **One embed/render seam**: `ReportRenderer.renderFragment()` branches on `Type.GRAPHVIZ`;
  `renderAndSaveVisuals()` writes `<id>.dot.txt` + `<id>.svg` and inlines the SVG. This is the
  ONLY place SVG is produced/embedded.
- **Shared `<head>`**: `ReportConstants.REPORTS_HTML_HEADER` — where the mermaid.js loader is
  added once so every report page can render `.mermaid` blocks.
- **A `mermaid.html` template already exists** (mermaid@10 from jsDelivr CDN) — proves the
  client-side mechanism; CDN verified live.
- **Force-graph / d3 visuals are NOT Graphviz** (separate `ForceGraphExporter` / d3 templates)
  — out of scope, untouched.

## Architecture: convert at the producer, embed at the one seam

Keep the caller API (`addGraphvizFigure`) and the `RichTextFragment` flow. Change two things:

1. **Producer** — `GraphvizDependencyRenderer` emits **Mermaid flowchart** text instead of DOT
   (same inputs, same setters). The fragment now carries Mermaid, not DOT.
2. **Embed seam** — `ReportRenderer.renderFragment` for `Type.GRAPHVIZ` emits a
   `<pre class="mermaid">…</pre>` block (+ `.mmd` download link) instead of shelling out to
   `dot`. mermaid.js (loaded once via `REPORTS_HTML_HEADER`) renders it on page load.

Rejected alternative: keep emitting DOT and write a DOT→Mermaid translator at embed time —
fragile string parsing, no benefit.

### DOT → Mermaid feature mapping

| DOT feature | Mermaid equivalent |
|---|---|
| `digraph` + `->` | `flowchart <dir>` with `A --> B` |
| `graph` + `--` | `flowchart` with `A --- B` (undirected link) |
| `rankdir=LR/TB/RL/BT` | `flowchart LR/TB/RL/BT` |
| node `[fillcolor=…]` box | node `id["label"]` + `style id fill:…` (or a `classDef` per fill) |
| edge `[label=" n "]` | `A -- "n" --> B` (labeled link) |
| `penwidth` (thickness ∝ count) | `linkStyle <edgeIndex> stroke-width:<n>px,stroke:<color>` |
| cyclic edge recolor | same `linkStyle`, with the cyclic color |
| edge color + alpha | `linkStyle … stroke:<rrggbbaa>` (Mermaid accepts CSS colors) |
| `subgraph cluster_N { label }` | `subgraph name["label"] … end` |
| node id escaping (`encodeLabel`) | Mermaid id sanitization: ids must be safe tokens; put the
  human text in the `["…"]` label, use a generated safe id (e.g. `n0`, `n1`) mapped from the
  component name. Labels with quotes/special chars wrapped/escaped per Mermaid rules. |

Note: Mermaid ids can't contain arbitrary characters the way quoted DOT labels can, so the
producer must assign **stable synthetic ids** (`n<index>`) and keep the real name only in the
node label. `linkStyle` targets edges by **definition order index**, so edges must be emitted
in a known order and their indices tracked while building.

## Implementation steps

### Step 1 — Mermaid producer
- Add `getMermaidContent(allComponents, componentDependencies, groups)` to
  `GraphvizDependencyRenderer` (or a sibling `MermaidDependencyRenderer` sharing the setters).
  Emits a `flowchart` per the mapping above: assign synthetic node ids, emit clusters as
  `subgraph`, emit edges in order while accumulating a `linkStyle` line per edge for
  thickness/color (including cyclic recolor and the existing alpha math). Honor
  `maxNumberOfDependencies`, `reverseDirection`, `type` (directed vs undirected link),
  `orientation`.
- Keep `encodeLabel` semantics for label text; add id-sanitization for node ids.

### Step 2 — Embed seam + mermaid loader
- `RichTextFragment.Type`: keep `GRAPHVIZ` (now carrying Mermaid) or rename to `MERMAID`
  (rename is cleaner but touches the enum + `addGraphvizFigure`; keeping the name is lower-risk).
  Decision: **add `MERMAID`, keep `GRAPHVIZ` deprecated-unused**, OR reuse `GRAPHVIZ` —
  pick at implementation (favor reuse to minimize churn).
- `ReportRenderer.renderFragment`: for the diagram fragment, append
  `<pre class="mermaid">\n<mermaid text>\n</pre>` when `isShow()`, plus a small "copy/download
  .mmd" affordance. Stop calling `GraphvizUtil`. `renderAndSaveVisuals` no longer writes
  `.svg`/`.dot.txt`; optionally write `<id>.mmd` to `visuals/` for the download link.
- `ReportConstants.REPORTS_HTML_HEADER`: add the mermaid.js module loader + `initialize`
  (mirror `mermaid.html`: `securityLevel:'loose'`, `startOnLoad:true`). Loaded once per page.

### Step 3 — Update callers / helper naming
- The ~10 `getGraphvizContent(...)` + `addGraphvizFigure(...)` call sites switch to the Mermaid
  producer. Either repoint them to `getMermaidContent` or have `addGraphvizFigure` accept the
  Mermaid string transparently. Keep the per-graph `id` and `description` exactly as today.
- `VisualizationTools.addDownloadLinks` / "open in Graphviz editor" link: replace with
  `.mmd` download (and optionally an "open in Mermaid live editor" link).

### Step 4 — Remove Graphviz
- Delete `GraphvizUtil`, `GraphvizSettings`. Remove the `-internalGraphviz` CLI option (it was
  never implemented). Remove `GRAPHVIZ_DOT` handling. Update Dockerfile (drop graphviz install
  + `GRAPHVIZ_DOT` env) and any docs/CLAUDE.md mention of Graphviz being required.
- **Swing GUI**: stub `DependenciesPane`'s 3 `GraphvizUtil.getSvgFromDot` calls — replace the
  inline SVG panel with a placeholder/message (graphs now live in the HTML reports). Verify
  codeexplorer still compiles and launches.

### Step 5 — Verify
- Build all modules; run report generation **without** `dot` on PATH and **without**
  `GRAPHVIZ_DOT` set — confirm it succeeds (the whole point).
- Open the reports; confirm each diagram type renders via Mermaid: component dependencies
  (incl. clusters + cyclic coloring), file temporal dependencies, duplication-between-
  components, contributor-shared-files, and landscape graphs.
- Validate the mermaid header JS + a sample generated `.mermaid` block parse (extract, run a
  Mermaid parse check under node, or at least confirm well-formed flowchart text).
- Update CLAUDE.md (the "External runtime dependency: Graphviz" note and the visuals section).

## Risks / trade-offs (on the record)

- **Layout**: Mermaid/dagre layouts are looser and computed in-browser (non-deterministic, no
  server SVG). Dense graphs (capped at 50–200 edges today) may look worse; accepted.
- **`linkStyle` by index** is brittle if edge emission order isn't carefully tracked — the
  producer must own ordering. Unit-test the Mermaid output for a known graph.
- **Browser render cost**: many/large diagrams now render client-side on page load; very large
  component graphs could be slow in the browser. The existing edge caps mitigate this.
- **No offline SVG**: users who relied on the downloadable `.svg` lose it (get `.mmd` instead).
- **Mermaid id constraints**: must map names→safe ids; a bug here mislabels nodes — cover with
  a test.
- **Reports must be served/opened in a browser with CDN access** for mermaid.js (consistent
  with the viewer.html/highlight.js CDN approach already adopted).

## Test touchpoints

- New unit test for the Mermaid producer: given components + dependencies (+ a cyclic pair +
  a group), assert the emitted flowchart has the right nodes, subgraph, directed/undirected
  links, labels, and `linkStyle` lines (thickness/cyclic color). This is the safety net for the
  index-based `linkStyle` and id-mapping risks.
- Build + headless report-generation smoke test with no `dot` available.
