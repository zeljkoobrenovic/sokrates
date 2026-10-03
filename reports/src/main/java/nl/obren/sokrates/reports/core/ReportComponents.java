/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

/**
 * Shared client-side behaviour of the server-rendered reports, included in
 * {@link ReportConstants#REPORTS_HTML_HEADER} and applied as progressive enhancement (the HTML works
 * without it):
 * <ul>
 * <li><b>Data tables</b> — a <code>table.sk-data-table</code> (see {@link RichTextReport#startDataTable})
 * gets click-to-sort column headers (numbers sorted as numbers, so "1,234", "12%" and "+5" work), a
 * row filter (from 8 rows), a row count, a CSV download of the visible rows, and its own horizontal
 * scroll. Sorting is offered only when every row has as many cells as the header and none spans
 * columns, so summary rows are never reordered.</li>
 * <li><b>Diagrams</b> — a rendered Mermaid diagram gets zoom in/out, fit and fullscreen buttons, drag to
 * pan and ⌘/Ctrl + wheel zoom (svg-pan-zoom); its height is capped so a huge graph no longer pushes the
 * page down.</li>
 * <li><b>Narrow screens</b> — on shell pages every other top-level table scrolls horizontally inside its
 * own box and iframes are capped at the content width, so the page itself never scrolls sideways.</li>
 * </ul>
 * Table cells are read and written with <code>textContent</code>; nothing is re-rendered as HTML.
 */
public class ReportComponents {
    private ReportComponents() {
    }

    public static final String CSS = "" +
            ".sk-table-tools {display: flex; flex-wrap: wrap; align-items: center; gap: 10px; margin: 6px 0;}\n" +
            ".sk-table-filter {flex: 0 1 260px; padding: 5px 9px; font: inherit; font-size: 13px; color: var(--sk-text); " +
            "background: var(--sk-surface); border: 1px solid var(--sk-border-strong); border-radius: 6px;}\n" +
            ".sk-table-filter:focus {outline: 2px solid var(--sk-accent); outline-offset: 0;}\n" +
            ".sk-table-count {font-size: 12px; color: var(--sk-text-muted);}\n" +
            ".sk-table-button {padding: 4px 10px; font: inherit; font-size: 12px; cursor: pointer; color: var(--sk-text-muted); " +
            "background: var(--sk-surface); border: 1px solid var(--sk-border); border-radius: 6px;}\n" +
            ".sk-table-button:hover {color: var(--sk-text); border-color: var(--sk-border-strong);}\n" +
            ".sk-table-scroll, .sk-scroll-x {max-width: 100%; overflow-x: auto;}\n" +
            "th.sk-sortable {cursor: pointer; user-select: none; white-space: nowrap;}\n" +
            "th.sk-sortable:hover {color: var(--sk-accent);}\n" +
            "th.sk-sortable::after {content: '\\2195'; margin-left: 4px; font-size: 80%; opacity: 0.35;}\n" +
            "th.sk-sortable[aria-sort='ascending']::after {content: '\\25B2'; opacity: 0.9;}\n" +
            "th.sk-sortable[aria-sort='descending']::after {content: '\\25BC'; opacity: 0.9;}\n" +
            ".sk-data-table tr.sk-filtered-out {display: none;}\n" +
            ".sk-diagram {position: relative;}\n" +
            ".sk-diagram svg {cursor: grab;}\n" +
            ".sk-diagram svg:active {cursor: grabbing;}\n" +
            ".sk-diagram:fullscreen {background: var(--sk-canvas); padding: 12px; overflow: hidden;}\n" +
            ".sk-diagram-tools {position: absolute; top: 6px; right: 6px; z-index: 5; display: flex; gap: 4px; opacity: 0.55; transition: opacity 0.15s ease;}\n" +
            ".sk-diagram:hover .sk-diagram-tools, .sk-diagram-tools:focus-within {opacity: 1;}\n" +
            ".sk-diagram-tools button {width: 28px; height: 28px; padding: 0; font: inherit; font-size: 15px; line-height: 1; cursor: pointer; " +
            "color: #1f2937; background: #ffffff; border: 1px solid #c8cdd5; border-radius: 6px; box-shadow: 0 1px 2px rgba(16, 24, 40, 0.08);}\n" +
            ".sk-diagram-tools button:hover {background: #f1f3f6;}\n" +
            ".sk-main iframe {max-width: 100%;}\n" +
            "@media print {.sk-table-tools, .sk-diagram-tools {display: none;}}\n";

    public static final String SCRIPT = "" +
            "(function () {\n" +
            "  function text(cell) { return (cell.textContent || '').replace(/\\s+/g, ' ').trim(); }\n" +
            "  // A number when the cell reads as one (thousands separators, %, + and the minus sign allowed), else text.\n" +
            "  function sortValue(cell) {\n" +
            "    var t = text(cell);\n" +
            "    var n = t.replace(/[,%+\\s]/g, '').replace(/^\\u2212/, '-');\n" +
            "    return /^-?\\d+(\\.\\d+)?$/.test(n) ? {num: parseFloat(n)} : {str: t.toLowerCase()};\n" +
            "  }\n" +
            "  function compare(a, b) {\n" +
            "    if (a.num !== undefined && b.num !== undefined) { return a.num - b.num; }\n" +
            "    if (a.num !== undefined) { return -1; }\n" +
            "    if (b.num !== undefined) { return 1; }\n" +
            "    return a.str.localeCompare(b.str, undefined, {numeric: true});\n" +
            "  }\n" +
            "  function csvField(value) { return /[\",\\n]/.test(value) ? '\"' + value.replace(/\"/g, '\"\"') + '\"' : value; }\n" +
            "  function tableTitle(table) {\n" +
            "    var section = table.closest('.section, .subSection');\n" +
            "    var title = section ? section.querySelector('.sectionTitle, .subSectionTitle') : null;\n" +
            "    return (title ? text(title) : document.title || 'table').replace(/[^A-Za-z0-9]+/g, '-').replace(/^-|-$/g, '').toLowerCase() || 'table';\n" +
            "  }\n" +
            "  function enhanceTable(table) {\n" +
            "    if (table.getAttribute('data-sk-enhanced')) { return; }\n" +
            "    table.setAttribute('data-sk-enhanced', 'true');\n" +
            "    var rows = Array.prototype.slice.call(table.rows);\n" +
            "    if (rows.length < 2) { return; }\n" +
            "    var header = rows[0];\n" +
            "    var headerCells = Array.prototype.slice.call(header.cells);\n" +
            "    var bodyRows = rows.slice(1);\n" +
            "    var parent = bodyRows[0].parentNode;\n" +
            "    var sortable = headerCells.length > 0 && headerCells.every(function (c) { return c.tagName === 'TH' && c.colSpan === 1; })\n" +
            "      && bodyRows.every(function (r) {\n" +
            "        return r.parentNode === parent && r.cells.length === headerCells.length\n" +
            "          && Array.prototype.every.call(r.cells, function (c) { return c.colSpan === 1 && c.rowSpan === 1; });\n" +
            "      });\n" +
            "    var scroll = document.createElement('div');\n" +
            "    scroll.className = 'sk-table-scroll';\n" +
            "    table.parentNode.insertBefore(scroll, table);\n" +
            "    scroll.appendChild(table);\n" +
            "    var tools = document.createElement('div');\n" +
            "    tools.className = 'sk-table-tools';\n" +
            "    scroll.parentNode.insertBefore(tools, scroll);\n" +
            "    var count = document.createElement('span');\n" +
            "    count.className = 'sk-table-count';\n" +
            "    function updateCount() {\n" +
            "      var shown = bodyRows.filter(function (r) { return !r.classList.contains('sk-filtered-out'); }).length;\n" +
            "      count.textContent = shown === bodyRows.length ? bodyRows.length + (bodyRows.length === 1 ? ' row' : ' rows')\n" +
            "        : shown + ' of ' + bodyRows.length + ' rows';\n" +
            "    }\n" +
            "    if (bodyRows.length >= 8) {\n" +
            "      var filter = document.createElement('input');\n" +
            "      filter.type = 'search';\n" +
            "      filter.className = 'sk-table-filter';\n" +
            "      filter.placeholder = 'Filter rows…';\n" +
            "      filter.setAttribute('aria-label', 'Filter rows');\n" +
            "      filter.addEventListener('input', function () {\n" +
            "        var terms = filter.value.toLowerCase().split(/\\s+/).filter(Boolean);\n" +
            "        bodyRows.forEach(function (r) {\n" +
            "          var hay = text(r).toLowerCase();\n" +
            "          r.classList.toggle('sk-filtered-out', !terms.every(function (t) { return hay.indexOf(t) >= 0; }));\n" +
            "        });\n" +
            "        updateCount();\n" +
            "      });\n" +
            "      tools.appendChild(filter);\n" +
            "    }\n" +
            "    tools.appendChild(count);\n" +
            "    var csv = document.createElement('button');\n" +
            "    csv.type = 'button';\n" +
            "    csv.className = 'sk-table-button';\n" +
            "    csv.textContent = 'Download CSV';\n" +
            "    csv.addEventListener('click', function () {\n" +
            "      var lines = [headerCells.map(function (c) { return csvField(text(c)); }).join(',')];\n" +
            "      bodyRows.forEach(function (r) {\n" +
            "        if (r.classList.contains('sk-filtered-out')) { return; }\n" +
            "        lines.push(Array.prototype.map.call(r.cells, function (c) { return csvField(text(c)); }).join(','));\n" +
            "      });\n" +
            "      var url = URL.createObjectURL(new Blob([lines.join('\\n') + '\\n'], {type: 'text/csv'}));\n" +
            "      var a = document.createElement('a');\n" +
            "      a.href = url; a.download = tableTitle(table) + '.csv';\n" +
            "      document.body.appendChild(a); a.click(); document.body.removeChild(a);\n" +
            "      setTimeout(function () { URL.revokeObjectURL(url); }, 0);\n" +
            "    });\n" +
            "    tools.appendChild(csv);\n" +
            "    updateCount();\n" +
            "    if (!sortable) { return; }\n" +
            "    headerCells.forEach(function (th, column) {\n" +
            "      if (!text(th)) { return; }\n" +
            "      th.classList.add('sk-sortable');\n" +
            "      th.setAttribute('tabindex', '0');\n" +
            "      th.setAttribute('aria-sort', 'none');\n" +
            "      function sort() {\n" +
            "        var values = bodyRows.map(function (r) { return {row: r, value: sortValue(r.cells[column])}; });\n" +
            "        var numeric = values.some(function (v) { return v.value.num !== undefined; });\n" +
            "        var current = th.getAttribute('aria-sort');\n" +
            "        var direction = current === 'none' ? (numeric ? 'descending' : 'ascending') : (current === 'ascending' ? 'descending' : 'ascending');\n" +
            "        headerCells.forEach(function (other) { if (other.classList.contains('sk-sortable')) { other.setAttribute('aria-sort', 'none'); } });\n" +
            "        th.setAttribute('aria-sort', direction);\n" +
            "        values.sort(function (a, b) { var c = compare(a.value, b.value); return direction === 'ascending' ? c : -c; });\n" +
            "        values.forEach(function (v) { parent.appendChild(v.row); });\n" +
            "        bodyRows = values.map(function (v) { return v.row; });\n" +
            "      }\n" +
            "      th.addEventListener('click', sort);\n" +
            "      th.addEventListener('keydown', function (e) { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); sort(); } });\n" +
            "    });\n" +
            "  }\n" +
            "  // Shell pages: every other top-level table scrolls in its own box, so the page never scrolls sideways.\n" +
            "  function contain() {\n" +
            "    document.querySelectorAll('.sk-main table').forEach(function (table) {\n" +
            "      if (table.parentNode.closest && table.parentNode.closest('table, .sk-table-scroll, .sk-scroll-x')) { return; }\n" +
            "      var box = document.createElement('div');\n" +
            "      box.className = 'sk-scroll-x';\n" +
            "      table.parentNode.insertBefore(box, table);\n" +
            "      box.appendChild(table);\n" +
            "    });\n" +
            "  }\n" +
            "  document.addEventListener('DOMContentLoaded', function () {\n" +
            "    document.querySelectorAll('table.sk-data-table').forEach(enhanceTable);\n" +
            "    contain();\n" +
            "  });\n" +
            "\n" +
            "  // Zoom, pan and fullscreen for a rendered Mermaid diagram (called after mermaid.run).\n" +
            "  window.sokratesEnhanceDiagram = function (pre) {\n" +
            "    var svg = pre.querySelector('svg');\n" +
            "    if (!svg || pre.getAttribute('data-sk-zoom') || !window.svgPanZoom) { return; }\n" +
            "    pre.setAttribute('data-sk-zoom', 'true');\n" +
            "    var box = document.createElement('div');\n" +
            "    box.className = 'sk-diagram';\n" +
            "    pre.parentNode.insertBefore(box, pre);\n" +
            "    box.appendChild(pre);\n" +
            "    var height = Math.max(140, Math.min(640, Math.ceil(svg.getBoundingClientRect().height)));\n" +
            "    svg.style.maxWidth = 'none';\n" +
            "    svg.style.width = '100%';\n" +
            "    svg.style.height = height + 'px';\n" +
            "    var zoom = window.svgPanZoom(svg, {zoomEnabled: true, panEnabled: true, controlIconsEnabled: false,\n" +
            "      mouseWheelZoomEnabled: false, dblClickZoomEnabled: true, fit: true, center: true, minZoom: 0.2, maxZoom: 30});\n" +
            "    function reset() { zoom.resize(); zoom.fit(); zoom.center(); }\n" +
            "    svg.addEventListener('wheel', function (e) {\n" +
            "      if (!e.ctrlKey && !e.metaKey) { return; }\n" +
            "      e.preventDefault();\n" +
            "      if (e.deltaY < 0) { zoom.zoomIn(); } else { zoom.zoomOut(); }\n" +
            "    }, {passive: false});\n" +
            "    var tools = document.createElement('div');\n" +
            "    tools.className = 'sk-diagram-tools';\n" +
            "    [['+', 'Zoom in', function () { zoom.zoomIn(); }],\n" +
            "     ['\\u2212', 'Zoom out', function () { zoom.zoomOut(); }],\n" +
            "     ['\\u2922', 'Fit to view', reset],\n" +
            "     ['\\u26F6', 'Fullscreen', function () {\n" +
            "       if (document.fullscreenElement === box) { document.exitFullscreen(); }\n" +
            "       else if (box.requestFullscreen) { box.requestFullscreen(); }\n" +
            "     }]].forEach(function (b) {\n" +
            "      var button = document.createElement('button');\n" +
            "      button.type = 'button';\n" +
            "      button.textContent = b[0];\n" +
            "      button.title = b[1];\n" +
            "      button.setAttribute('aria-label', b[1]);\n" +
            "      button.addEventListener('click', b[2]);\n" +
            "      tools.appendChild(button);\n" +
            "    });\n" +
            "    box.appendChild(tools);\n" +
            "    document.addEventListener('fullscreenchange', function () {\n" +
            "      svg.style.height = document.fullscreenElement === box ? 'calc(100vh - 24px)' : height + 'px';\n" +
            "      setTimeout(reset, 50);\n" +
            "    });\n" +
            "  };\n" +
            "})();\n";
}
