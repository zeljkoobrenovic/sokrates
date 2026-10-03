/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.renderingutils;

/**
 * The shared look of every Sokrates HTML page: design tokens (CSS custom properties), their dark
 * variant, a few base element styles and the light/dark/auto theme switch.
 * <p>
 * Server-rendered reports get it through {@code ReportConstants.REPORTS_HTML_HEADER}; client-rendered
 * templates carry a <code>${sokrates-theme}</code> placeholder at the top of their head, which
 * {@link #apply(String)} (called by {@link ExplorerTemplate} and the other template renderers) replaces
 * with {@link #headBlock()}. Template and report CSS should use the <code>--sk-*</code> tokens for
 * chrome (text, surfaces, borders, accents) and keep literal colors only for data (risk categories,
 * severities, scopes), which read the same on light and dark backgrounds.
 * <p>
 * The theme follows the OS setting (<code>prefers-color-scheme</code>) unless the viewer picks light
 * or dark with the toggle in the top-right corner; the choice is kept in <code>localStorage</code>
 * and pushed into embedded iframes with <code>postMessage</code> (file:// pages are separate origins,
 * so an iframe can not always read the same storage). Data visualizations (d3, force graphs) and
 * Mermaid diagrams stay on a light canvas.
 */
public class ReportTheme {
    public static final String PLACEHOLDER = "${sokrates-theme}";

    private static final String LIGHT_TOKENS = "" +
            "--sk-bg: #ffffff;" +
            "--sk-surface: #ffffff;" +
            "--sk-surface-2: #f8f9fb;" +
            "--sk-surface-3: #f1f3f6;" +
            "--sk-hover: #eceff3;" +
            "--sk-border: #e3e6eb;" +
            "--sk-border-strong: #c8cdd5;" +
            "--sk-text: #1f2937;" +
            "--sk-text-muted: #5b6472;" +
            "--sk-text-faint: #8a93a0;" +
            "--sk-accent: #1d4ed8;" +
            "--sk-accent-soft: #eaf0fd;" +
            "--sk-link: #1d4ed8;" +
            "--sk-added: #2e7d32;" +
            "--sk-deleted: #c62828;" +
            "--sk-shadow: 0 1px 2px rgba(16, 24, 40, 0.06);" +
            "--sk-shadow-hover: 0 4px 12px rgba(16, 24, 40, 0.12);" +
            "--sk-canvas: #ffffff;";

    private static final String DARK_TOKENS = "" +
            "--sk-bg: #0f1419;" +
            "--sk-surface: #161b22;" +
            "--sk-surface-2: #1b2129;" +
            "--sk-surface-3: #212833;" +
            "--sk-hover: #29313d;" +
            "--sk-border: #2d3540;" +
            "--sk-border-strong: #424b58;" +
            "--sk-text: #e6e9ee;" +
            "--sk-text-muted: #a3acb9;" +
            "--sk-text-faint: #7d8693;" +
            "--sk-accent: #6ea8fe;" +
            "--sk-accent-soft: #1c2a44;" +
            "--sk-link: #79b0ff;" +
            "--sk-added: #66bb6a;" +
            "--sk-deleted: #ef5350;" +
            "--sk-shadow: 0 1px 2px rgba(0, 0, 0, 0.4);" +
            "--sk-shadow-hover: 0 4px 14px rgba(0, 0, 0, 0.55);" +
            "--sk-canvas: #f5f6f8;";

    private static final String FONT_TOKENS = "" +
            "--sk-font: system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;" +
            "--sk-font-mono: ui-monospace, SFMono-Regular, Menlo, Consolas, 'Liberation Mono', monospace;";

    public static final String TOKENS_CSS = "" +
            ":root {" + FONT_TOKENS + LIGHT_TOKENS + "color-scheme: light;}\n" +
            "@media (prefers-color-scheme: dark) {:root:not([data-theme=\"light\"]) {" + DARK_TOKENS + "color-scheme: dark;}}\n" +
            ":root[data-theme=\"dark\"] {" + DARK_TOKENS + "color-scheme: dark;}\n";

    /**
     * A CSS rule that only applies in dark mode, written once per way dark mode can be on: the OS
     * setting without an explicit light choice, or an explicit dark choice.
     */
    public static String darkOnly(String selector, String declarations) {
        return "@media (prefers-color-scheme: dark) {:root:not([data-theme=\"light\"]) " + selector + " {" + declarations + "}}\n" +
                ":root[data-theme=\"dark\"] " + selector + " {" + declarations + "}\n";
    }

    public static final String BASE_CSS = "" +
            "html {background: var(--sk-bg);}\n" +
            "body {background: var(--sk-bg); color: var(--sk-text); font-family: var(--sk-font); " +
            "-webkit-font-smoothing: antialiased; -moz-osx-font-smoothing: grayscale;}\n" +
            "a {color: var(--sk-link);}\n" +
            "td, th {font-variant-numeric: tabular-nums;}\n" +
            ".sk-icon {color: var(--sk-text);}\n" +
            ".sk-added {color: var(--sk-added);}\n" +
            ".sk-deleted {color: var(--sk-deleted);}\n" +
            ".sk-pill {padding: 3px 12px; margin-right: 4px; cursor: pointer; border-radius: 999px; font-size: 80%; font-family: inherit; " +
            "border: 1px solid transparent; background-color: var(--sk-surface-3); color: var(--sk-text); transition: background-color .15s ease;}\n" +
            ".sk-pill:hover {background-color: var(--sk-hover);}\n" +
            ".sk-pill.active {background-color: var(--sk-text); color: var(--sk-bg);}\n" +
            "#sk-theme-toggle {position: fixed; top: 12px; right: 12px; z-index: 1000; width: 34px; height: 34px; " +
            "padding: 0; display: flex; align-items: center; justify-content: center; cursor: pointer; " +
            "border-radius: 50%; border: 1px solid var(--sk-border); background: var(--sk-surface); " +
            "color: var(--sk-text-muted); box-shadow: var(--sk-shadow); transition: color .15s ease, box-shadow .15s ease;}\n" +
            "#sk-theme-toggle:hover {color: var(--sk-text); box-shadow: var(--sk-shadow-hover);}\n" +
            "#sk-theme-toggle:focus-visible {outline: 2px solid var(--sk-accent); outline-offset: 2px;}\n" +
            "@media print {#sk-theme-toggle {display: none;}}\n" +
            darkOnly(".sk-invert-dark", "filter: invert(0.88);");

    private static final String ICON_AUTO = "<svg width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2'>" +
            "<circle cx='12' cy='12' r='9'/><path d='M12 3a9 9 0 0 1 0 18z' fill='currentColor'/></svg>";
    private static final String ICON_LIGHT = "<svg width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round'>" +
            "<circle cx='12' cy='12' r='4'/><path d='M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4'/></svg>";
    private static final String ICON_DARK = "<svg width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linejoin='round'>" +
            "<path d='M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z'/></svg>";

    // Runs in <head> so data-theme is set before the first paint (no light flash in dark mode).
    public static final String SCRIPT = "" +
            "(function () {\n" +
            "  var KEY = 'sokrates-theme';\n" +
            "  var ICONS = {auto: \"" + ICON_AUTO + "\", light: \"" + ICON_LIGHT + "\", dark: \"" + ICON_DARK + "\"};\n" +
            "  var LABELS = {auto: 'Theme: automatic (follows the system)', light: 'Theme: light', dark: 'Theme: dark'};\n" +
            "  var embedded = window.parent !== window;\n" +
            "  function stored() { try { return localStorage.getItem(KEY); } catch (e) { return null; } }\n" +
            "  function normalized(t) { return t === 'light' || t === 'dark' ? t : 'auto'; }\n" +
            "  function updateButton() {\n" +
            "    var b = document.getElementById('sk-theme-toggle');\n" +
            "    if (!b) { return; }\n" +
            "    var t = window.sokratesTheme;\n" +
            "    b.innerHTML = ICONS[t];\n" +
            "    b.title = LABELS[t] + ' - click to change';\n" +
            "    b.setAttribute('aria-label', b.title);\n" +
            "  }\n" +
            "  function forward(t) {\n" +
            "    var frames = document.getElementsByTagName('iframe');\n" +
            "    for (var i = 0; i < frames.length; i++) {\n" +
            "      try { frames[i].contentWindow.postMessage({sokratesTheme: t}, '*'); } catch (e) {}\n" +
            "    }\n" +
            "  }\n" +
            "  function apply(t) {\n" +
            "    t = normalized(t);\n" +
            "    if (t === 'auto') { document.documentElement.removeAttribute('data-theme'); }\n" +
            "    else { document.documentElement.setAttribute('data-theme', t); }\n" +
            "    window.sokratesTheme = t;\n" +
            "    updateButton();\n" +
            "    forward(t);\n" +
            "    notify();\n" +
            "  }\n" +
            "  // Pages with their own theme-dependent parts (e.g. the code colors of the source viewer)\n" +
            "  // ask sokratesIsDark() and listen to the 'sokrates-theme' event.\n" +
            "  var systemDark = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;\n" +
            "  window.sokratesIsDark = function () {\n" +
            "    var t = window.sokratesTheme;\n" +
            "    return t === 'dark' || (t === 'auto' && !!systemDark && systemDark.matches);\n" +
            "  };\n" +
            "  function notify() {\n" +
            "    try { document.dispatchEvent(new CustomEvent('sokrates-theme', {detail: {dark: window.sokratesIsDark()}})); } catch (e) {}\n" +
            "  }\n" +
            "  if (systemDark && systemDark.addEventListener) {\n" +
            "    systemDark.addEventListener('change', function () { if (window.sokratesTheme === 'auto') { notify(); } });\n" +
            "  }\n" +
            "  window.addEventListener('message', function (e) {\n" +
            "    var d = e.data;\n" +
            "    if (!d || typeof d !== 'object') { return; }\n" +
            "    if (typeof d.sokratesTheme === 'string') { apply(d.sokratesTheme); }\n" +
            "    else if (d.sokratesThemeRequest && e.source) {\n" +
            "      try { e.source.postMessage({sokratesTheme: window.sokratesTheme}, '*'); } catch (x) {}\n" +
            "    }\n" +
            "  });\n" +
            "  window.sokratesCycleTheme = function () {\n" +
            "    var order = ['auto', 'light', 'dark'];\n" +
            "    var next = order[(order.indexOf(window.sokratesTheme) + 1) % order.length];\n" +
            "    try { localStorage.setItem(KEY, next); } catch (e) {}\n" +
            "    apply(next);\n" +
            "  };\n" +
            "  apply(stored());\n" +
            "  // An embedded page asks its parent, which knows the viewer's choice even when storage is not shared.\n" +
            "  if (embedded) { try { window.parent.postMessage({sokratesThemeRequest: true}, '*'); } catch (e) {} }\n" +
            "  function addButton() {\n" +
            "    if (embedded || document.getElementById('sk-theme-toggle')) { return; }\n" +
            "    var b = document.createElement('button');\n" +
            "    b.id = 'sk-theme-toggle';\n" +
            "    b.type = 'button';\n" +
            "    b.onclick = window.sokratesCycleTheme;\n" +
            "    document.body.appendChild(b);\n" +
            "    updateButton();\n" +
            "  }\n" +
            "  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', addButton); }\n" +
            "  else { addButton(); }\n" +
            "})();\n";

    /**
     * Makes a monochrome (black) icon follow the text color, so it stays visible in dark mode:
     * black fills become <code>currentColor</code>, and the root element gets that fill when it has
     * none (SVG's default fill is black). Colored parts keep their colors. The <code>sk-icon</code>
     * class pins the color to the text color, so an icon inside a link does not turn link-blue.
     */
    public static String adaptiveIcon(String svg) {
        if (svg == null) {
            return null;
        }
        String adapted = svg.replaceAll("fill=([\"'])(?i:#000000|#000|#1a1a1a|black)\\1", "fill=$1currentColor$1");
        int rootStart = adapted.indexOf("<svg");
        if (rootStart < 0) {
            return adapted;
        }
        int rootEnd = adapted.indexOf('>', rootStart);
        boolean rootHasFill = rootEnd > 0 && adapted.substring(rootStart, rootEnd).contains("fill=");
        int insertAt = rootStart + "<svg".length();
        return adapted.substring(0, insertAt) + " class=\"sk-icon\"" + (rootHasFill ? "" : " fill=\"currentColor\"") + adapted.substring(insertAt);
    }

    public static String headBlock() {
        return "<style>\n" + TOKENS_CSS + BASE_CSS + "</style>\n<script>\n" + SCRIPT + "</script>\n";
    }

    public static String apply(String html) {
        return html == null ? null : html.replace(PLACEHOLDER, headBlock());
    }
}
