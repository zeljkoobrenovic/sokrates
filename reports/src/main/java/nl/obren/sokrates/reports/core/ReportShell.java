/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

/**
 * The "app shell" of the server-rendered reports, included in {@link ReportConstants#REPORTS_HTML_HEADER}:
 * <ul>
 * <li>the layout of the {@link ReportNavigation} sidebar (sticky on the left, a drawer below 900px),
 * used on pages whose body has the <code>sk-has-shell</code> class;</li>
 * <li>tab routing: a page's open tab is kept in the URL fragment (<code>index.html#files</code>), restored on
 * load and on back/forward, and sidebar links to index tabs are routed in-page;</li>
 * <li>the command palette (⌘K / Ctrl+K, or "/"): the sidebar links, the tabs and section titles of the
 * current page, and a few commands, filtered as you type. It works on every report page, with or
 * without a sidebar. Entries are read from the DOM and rendered with <code>textContent</code>, so
 * repository-controlled names stay text.</li>
 * </ul>
 */
public class ReportShell {
    private ReportShell() {
    }

    public static final String CSS = "" +
            "body.sk-has-shell {margin: 0;}\n" +
            ".sk-sidebar {position: fixed; top: 0; left: 0; bottom: 0; width: 236px; box-sizing: border-box; overflow-y: auto; " +
            "padding: 18px 12px 24px 12px; background: var(--sk-surface-2); border-right: 1px solid var(--sk-border); z-index: 900;}\n" +
            ".sk-main {margin-left: 236px; padding: 4px 40px 40px 40px; min-width: 0;}\n" +
            // The page header (logo, title) as a full-width band in the sidebar's color.
            ".sk-has-shell .sk-page-header {margin: -4px -40px 24px -40px; padding: 4px 40px 0 40px; " +
            "background: var(--sk-surface-2); border-bottom: 1px solid var(--sk-border);}\n" +
            ".sk-sidebar-brand {display: block; padding: 2px 8px 14px 8px; font-size: 17px; font-weight: 600; " +
            "color: var(--sk-text); text-decoration: none; overflow-wrap: anywhere;}\n" +
            ".sk-sidebar-brand:hover {text-decoration: none;}\n" +
            ".sk-search-button {display: flex; align-items: center; gap: 8px; width: 100%; box-sizing: border-box; margin-bottom: 18px; " +
            "padding: 7px 10px; font: inherit; font-size: 13px; color: var(--sk-text-muted); cursor: pointer; " +
            "background: var(--sk-surface); border: 1px solid var(--sk-border); border-radius: 8px;}\n" +
            ".sk-search-button:hover {border-color: var(--sk-border-strong); color: var(--sk-text);}\n" +
            ".sk-search-button span {flex: 1; text-align: left;}\n" +
            ".sk-kbd {font-family: var(--sk-font); font-size: 11px; padding: 0 5px; color: var(--sk-text-faint); " +
            "background: var(--sk-surface-2); border: 1px solid var(--sk-border); border-radius: 4px;}\n" +
            ".sk-nav-group {margin-bottom: 16px;}\n" +
            ".sk-nav-group-label {padding: 0 8px 6px 8px; font-size: 11px; font-weight: 600; letter-spacing: 0.06em; " +
            "text-transform: uppercase; color: var(--sk-text-faint);}\n" +
            ".sk-nav-item {display: flex; align-items: center; gap: 10px; padding: 6px 8px; margin: 1px 0; border-radius: 6px; " +
            "font-size: 14px; color: var(--sk-text-muted); text-decoration: none;}\n" +
            ".sk-nav-item:hover {background: var(--sk-hover); color: var(--sk-text); text-decoration: none;}\n" +
            ".sk-nav-item.active {background: var(--sk-accent-soft); color: var(--sk-accent); font-weight: 600;}\n" +
            ".sk-nav-icon {display: inline-flex; flex: none; width: 16px;}\n" +
            ".sk-nav-label {overflow: hidden; text-overflow: ellipsis; white-space: nowrap;}\n" +
            ".sk-has-shell .sk-index-tabs {display: none;}\n" +
            ".sk-nav-footer {margin-top: 8px; padding: 12px 8px 0 8px; border-top: 1px solid var(--sk-border);}\n" +
            ".sk-nav-meta {margin-top: 14px; font-size: 12px; line-height: 1.6; color: var(--sk-text-faint);}\n" +
            ".sk-nav-meta a {color: var(--sk-text-muted);}\n" +
            ".sk-nav-option {display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--sk-text-muted); cursor: pointer;}\n" +
            ".sk-nav-toggle {display: none;}\n" +
            "@media (max-width: 900px) {\n" +
            "  .sk-sidebar {transform: translateX(-100%); transition: transform 0.2s ease; box-shadow: var(--sk-shadow-hover); padding-top: 58px;}\n" +
            "  .sk-nav-open .sk-sidebar {transform: none;}\n" +
            "  .sk-main {margin-left: 0; padding: 52px 16px 32px 16px;}\n" +
            "  .sk-has-shell .sk-page-header {margin: -52px -16px 18px -16px; padding: 52px 16px 0 16px;}\n" +
            "  .sk-nav-toggle {display: flex; align-items: center; justify-content: center; position: fixed; top: 12px; left: 12px; " +
            "z-index: 950; width: 34px; height: 34px; padding: 0; cursor: pointer; color: var(--sk-text-muted); " +
            "background: var(--sk-surface); border: 1px solid var(--sk-border); border-radius: 8px; box-shadow: var(--sk-shadow);}\n" +
            "}\n" +
            "@media print {.sk-sidebar, .sk-nav-toggle {display: none;} .sk-main {margin-left: 0;}}\n" +
            // command palette
            ".sk-palette {position: fixed; inset: 0; z-index: 2000; display: flex; justify-content: center; align-items: flex-start; " +
            "padding-top: 12vh; background: rgba(15, 20, 25, 0.45);}\n" +
            ".sk-palette[hidden] {display: none;}\n" +
            ".sk-palette-dialog {width: min(640px, calc(100vw - 32px)); max-height: 70vh; display: flex; flex-direction: column; " +
            "overflow: hidden; background: var(--sk-surface); color: var(--sk-text); border: 1px solid var(--sk-border); " +
            "border-radius: 12px; box-shadow: 0 16px 48px rgba(0, 0, 0, 0.3);}\n" +
            ".sk-palette-input {padding: 14px 16px; font: inherit; font-size: 16px; color: var(--sk-text); background: transparent; " +
            "border: none; border-bottom: 1px solid var(--sk-border); outline: none;}\n" +
            ".sk-palette-list {list-style: none; margin: 0; padding: 6px; overflow-y: auto;}\n" +
            ".sk-palette-item {display: flex; align-items: baseline; gap: 10px; padding: 8px 10px; border-radius: 8px; cursor: pointer;}\n" +
            ".sk-palette-item.selected {background: var(--sk-accent-soft);}\n" +
            ".sk-palette-kind {flex: none; width: 64px; font-size: 11px; text-transform: uppercase; letter-spacing: 0.05em; color: var(--sk-text-faint);}\n" +
            ".sk-palette-label {flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;}\n" +
            ".sk-palette-context {flex: none; max-width: 40%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; " +
            "font-size: 12px; color: var(--sk-text-muted);}\n" +
            ".sk-palette-empty {padding: 16px; color: var(--sk-text-muted);}\n" +
            ".sk-palette-hint {padding: 8px 14px; font-size: 12px; color: var(--sk-text-faint); border-top: 1px solid var(--sk-border);}\n";

    public static final String SCRIPT = "" +
            "(function () {\n" +
            "  function tabContent(id) {\n" +
            "    var el = id ? document.getElementById(id) : null;\n" +
            "    return el && el.classList.contains('tabcontent') ? el : null;\n" +
            "  }\n" +
            "  function tabButton(id) {\n" +
            "    var buttons = document.querySelectorAll('.tablinks[data-tab]');\n" +
            "    for (var i = 0; i < buttons.length; i++) { if (buttons[i].getAttribute('data-tab') === id) { return buttons[i]; } }\n" +
            "    return null;\n" +
            "  }\n" +
            "  // The header title (and the browser tab title) follow the active sidebar item, as on the report pages.\n" +
            "  function setPageTitle(item) {\n" +
            "    var label = item.querySelector('.sk-nav-label');\n" +
            "    var title = document.querySelector('[data-sk-page-title]');\n" +
            "    var brand = document.querySelector('.sk-sidebar-brand');\n" +
            "    if (!label || !title) { return; }\n" +
            "    title.textContent = label.textContent;\n" +
            "    var subtitle = document.querySelector('[data-sk-page-subtitle]');\n" +
            "    if (subtitle) {\n" +
            "      subtitle.textContent = item.getAttribute('data-sk-subtitle') || '';\n" +
            "      var link = item.getAttribute('data-sk-subtitle-link');\n" +
            "      if (link) {\n" +
            "        var a = document.createElement('a');\n" +
            "        a.className = 'sk-open-new-tab'; a.target = '_blank'; a.rel = 'noopener'; a.href = link;\n" +
            "        a.textContent = 'Open in new tab \\u2197';\n" +
            "        subtitle.appendChild(document.createTextNode(' \\u00b7 '));\n" +
            "        subtitle.appendChild(a);\n" +
            "      }\n" +
            "    }\n" +
            "    var icon = item.querySelector('.sk-nav-icon svg'), headerIcon = document.querySelector('[data-sk-page-icon]');\n" +
            "    if (headerIcon) {\n" +
            "      headerIcon.textContent = '';\n" +
            "      if (icon) { var big = icon.cloneNode(true); big.setAttribute('width', '68'); big.setAttribute('height', '68'); headerIcon.appendChild(big); }\n" +
            "    }\n" +
            "    document.title = label.textContent + (brand ? ' \\u00b7 ' + brand.textContent : '');\n" +
            "  }\n" +
            "  function markNav(id) {\n" +
            "    var items = document.querySelectorAll('.sk-nav-item[data-sk-tab]');\n" +
            "    var found = false;\n" +
            "    for (var i = 0; i < items.length; i++) { if (items[i].getAttribute('data-sk-tab') === id) { found = true; } }\n" +
            "    if (!found) { return; }\n" +
            "    for (var j = 0; j < items.length; j++) {\n" +
            "      var on = items[j].getAttribute('data-sk-tab') === id;\n" +
            "      items[j].classList.toggle('active', on);\n" +
            "      if (on) { items[j].setAttribute('aria-current', 'page'); } else { items[j].removeAttribute('aria-current'); }\n" +
            "      if (on) { setPageTitle(items[j]); }\n" +
            "    }\n" +
            "  }\n" +
            "  // Shows one tab of the page (the tabs of a report page form one group, as in openTab).\n" +
            "  window.sokratesShowTab = function (id, button) {\n" +
            "    var el = tabContent(id);\n" +
            "    if (!el) { return false; }\n" +
            "    var contents = document.getElementsByClassName('tabcontent');\n" +
            "    for (var i = 0; i < contents.length; i++) { contents[i].style.display = 'none'; }\n" +
            "    var links = document.getElementsByClassName('tablinks');\n" +
            "    for (var j = 0; j < links.length; j++) { links[j].classList.remove('active'); }\n" +
            "    el.style.display = 'block';\n" +
            "    // An iframe that must lay out at its visible size loads on first show (data-sk-src).\n" +
            "    el.querySelectorAll('iframe[data-sk-src]').forEach(function (f) {\n" +
            "      if (!f.getAttribute('src')) { f.setAttribute('src', f.getAttribute('data-sk-src')); }\n" +
            "    });\n" +
            "    var b = button || tabButton(id);\n" +
            "    if (b) { b.classList.add('active'); }\n" +
            "    markNav(id);\n" +
            "    if (window.renderMermaidIn) { window.renderMermaidIn(el); }\n" +
            "    return true;\n" +
            "  };\n" +
            "  window.sokratesRememberTab = function (id) {\n" +
            "    if (decodeURIComponent(location.hash.slice(1)) === id) { return; }\n" +
            "    try { history.pushState(null, '', '#' + encodeURIComponent(id)); } catch (e) {}\n" +
            "  };\n" +
            "  function openFromHash(scrollTop) {\n" +
            "    var id = decodeURIComponent(location.hash.slice(1));\n" +
            "    if (window.sokratesShowTab(id) && scrollTop) { holdTop(); }\n" +
            "  }\n" +
            "  window.addEventListener('popstate', function () { openFromHash(false); });\n" +
            "  // A typed or linked #tab jumps to the tab's div first; start at the top of the tab instead.\n" +
            "  window.addEventListener('hashchange', function () { openFromHash(true); });\n" +
            "  // The tab of this page a link points at (href '#tab' or '<this file>#tab'), or null.\n" +
            "  function linkedTab(link) {\n" +
            "    var href = link.getAttribute('href') || '';\n" +
            "    var hash = href.indexOf('#');\n" +
            "    var file = href.substring(0, hash);\n" +
            "    var page = location.pathname.substring(location.pathname.lastIndexOf('/') + 1);\n" +
            "    if (file && file !== page) { return null; }\n" +
            "    var id = decodeURIComponent(href.substring(hash + 1));\n" +
            "    return tabContent(id) ? id : null;\n" +
            "  }\n" +
            "  window.sokratesToggleNav = function (open) {\n" +
            "    var on = typeof open === 'boolean' ? open : !document.body.classList.contains('sk-nav-open');\n" +
            "    document.body.classList.toggle('sk-nav-open', on);\n" +
            "  };\n" +
            "  document.addEventListener('click', function (e) {\n" +
            "    var link = e.target.closest ? e.target.closest('a[href*=\"#\"]') : null;\n" +
            "    if (link && !link.target) {\n" +
            "      var id = linkedTab(link);\n" +
            "      if (tabContent(id) && !e.metaKey && !e.ctrlKey && !e.shiftKey) {\n" +
            "        e.preventDefault();\n" +
            "        window.sokratesShowTab(id);\n" +
            "        window.sokratesRememberTab(id);\n" +
            "        window.scrollTo(0, 0);\n" +
            "      }\n" +
            "      if (id || link.classList.contains('sk-nav-item')) { window.sokratesToggleNav(false); return; }\n" +
            "    }\n" +
            "    if (document.body.classList.contains('sk-nav-open') && !(e.target.closest && e.target.closest('.sk-sidebar, .sk-nav-toggle'))) {\n" +
            "      window.sokratesToggleNav(false);\n" +
            "    }\n" +
            "  });\n" +
            "  // A tab opened from the URL starts at the top. The browser scrolls to the element named by the\n" +
            "  // fragment (the tab's div) and repeats that on later layout changes (web font, frames loading),\n" +
            "  // so for a few seconds after such an open any scroll is undone, until the viewer interacts.\n" +
            "  var holdTopUntil = 0;\n" +
            "  function holdTop() { holdTopUntil = Date.now() + 4000; window.scrollTo(0, 0); }\n" +
            "  ['wheel', 'touchstart', 'keydown', 'mousedown'].forEach(function (type) {\n" +
            "    window.addEventListener(type, function () { holdTopUntil = 0; }, {passive: true, capture: true});\n" +
            "  });\n" +
            "  window.addEventListener('scroll', function () {\n" +
            "    if (Date.now() < holdTopUntil && window.scrollY !== 0) { window.scrollTo(0, 0); }\n" +
            "  }, {passive: true});\n" +
            "  document.addEventListener('DOMContentLoaded', function () {\n" +
            "    if (location.hash) { openFromHash(true); }\n" +
            "    if (!/Mac|iPhone|iPad/.test(navigator.platform || '')) {\n" +
            "      document.querySelectorAll('.sk-search-button .sk-kbd').forEach(function (k) { k.textContent = 'Ctrl K'; });\n" +
            "    }\n" +
            "  });\n" +
            "\n" +
            "  // Command palette.\n" +
            "  var palette, input, list, entries = [], shown = [], selected = 0;\n" +
            "  function text(el) { return (el.textContent || '').replace(/\\s+/g, ' ').trim(); }\n" +
            "  function tabLabel(id) { var b = tabButton(id); return b ? text(b) : ''; }\n" +
            "  function collect() {\n" +
            "    var result = [], seenTabs = {};\n" +
            "    document.querySelectorAll('.sk-nav-group').forEach(function (group) {\n" +
            "      var groupLabel = text(group.querySelector('.sk-nav-group-label') || group);\n" +
            "      group.querySelectorAll('a.sk-nav-item').forEach(function (a) {\n" +
            "        var tab = a.getAttribute('data-sk-tab');\n" +
            "        if (tab && tabContent(tab)) { seenTabs[tab] = true; }\n" +
            "        result.push({kind: 'Page', label: text(a), context: groupLabel, run: function () {\n" +
            "          if (tab && tabContent(tab)) { window.sokratesShowTab(tab); window.sokratesRememberTab(tab); window.scrollTo(0, 0); }\n" +
            "          else { window.location.href = a.getAttribute('href'); }\n" +
            "        }});\n" +
            "      });\n" +
            "    });\n" +
            "    document.querySelectorAll('.tablinks[data-tab]').forEach(function (b) {\n" +
            "      var id = b.getAttribute('data-tab');\n" +
            "      if (seenTabs[id] || !tabContent(id)) { return; }\n" +
            "      seenTabs[id] = true;\n" +
            "      result.push({kind: 'Tab', label: text(b), context: '', run: function () {\n" +
            "        window.sokratesShowTab(id); window.sokratesRememberTab(id); window.scrollTo(0, 0);\n" +
            "      }});\n" +
            "    });\n" +
            "    document.querySelectorAll('.sectionTitle, .subSectionTitle').forEach(function (t) {\n" +
            "      var label = text(t);\n" +
            "      if (!label) { return; }\n" +
            "      var tab = t.closest('.tabcontent');\n" +
            "      result.push({kind: 'Section', label: label, context: tab ? tabLabel(tab.id) : '', run: function () {\n" +
            "        if (tab && tab.style.display === 'none') { window.sokratesShowTab(tab.id); window.sokratesRememberTab(tab.id); }\n" +
            "        t.scrollIntoView({block: 'start'});\n" +
            "      }});\n" +
            "    });\n" +
            "    result.push({kind: 'Command', label: 'Change theme (light / dark / automatic)', context: '', run: function () {\n" +
            "      if (window.sokratesCycleTheme) { window.sokratesCycleTheme(); }\n" +
            "    }});\n" +
            "    result.push({kind: 'Command', label: 'Colour-blind safe colours: ' + (window.sokratesPalette === 'cvd' ? 'turn off' : 'turn on'), context: '', run: function () {\n" +
            "      if (window.sokratesSetPalette) { window.sokratesSetPalette(window.sokratesPalette === 'cvd' ? 'default' : 'cvd'); }\n" +
            "    }});\n" +
            "    result.push({kind: 'Command', label: 'Print or save as PDF', context: '', run: function () { window.print(); }});\n" +
            "    return result;\n" +
            "  }\n" +
            "  function matches(entry, terms) {\n" +
            "    var hay = (entry.label + ' ' + entry.context + ' ' + entry.kind).toLowerCase();\n" +
            "    for (var i = 0; i < terms.length; i++) { if (hay.indexOf(terms[i]) < 0) { return false; } }\n" +
            "    return true;\n" +
            "  }\n" +
            "  function render() {\n" +
            "    var q = input.value.toLowerCase().trim();\n" +
            "    var terms = q ? q.split(/\\s+/) : [];\n" +
            "    shown = entries.filter(function (e) { return matches(e, terms); });\n" +
            "    if (q) {\n" +
            "      shown.sort(function (a, b) {\n" +
            "        var sa = a.label.toLowerCase().indexOf(q) === 0 ? 0 : 1, sb = b.label.toLowerCase().indexOf(q) === 0 ? 0 : 1;\n" +
            "        return sa - sb;\n" +
            "      });\n" +
            "    }\n" +
            "    shown = shown.slice(0, 60);\n" +
            "    selected = Math.min(selected, Math.max(0, shown.length - 1));\n" +
            "    list.textContent = '';\n" +
            "    if (shown.length === 0) {\n" +
            "      var empty = document.createElement('li');\n" +
            "      empty.className = 'sk-palette-empty';\n" +
            "      empty.textContent = 'No matches';\n" +
            "      list.appendChild(empty);\n" +
            "      return;\n" +
            "    }\n" +
            "    shown.forEach(function (entry, i) {\n" +
            "      var li = document.createElement('li');\n" +
            "      li.className = 'sk-palette-item' + (i === selected ? ' selected' : '');\n" +
            "      li.setAttribute('role', 'option');\n" +
            "      li.setAttribute('aria-selected', i === selected ? 'true' : 'false');\n" +
            "      [['sk-palette-kind', entry.kind], ['sk-palette-label', entry.label], ['sk-palette-context', entry.context]].forEach(function (part) {\n" +
            "        var span = document.createElement('span');\n" +
            "        span.className = part[0];\n" +
            "        span.textContent = part[1];\n" +
            "        li.appendChild(span);\n" +
            "      });\n" +
            "      li.addEventListener('mousemove', function () { if (selected !== i) { selected = i; highlight(); } });\n" +
            "      li.addEventListener('click', function () { choose(i); });\n" +
            "      list.appendChild(li);\n" +
            "    });\n" +
            "  }\n" +
            "  function highlight() {\n" +
            "    var items = list.querySelectorAll('.sk-palette-item');\n" +
            "    for (var i = 0; i < items.length; i++) {\n" +
            "      items[i].classList.toggle('selected', i === selected);\n" +
            "      items[i].setAttribute('aria-selected', i === selected ? 'true' : 'false');\n" +
            "    }\n" +
            "    if (items[selected]) { items[selected].scrollIntoView({block: 'nearest'}); }\n" +
            "  }\n" +
            "  function choose(i) {\n" +
            "    var entry = shown[i];\n" +
            "    window.sokratesClosePalette();\n" +
            "    if (entry) { entry.run(); }\n" +
            "  }\n" +
            "  function build() {\n" +
            "    palette = document.createElement('div');\n" +
            "    palette.className = 'sk-palette';\n" +
            "    palette.hidden = true;\n" +
            "    var dialog = document.createElement('div');\n" +
            "    dialog.className = 'sk-palette-dialog';\n" +
            "    dialog.setAttribute('role', 'dialog');\n" +
            "    dialog.setAttribute('aria-modal', 'true');\n" +
            "    dialog.setAttribute('aria-label', 'Search the report');\n" +
            "    input = document.createElement('input');\n" +
            "    input.className = 'sk-palette-input';\n" +
            "    input.type = 'text';\n" +
            "    input.placeholder = 'Search pages, tabs, sections and commands…';\n" +
            "    input.setAttribute('aria-label', 'Search');\n" +
            "    list = document.createElement('ul');\n" +
            "    list.className = 'sk-palette-list';\n" +
            "    list.setAttribute('role', 'listbox');\n" +
            "    var hint = document.createElement('div');\n" +
            "    hint.className = 'sk-palette-hint';\n" +
            "    hint.textContent = '↑ ↓ to move · Enter to open · Esc to close';\n" +
            "    dialog.appendChild(input); dialog.appendChild(list); dialog.appendChild(hint);\n" +
            "    palette.appendChild(dialog);\n" +
            "    document.body.appendChild(palette);\n" +
            "    palette.addEventListener('mousedown', function (e) { if (e.target === palette) { window.sokratesClosePalette(); } });\n" +
            "    input.addEventListener('input', function () { selected = 0; render(); });\n" +
            "    input.addEventListener('keydown', function (e) {\n" +
            "      if (e.key === 'ArrowDown') { e.preventDefault(); selected = Math.min(selected + 1, shown.length - 1); highlight(); }\n" +
            "      else if (e.key === 'ArrowUp') { e.preventDefault(); selected = Math.max(selected - 1, 0); highlight(); }\n" +
            "      else if (e.key === 'Enter') { e.preventDefault(); choose(selected); }\n" +
            "      else if (e.key === 'Escape') { e.preventDefault(); window.sokratesClosePalette(); }\n" +
            "    });\n" +
            "  }\n" +
            "  var returnFocus = null;\n" +
            "  window.sokratesOpenPalette = function () {\n" +
            "    if (!palette) { build(); }\n" +
            "    returnFocus = document.activeElement;\n" +
            "    entries = collect();\n" +
            "    input.value = '';\n" +
            "    selected = 0;\n" +
            "    render();\n" +
            "    palette.hidden = false;\n" +
            "    input.focus();\n" +
            "  };\n" +
            "  window.sokratesClosePalette = function () {\n" +
            "    if (!palette || palette.hidden) { return; }\n" +
            "    palette.hidden = true;\n" +
            "    if (returnFocus && returnFocus.focus) { try { returnFocus.focus(); } catch (e) {} }\n" +
            "  };\n" +
            "  document.addEventListener('keydown', function (e) {\n" +
            "    var typing = /^(INPUT|TEXTAREA|SELECT)$/.test((e.target && e.target.tagName) || '') || (e.target && e.target.isContentEditable);\n" +
            "    if ((e.metaKey || e.ctrlKey) && (e.key === 'k' || e.key === 'K')) {\n" +
            "      e.preventDefault();\n" +
            "      if (palette && !palette.hidden) { window.sokratesClosePalette(); } else { window.sokratesOpenPalette(); }\n" +
            "    } else if (e.key === '/' && !typing && !e.metaKey && !e.ctrlKey && !e.altKey) {\n" +
            "      e.preventDefault();\n" +
            "      window.sokratesOpenPalette();\n" +
            "    }\n" +
            "  });\n" +
            "})();\n";
}
