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
 * <li>the sidebar search (⌘K / Ctrl+K, or "/" to focus): filters the sidebar's own items in place as you
 * type, no popup; Enter opens the first match, Esc clears. Only pages with a sidebar have it.</li>
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
            ".sk-page-context {display: block; padding-top: 10px; font-size: 15px; font-weight: 600; color: var(--sk-text-muted); " +
            "text-decoration: none; overflow-wrap: anywhere;}\n" +
            ".sk-page-context:hover {color: var(--sk-text); text-decoration: none;}\n" +
            ".sk-page-context[hidden] {display: none;}\n" +            ".sk-nav-search {display: flex; align-items: center; gap: 8px; margin-bottom: 18px; padding: 0 10px; color: var(--sk-text-muted); " +
            "background: var(--sk-surface); border: 1px solid var(--sk-border); border-radius: 8px;}\n" +
            ".sk-nav-search:focus-within {border-color: var(--sk-accent); color: var(--sk-text);}\n" +
            ".sk-nav-search-input {flex: 1; min-width: 0; padding: 7px 0; font: inherit; font-size: 13px; color: var(--sk-text); " +
            "background: transparent; border: none; outline: none;}\n" +
            ".sk-nav-search-input::-webkit-search-cancel-button {cursor: pointer;}\n" +
            ".sk-nav-search:focus-within .sk-kbd {display: none;}\n" +
            ".sk-nav-empty {padding: 0 8px 12px 8px; font-size: 13px; color: var(--sk-text-faint);}\n" +
            ".sk-nav-item.sk-nav-first {background: var(--sk-hover); color: var(--sk-text);}\n" +
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
            // Sub-items (a tab inside an index tab's page) sit indented under their parent's label.
            ".sk-nav-item.sk-nav-sub {padding: 4px 8px 4px 42px; font-size: 13px;}\n" +
            ".sk-nav-sub .sk-nav-icon {display: none;}\n" +
            ".sk-nav-label {overflow: hidden; text-overflow: ellipsis; white-space: nowrap;}\n" +
            ".sk-has-shell .sk-index-tabs {display: none;}\n" +
            ".sk-nav-footer {margin-top: 8px; padding: 12px 8px 0 8px; border-top: 1px solid var(--sk-border);}\n" +
            ".sk-nav-meta {margin-top: 14px; font-size: 12px; line-height: 1.6; color: var(--sk-text-faint);}\n" +
            ".sk-nav-meta a {color: var(--sk-text-muted);}\n" +
            ".sk-nav-option {display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--sk-text-muted); cursor: pointer;}\n" +
            ".sk-nav-toggle {display: none;}\n" +
            ".sk-nav-collapse {display: none;}\n" +
            // Desktop: the sidebar can be hidden (html.sk-nav-hidden, remembered in localStorage['sokrates-nav']);
            // the menu button then shows it again. Below 900px the sidebar is a drawer instead.
            "@media (min-width: 901px) {\n" +
            "  .sk-nav-collapse {display: flex; align-items: center; justify-content: center; position: absolute; top: 20px; right: 10px; " +
            "width: 26px; height: 26px; padding: 0; cursor: pointer; color: var(--sk-text-faint); background: transparent; " +
            "border: 1px solid transparent; border-radius: 6px;}\n" +
            "  .sk-nav-collapse:hover {color: var(--sk-text); background: var(--sk-surface); border-color: var(--sk-border);}\n" +
            "  .sk-nav-collapse + .sk-nav-search {margin-right: 32px;}\n" +
            "  .sk-nav-hidden .sk-sidebar {display: none;}\n" +
            "  .sk-nav-hidden .sk-main {margin-left: 0; padding-left: 64px;}\n" +
            "  .sk-nav-hidden .sk-has-shell .sk-page-header {margin-left: -64px; padding-left: 64px;}\n" +
            "  .sk-nav-hidden .sk-nav-toggle {display: flex; align-items: center; justify-content: center; position: fixed; top: 12px; left: 12px; " +
            "z-index: 950; width: 34px; height: 34px; padding: 0; cursor: pointer; color: var(--sk-text-muted); " +
            "background: var(--sk-surface); border: 1px solid var(--sk-border); border-radius: 8px; box-shadow: var(--sk-shadow);}\n" +
            "}\n" +
            "@media (max-width: 900px) {\n" +
            "  .sk-sidebar {transform: translateX(-100%); transition: transform 0.2s ease; box-shadow: var(--sk-shadow-hover); padding-top: 58px;}\n" +
            "  .sk-nav-open .sk-sidebar {transform: none;}\n" +
            "  .sk-main {margin-left: 0; padding: 52px 16px 32px 16px;}\n" +
            "  .sk-has-shell .sk-page-header {margin: -52px -16px 18px -16px; padding: 52px 16px 0 16px;}\n" +
            "  .sk-nav-toggle {display: flex; align-items: center; justify-content: center; position: fixed; top: 12px; left: 12px; " +
            "z-index: 950; width: 34px; height: 34px; padding: 0; cursor: pointer; color: var(--sk-text-muted); " +
            "background: var(--sk-surface); border: 1px solid var(--sk-border); border-radius: 8px; box-shadow: var(--sk-shadow);}\n" +
            "}\n" +
            "@media print {.sk-sidebar, .sk-nav-toggle {display: none;} .sk-main {margin-left: 0;}}\n";

    public static final String SCRIPT = "" +
            "(function () {\n" +
            "  // A hidden sidebar is restored before the page paints (this script runs in the head).\n" +
            "  try { if (localStorage.getItem('sokrates-nav') === 'hidden') { document.documentElement.classList.add('sk-nav-hidden'); } } catch (e) {}\n" +
            "  function tabContent(id) {\n" +
            "    var el = id ? document.getElementById(id) : null;\n" +
            "    return el && el.classList.contains('tabcontent') ? el : null;\n" +
            "  }\n" +
            "  // The tab of a fragment: the id itself, or for a sub-item <tab>/<sub> its tab; null when none.\n" +
            "  function baseTab(id) {\n" +
            "    if (!id) { return null; }\n" +
            "    if (tabContent(id)) { return id; }\n" +
            "    var slash = id.indexOf('/');\n" +
            "    return slash > 0 && tabContent(id.substring(0, slash)) ? id.substring(0, slash) : null;\n" +
            "  }\n" +
            "  function framePath(src) { return (src || '').split('#')[0].split('?')[0]; }\n" +
            "  // A sub-item points the tab's iframe showing the same page at its own URL (repositories.html?tab=...);\n" +
            "  // the tab itself (or another sub-item) puts the iframe back on its own URL. The iframe is replaced by a\n" +
            "  // copy with the new URL instead of navigated, so the browser's back button is not spent on the frame.\n" +
            "  function setFrameSrc(f, src) {\n" +
            "    var copy = f.cloneNode(false);\n" +
            "    copy.setAttribute('src', src);\n" +
            "    f.parentNode.replaceChild(copy, f);\n" +
            "  }\n" +
            "  function applyFrame(id, el) {\n" +
            "    var item = document.querySelector('.sk-nav-item[data-sk-nav=\"' + id.replace(/\"/g, '') + '\"]');\n" +
            "    var src = item ? item.getAttribute('data-sk-frame-src') : null;\n" +
            "    el.querySelectorAll('iframe').forEach(function (f) {\n" +
            "      var original = f.getAttribute('data-sk-frame-default') || f.getAttribute('src');\n" +
            "      if (!original) { return; }\n" +
            "      if (src && framePath(src) === framePath(original)) {\n" +
            "        if (!f.getAttribute('data-sk-frame-default')) { f.setAttribute('data-sk-frame-default', original); }\n" +
            "        if (f.getAttribute('src') !== src) { setFrameSrc(f, src); }\n" +
            "      } else if (!src && f.getAttribute('data-sk-frame-default') && f.getAttribute('src') !== original) {\n" +
            "        setFrameSrc(f, original);\n" +
            "      }\n" +
            "    });\n" +
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
            "    var brand = document.querySelector('.sk-page-context');\n" +
            "    if (!label || !title) { return; }\n" +
            "    // An item's data-sk-title (e.g. the repository name on the Overview) wins over its sidebar label.\n" +
            "    var heading = item.getAttribute('data-sk-title') || label.textContent;\n" +
            "    title.textContent = heading;\n" +
            "    if (brand) { brand.hidden = heading === brand.textContent; }\n" +
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
            "    document.title = heading + (brand && heading !== brand.textContent ? ' \\u00b7 ' + brand.textContent : '');\n" +
            "  }\n" +
            "  function markNav(id) {\n" +
            "    var items = document.querySelectorAll('.sk-nav-item[data-sk-tab]');\n" +
            "    var found = false;\n" +
            "    for (var i = 0; i < items.length; i++) { if (items[i].getAttribute('data-sk-nav') === id) { found = true; } }\n" +
            "    if (!found) { return; }\n" +
            "    for (var j = 0; j < items.length; j++) {\n" +
            "      var on = items[j].getAttribute('data-sk-nav') === id;\n" +
            "      items[j].classList.toggle('active', on);\n" +
            "      if (on) { items[j].setAttribute('aria-current', 'page'); } else { items[j].removeAttribute('aria-current'); }\n" +
            "      if (on) { setPageTitle(items[j]); }\n" +
            "    }\n" +
            "  }\n" +
            "  // Shows one tab of the page (the tabs of a report page form one group, as in openTab).\n" +
            "  window.sokratesShowTab = function (id, button) {\n" +
            "    var base = baseTab(id);\n" +
            "    var el = tabContent(base);\n" +
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
            "    var b = button || tabButton(base);\n" +
            "    if (b) { b.classList.add('active'); }\n" +
            "    markNav(id);\n" +
            "    applyFrame(id, el);\n" +
            "    if (window.renderMermaidIn) { window.renderMermaidIn(el); }\n" +
            "    return true;\n" +
            "  };\n" +
            "  window.sokratesRememberTab = function (id) {\n" +
            "    if (decodeURIComponent(location.hash.slice(1)) === id) { return; }\n" +
            "    try { history.pushState(null, '', '#' + encodeURIComponent(id)); } catch (e) {}\n" +
            "  };\n" +
            "  function openFromHash(scrollTop) {\n" +
            "    var id = decodeURIComponent(location.hash.slice(1));\n" +
            "    // Back to the page's own URL (no fragment): its first tab, as on load.\n" +
            "    if (!id) { var first = document.querySelector('.tablinks[data-tab]'); id = first ? first.getAttribute('data-tab') : ''; }\n" +
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
            "    return baseTab(id) ? id : null;\n" +
            "  }\n" +
            "  window.sokratesSetNavHidden = function (hidden) {\n" +
            "    document.documentElement.classList.toggle('sk-nav-hidden', hidden);\n" +
            "    try { if (hidden) { localStorage.setItem('sokrates-nav', 'hidden'); } else { localStorage.removeItem('sokrates-nav'); } } catch (e) {}\n" +
            "  };\n" +
            "  function narrow() { return window.matchMedia && window.matchMedia('(max-width: 900px)').matches; }\n" +
            "  // The menu button: on desktop it shows/hides the sidebar, below 900px it opens/closes the drawer\n" +
            "  // (an explicit true/false always means the drawer, e.g. closing it after a link).\n" +
            "  window.sokratesToggleNav = function (open) {\n" +
            "    if (typeof open !== 'boolean' && !narrow()) {\n" +
            "      window.sokratesSetNavHidden(!document.documentElement.classList.contains('sk-nav-hidden'));\n" +
            "      return;\n" +
            "    }\n" +
            "    var on = typeof open === 'boolean' ? open : !document.body.classList.contains('sk-nav-open');\n" +
            "    document.body.classList.toggle('sk-nav-open', on);\n" +
            "  };\n" +
            "  document.addEventListener('click', function (e) {\n" +
            "    var link = e.target.closest ? e.target.closest('a[href*=\"#\"]') : null;\n" +
            "    if (link && !link.target) {\n" +
            "      var id = linkedTab(link);\n" +
            "      if (baseTab(id) && !e.metaKey && !e.ctrlKey && !e.shiftKey) {\n" +
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
            "      document.querySelectorAll('.sk-nav-search .sk-kbd').forEach(function (k) { k.textContent = 'Ctrl K'; });\n" +
            "    }\n" +
            "  });\n" +
            "\n" +
            "  // Sidebar search: filters the sidebar's own items in place (no popup). ⌘K / Ctrl+K or \"/\" focus it;\n" +
            "  // Enter opens the first match, Esc clears. Labels are matched as text (all terms, any order).\n" +
            "  function text(el) { return (el.textContent || '').replace(/\\s+/g, ' ').trim(); }\n" +
            "  window.sokratesFilterNav = function (query) {\n" +
            "    var terms = (query || '').toLowerCase().trim().split(/\\s+/).filter(function (t) { return t; });\n" +
            "    var first = null;\n" +
            "    document.querySelectorAll('.sk-sidebar .sk-nav-group').forEach(function (group) {\n" +
            "      var groupLabel = text(group.querySelector('.sk-nav-group-label') || group).toLowerCase();\n" +
            "      var visible = 0;\n" +
            "      group.querySelectorAll('a.sk-nav-item').forEach(function (a) {\n" +
            "        var hay = text(a).toLowerCase() + ' ' + groupLabel;\n" +
            "        var hit = terms.every(function (t) { return hay.indexOf(t) >= 0; });\n" +
            "        a.style.display = hit ? '' : 'none';\n" +
            "        a.classList.remove('sk-nav-first');\n" +
            "        if (hit) { visible++; if (!first && terms.length) { first = a; } }\n" +
            "      });\n" +
            "      group.style.display = visible ? '' : 'none';\n" +
            "    });\n" +
            "    if (first) { first.classList.add('sk-nav-first'); }\n" +
            "    var empty = document.querySelector('.sk-sidebar .sk-nav-empty');\n" +
            "    if (empty) { empty.hidden = !(terms.length && !first); }\n" +
            "    return first;\n" +
            "  };\n" +
            "  function navSearch() { return document.querySelector('.sk-sidebar .sk-nav-search-input'); }\n" +
            "  document.addEventListener('keydown', function (e) {\n" +
            "    var input = navSearch();\n" +
            "    if (!input) { return; }\n" +
            "    if (e.target === input) {\n" +
            "      if (e.key === 'Enter') {\n" +
            "        e.preventDefault();\n" +
            "        var first = window.sokratesFilterNav(input.value);\n" +
            "        if (first) { input.value = ''; window.sokratesFilterNav(''); input.blur(); first.click(); }\n" +
            "      } else if (e.key === 'Escape') {\n" +
            "        e.preventDefault(); input.value = ''; window.sokratesFilterNav(''); input.blur();\n" +
            "      }\n" +
            "      return;\n" +
            "    }\n" +
            "    var typing = /^(INPUT|TEXTAREA|SELECT)$/.test((e.target && e.target.tagName) || '') || (e.target && e.target.isContentEditable);\n" +
            "    if (((e.metaKey || e.ctrlKey) && (e.key === 'k' || e.key === 'K')) || (e.key === '/' && !typing && !e.metaKey && !e.ctrlKey && !e.altKey)) {\n" +
            "      e.preventDefault();\n" +
            "      if (narrow()) { window.sokratesToggleNav(true); } else { window.sokratesSetNavHidden(false); }\n" +
            "      input.focus();\n" +
            "      input.select();\n" +
            "    }\n" +
            "  });\n" +
            "})();\n";
}
