/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.common.renderingutils.ReportTheme;
import nl.obren.sokrates.reports.utils.HtmlEscapeUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;

import java.util.List;
import java.util.Locale;

/**
 * Renders {@link HealthSummary} at the top of the Overview tab: a grid of headline tiles (value,
 * status, caption, month-bar sparkline, each linking to its report) and the "Where to look first"
 * hotspot list (file, signals, relative score; the file links to the source viewer, which caches it —
 * see {@code ReferencedFiles}). File names and paths come from the repository, so they are escaped.
 */
public class ReportHealthSection {
    private ReportHealthSection() {
    }

    public static final String CSS = "" +
            ".sk-health {display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 12px; margin: 8px 0 22px 0; max-width: 1240px;}\n" +
            ".sk-tile {display: flex; flex-direction: column; gap: 2px; padding: 12px 14px; min-height: 104px; box-sizing: border-box; " +
            "color: var(--sk-text); background: var(--sk-surface); border: 1px solid var(--sk-border); border-radius: 10px; " +
            "box-shadow: var(--sk-shadow); text-decoration: none; transition: box-shadow 0.15s ease, transform 0.15s ease;}\n" +
            "a.sk-tile:hover {box-shadow: var(--sk-shadow-hover); transform: translateY(-1px); text-decoration: none;}\n" +
            ".sk-tile-head {display: flex; align-items: center; justify-content: space-between; gap: 8px;}\n" +
            ".sk-tile-label {font-size: 12px; font-weight: 600; letter-spacing: 0.04em; text-transform: uppercase; color: var(--sk-text-muted);}\n" +
            ".sk-tile-value {font-size: 30px; font-weight: 650; letter-spacing: -0.02em; line-height: 1.15; font-variant-numeric: tabular-nums;}\n" +
            ".sk-tile-caption {font-size: 12px; color: var(--sk-text-muted);}\n" +
            ".sk-tile-spark {margin-top: auto; padding-top: 6px; color: var(--sk-accent);}\n" +
            ".sk-status {font-size: 11px; font-weight: 600; padding: 1px 8px; border-radius: 999px; white-space: nowrap;}\n" +
            ".sk-status-good {color: #1b6e2a; background: #dff3e3;}\n" +
            ".sk-status-watch {color: #8a5300; background: #fdf0d5;}\n" +
            ".sk-status-high {color: #a3161c; background: #fbe1e1;}\n" +
            ".sk-hotspots-card {max-width: 1240px; margin: 0 0 26px 0; padding: 14px 16px 6px 16px; box-sizing: border-box; " +
            "background: var(--sk-surface); border: 1px solid var(--sk-border); border-radius: 10px; box-shadow: var(--sk-shadow);}\n" +
            ".sk-hotspots-title {font-size: 16px; font-weight: 650; color: var(--sk-text);}\n" +
            ".sk-hotspots-intro {margin: 2px 0 8px 0; font-size: 13px; color: var(--sk-text-muted);}\n" +
            ".sk-hotspots {list-style: none; margin: 0; padding: 0;}\n" +
            ".sk-hotspot {display: grid; grid-template-columns: 28px minmax(180px, 2fr) minmax(200px, 3fr) 110px; align-items: center; " +
            "gap: 12px; padding: 8px 0; border-top: 1px solid var(--sk-border);}\n" +
            ".sk-hotspot-rank {font-size: 13px; color: var(--sk-text-faint); text-align: right; font-variant-numeric: tabular-nums;}\n" +
            ".sk-hotspot-file {min-width: 0;}\n" +
            ".sk-hotspot-name {display: block; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;}\n" +
            ".sk-hotspot-path {font-size: 12px; color: var(--sk-text-muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap;}\n" +
            ".sk-hotspot-signals {display: flex; flex-wrap: wrap; gap: 4px;}\n" +
            ".sk-chip {font-size: 12px; padding: 1px 8px; border-radius: 999px; white-space: nowrap; " +
            "color: var(--sk-text-muted); background: var(--sk-surface-3);}\n" +
            ".sk-chip-warn {color: #8a5300; background: #fdf0d5;}\n" +
            ".sk-hotspot-score {height: 6px; border-radius: 3px; background: var(--sk-surface-3); overflow: hidden;}\n" +
            ".sk-hotspot-score span {display: block; height: 100%; border-radius: 3px; background: var(--sk-deleted);}\n" +
            ".sk-hotspots-more {display: inline-block; margin: 8px 0 6px 0; font-size: 13px;}\n" +
            "@media (max-width: 760px) {.sk-hotspot {grid-template-columns: 22px 1fr;} .sk-hotspot-signals, .sk-hotspot-score {grid-column: 2;}}\n" +
            ReportTheme.darkOnly(".sk-status-good", "color: #8fdc9f; background: rgba(46, 125, 50, 0.25);") +
            ReportTheme.darkOnly(".sk-status-watch, .sk-chip-warn", "color: #f5c56b; background: rgba(245, 166, 35, 0.18);") +
            ReportTheme.darkOnly(".sk-status-high", "color: #ff9a9a; background: rgba(198, 40, 40, 0.25);");

    public static void add(RichTextReport report, CodeAnalysisResults results) {
        HealthSummary summary = new HealthSummary(results);
        List<HealthSummary.Tile> tiles = summary.tiles();
        if (!tiles.isEmpty()) {
            StringBuilder html = new StringBuilder("<div class='sk-health'>");
            tiles.forEach(tile -> html.append(tile(tile)));
            html.append("</div>");
            report.addHtmlContent(html.toString());
        }
        List<HealthSummary.Hotspot> hotspots = summary.hotspots();
        if (!hotspots.isEmpty()) {
            boolean viewerLinks = results.getCodeConfiguration().getAnalysis().isSaveSourceFiles();
            report.addHtmlContent(hotspotsCard(hotspots, summary.hasHistory(), viewerLinks));
        }
    }

    static String tile(HealthSummary.Tile tile) {
        StringBuilder html = new StringBuilder();
        html.append("<a class='sk-tile' href='").append(HtmlEscapeUtils.escape(tile.link)).append("' title='")
                .append(HtmlEscapeUtils.escape(tile.tooltip)).append("'>");
        html.append("<div class='sk-tile-head'><span class='sk-tile-label'>").append(HtmlEscapeUtils.escape(tile.label)).append("</span>");
        if (tile.status != HealthSummary.Status.NEUTRAL) {
            html.append("<span class='sk-status sk-status-").append(tile.status.getLabel()).append("'>")
                    .append(tile.status.getLabel()).append("</span>");
        }
        html.append("</div>");
        html.append("<div class='sk-tile-value'>").append(HtmlEscapeUtils.escape(tile.value)).append("</div>");
        html.append("<div class='sk-tile-caption'>").append(HtmlEscapeUtils.escape(tile.caption)).append("</div>");
        if (!tile.sparklineValues.isEmpty()) {
            html.append("<div class='sk-tile-spark'>").append(sparkline(tile.sparklineSlots, tile.sparklineValues)).append("</div>");
        }
        html.append("</a>");
        return html.toString();
    }

    // Month bars (oldest left); the current month is drawn at full opacity.
    static String sparkline(List<String> slots, List<Integer> values) {
        int width = 156;
        int height = 26;
        int n = values.size();
        int max = Math.max(1, values.stream().mapToInt(Integer::intValue).max().orElse(1));
        double slot = (double) width / n;
        double barWidth = Math.max(2, slot - 3);
        StringBuilder svg = new StringBuilder("<svg width='" + width + "' height='" + height + "' viewBox='0 0 " + width + " " + height
                + "' role='img' aria-label='per month, past " + n + " months'>");
        for (int i = 0; i < n; i++) {
            int value = values.get(i);
            double barHeight = value == 0 ? 1 : Math.max(2, (height - 1) * value / (double) max);
            svg.append(String.format(Locale.US, "<rect x='%.1f' y='%.1f' width='%.1f' height='%.1f' rx='1.5' fill='currentColor' opacity='%s'>",
                    i * slot, height - barHeight, barWidth, barHeight, i == n - 1 ? "1" : (value == 0 ? "0.25" : "0.55")));
            svg.append("<title>").append(HtmlEscapeUtils.escape(slots.get(i))).append(": ").append(value).append("</title></rect>");
        }
        svg.append("</svg>");
        return svg.toString();
    }

    static String hotspotsCard(List<HealthSummary.Hotspot> hotspots, boolean history, boolean viewerLinks) {
        StringBuilder html = new StringBuilder("<div class='sk-hotspots-card'>");
        html.append("<div class='sk-hotspots-title'>Where to look first</div>");
        html.append("<div class='sk-hotspots-intro'>");
        if (history) {
            html.append("Main files that are complex <i>and</i> changed often in the past year: changes there are frequent, ")
                    .append("slow and error-prone. Ranked by complexity × days with changes; a single contributor marks knowledge risk.");
        } else {
            html.append("The most complex main files (no git history, so change frequency is unknown).");
        }
        html.append("</div><ol class='sk-hotspots'>");
        long maxScore = Math.max(1, hotspots.get(0).score);
        for (int i = 0; i < hotspots.size(); i++) {
            html.append(hotspot(i + 1, hotspots.get(i), maxScore, history, viewerLinks));
        }
        html.append("</ol>");
        html.append("<a class='sk-hotspots-more' href='index.html#files'>Explore all files →</a>");
        html.append("</div>");
        return html.toString();
    }

    static String hotspot(int rank, HealthSummary.Hotspot hotspot, long maxScore, boolean history, boolean viewerLinks) {
        SourceFile file = hotspot.file;
        String path = file.getRelativePath();
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        String folder = slash >= 0 ? path.substring(0, slash + 1) : "";

        StringBuilder html = new StringBuilder("<li class='sk-hotspot'>");
        html.append("<span class='sk-hotspot-rank'>").append(rank).append("</span>");
        html.append("<div class='sk-hotspot-file' title='").append(HtmlEscapeUtils.escape(path)).append("'>");
        if (viewerLinks) {
            html.append("<a class='sk-hotspot-name' target='_blank' href='").append(HtmlEscapeUtils.viewerFileHref("main", path)).append("'>")
                    .append(HtmlEscapeUtils.escape(name)).append("</a>");
        } else {
            html.append("<span class='sk-hotspot-name'>").append(HtmlEscapeUtils.escape(name)).append("</span>");
        }
        html.append("<div class='sk-hotspot-path'>").append(HtmlEscapeUtils.escape(folder)).append("</div></div>");

        html.append("<div class='sk-hotspot-signals'>");
        if (hotspot.complexityIsLinesOfCode) {
            html.append(chip(String.format(Locale.US, "%,d LOC", hotspot.complexity), false));
        } else {
            html.append(chip(String.format(Locale.US, "McCabe %,d", hotspot.complexity), false));
            html.append(chip(String.format(Locale.US, "%,d LOC", file.getLinesOfCode()), false));
        }
        if (history) {
            html.append(chip(hotspot.changeDays + (hotspot.changeDays == 1 ? " change day" : " change days") + " / 1y", false));
            html.append(chip(hotspot.contributors + (hotspot.contributors == 1 ? " contributor" : " contributors"), hotspot.contributors == 1));
        }
        html.append("</div>");

        long percent = Math.max(2, Math.round(100.0 * hotspot.score / maxScore));
        html.append("<div class='sk-hotspot-score' title='score ").append(hotspot.score).append("'><span style='width: ")
                .append(percent).append("%'></span></div>");
        html.append("</li>");
        return html.toString();
    }

    private static String chip(String text, boolean warn) {
        return "<span class='sk-chip" + (warn ? " sk-chip-warn" : "") + "'>" + HtmlEscapeUtils.escape(text) + "</span>";
    }
}
