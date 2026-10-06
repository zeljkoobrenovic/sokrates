/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.common.renderingutils.ReportTheme;
import nl.obren.sokrates.reports.utils.HtmlEscapeUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.scores.MaintainabilityScore;
import nl.obren.sokrates.sourcecode.analysis.scores.MaintainabilityScores;
import nl.obren.sokrates.sourcecode.analysis.scores.SubScore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
            ".sk-tile-label {font-size: 12px; font-weight: 600; letter-spacing: 0.04em; text-transform: uppercase; color: var(--sk-text-muted); " +
            "white-space: nowrap; overflow: hidden; text-overflow: ellipsis;}\n" +
            ".sk-tile-value-row {display: flex; align-items: center; flex-wrap: wrap; gap: 4px 10px;}\n" +
            ".sk-tile-value {font-size: 30px; line-height: 1.15; font-variant-numeric: tabular-nums;}\n" +
            ".sk-tile-caption {font-size: 12px; color: var(--sk-text-muted);}\n" +
            ".sk-tile-spark {margin-top: auto; padding-top: 6px; color: var(--sk-accent); min-width: 0;}\n" +
            ".sk-tile-spark svg {display: block; max-width: 100%;}\n" +
            ".sk-status {font-size: 11px; font-weight: 600; padding: 1px 8px; border-radius: 999px; white-space: nowrap;}\n" +
            ".sk-status-good {color: #1b6e2a; background: #dff3e3;}\n" +
            ".sk-status-watch {color: #8a5300; background: #fdf0d5;}\n" +
            ".sk-status-high {color: #a3161c; background: #fbe1e1;}\n" +
            ".sk-hotspots-card {max-width: 1240px; margin: 0 0 26px 0; padding: 14px 16px 6px 16px; box-sizing: border-box; " +
            "background: var(--sk-surface); border: 1px solid var(--sk-border); border-radius: 10px; box-shadow: var(--sk-shadow);}\n" +
            ".sk-hotspots-title {font-size: 16px; font-weight: 600; color: var(--sk-text);}\n" +
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
            ".sk-score-note {color: var(--sk-text-faint);}\n" +
            // The A-E grade scale (energy-label style): every grade in its color, the current one larger and solid.
            ".sk-grade-scale {display: inline-flex; align-items: center; gap: 2px; vertical-align: middle;}\n" +
            ".sk-grade {display: inline-flex; align-items: center; justify-content: center; width: 14px; height: 14px; border-radius: 3px; " +
            "font-size: 9px; font-weight: 600; line-height: 1; color: rgba(0, 0, 0, 0.55); opacity: 0.28;}\n" +
            ".sk-grade.sk-grade-on {width: 24px; height: 24px; font-size: 15px; opacity: 1; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);}\n" +
            ".sk-grade-lg .sk-grade {width: 18px; height: 18px; font-size: 11px;}\n" +
            ".sk-grade-lg .sk-grade.sk-grade-on {width: 32px; height: 32px; font-size: 20px;}\n" +
            ".sk-grade-a {background: var(--sk-risk-negligible, #1a9641); color: #fff;}\n" +
            ".sk-grade-b {background: var(--sk-risk-low, #a6d96a); color: #1f3d0c;}\n" +
            ".sk-grade-c {background: #fee08b; color: #5c4400;}\n" +
            ".sk-grade-d {background: var(--sk-risk-high, #fdae61); color: #5c2a00;}\n" +
            ".sk-grade-e {background: var(--sk-risk-very-high, #d7191c); color: #fff;}\n" +
            ".sk-scores {display: grid; grid-template-columns: repeat(auto-fit, minmax(340px, 1fr)); gap: 8px 32px; margin: 6px 0 10px 0;}\n" +
            ".sk-score-head {display: flex; align-items: center; gap: 10px; padding-bottom: 4px;}\n" +
            ".sk-score-name {font-size: 13px; font-weight: 600; letter-spacing: 0.04em; text-transform: uppercase; color: var(--sk-text-muted); min-width: 52px;}\n" +
            ".sk-score-total {font-size: 28px; font-variant-numeric: tabular-nums;}\n" +
            ".sk-score-context {font-size: 12px; color: var(--sk-text-muted); margin-bottom: 4px;}\n" +
            ".sk-subscores {list-style: none; margin: 0; padding: 0;}\n" +
            ".sk-subscore {display: grid; grid-template-columns: 128px 1fr 32px 36px; grid-template-rows: auto auto; align-items: center; " +
            "column-gap: 8px; padding: 5px 0; font-size: 13px;}\n" +
            ".sk-subscore-label {font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;}\n" +
            ".sk-subscore-bar {height: 6px; border-radius: 3px; background: var(--sk-surface-3); overflow: hidden;}\n" +
            ".sk-subscore-bar span {display: block; height: 100%; border-radius: 3px;}\n" +
            ".sk-subscore-good {background: var(--sk-risk-negligible, #1a9641);}\n" +
            ".sk-subscore-watch {background: var(--sk-risk-high, #fdae61);}\n" +
            ".sk-subscore-high {background: var(--sk-risk-very-high, #d7191c);}\n" +
            ".sk-subscore-value {text-align: right; font-variant-numeric: tabular-nums;}\n" +
            ".sk-subscore-drag {text-align: right; font-size: 12px; color: var(--sk-deleted); font-variant-numeric: tabular-nums;}\n" +
            ".sk-subscore-measure {grid-column: 1 / -1; font-size: 12px; color: var(--sk-text-muted);}\n" +
            ".sk-subscore-item {border-top: 1px solid var(--sk-border);}\n" +
            ".sk-subscore-details {padding: 0; margin: 0; border: none; background: none; border-radius: 0;}\n" +
            ".sk-subscore-details > summary {list-style: none; cursor: pointer; margin: 0; white-space: normal; overflow: visible;}\n" +
            ".sk-subscore-details > summary::-webkit-details-marker {display: none;}\n" +
            ".sk-subscore-details > summary .sk-subscore-label::before {content: '\\25B8'; display: inline-block; width: 12px; color: var(--sk-text-faint); transition: transform 0.15s;}\n" +
            ".sk-subscore-details[open] > summary .sk-subscore-label::before {transform: rotate(90deg);}\n" +
            ".sk-subscore-details > summary:hover .sk-subscore-label {color: var(--sk-link);}\n" +
            ".sk-subscore-why {font-size: 12px; line-height: 1.5; color: var(--sk-text-muted); padding: 0 0 8px 12px;}\n" +
            ".sk-subscore-why p {margin: 4px 0;}\n" +
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
        MaintainabilityScores scores = HealthSummary.shownScores(results);
        if (scores != null) {
            report.addHtmlContent(scoresCard(scores, SubScoreExplanations.of(results)));
        }
        List<HealthSummary.Hotspot> hotspots = summary.hotspots();
        if (!hotspots.isEmpty()) {
            boolean viewerLinks = results.getCodeConfiguration().getAnalysis().isSaveSourceFiles();
            report.addHtmlContent(hotspotsCard(hotspots, summary.hasHistory(), viewerLinks));
        }
        // Shown only when changed files exceed the large-file size: otherwise it would repeat the hotspots.
        List<FileReadsForChanges.ChangedFile> largeFiles = summary.largeChangingFiles();
        if (!largeFiles.isEmpty()) {
            boolean viewerLinks = results.getCodeConfiguration().getAnalysis().isSaveSourceFiles();
            report.addHtmlContent(largeFilesCard(summary.largeFileReads(), largeFiles, viewerLinks));
        }
    }

    static String tile(HealthSummary.Tile tile) {
        StringBuilder html = new StringBuilder();
        String element = tile.link == null ? "div" : "a";
        html.append("<").append(element).append(" class='sk-tile'");
        if (tile.link != null) {
            html.append(" href='").append(HtmlEscapeUtils.escape(tile.link)).append("'");
        }
        html.append(" title='").append(HtmlEscapeUtils.escape(tile.tooltip)).append("'>");
        // The label has the full line (one line, ellipsis as a last resort); the status sits next to the value.
        html.append("<div class='sk-tile-label' title='").append(HtmlEscapeUtils.escape(tile.label)).append("'>")
                .append(HtmlEscapeUtils.escape(tile.label)).append("</div>");
        html.append("<div class='sk-tile-value-row'><span class='sk-tile-value'>").append(HtmlEscapeUtils.escape(tile.value)).append("</span>");
        if (tile.grade != null) {
            html.append(gradeScale(tile.grade, false));
        } else if (tile.status != HealthSummary.Status.NEUTRAL) {
            html.append("<span class='sk-status sk-status-").append(tile.status.getLabel()).append("'>")
                    .append(tile.status.getLabel()).append("</span>");
        }
        html.append("</div>");
        html.append("<div class='sk-tile-caption'>").append(HtmlEscapeUtils.escape(tile.caption)).append("</div>");
        if (!tile.sparklineValues.isEmpty()) {
            html.append("<div class='sk-tile-spark'>").append(sparkline(tile.sparklineSlots, tile.sparklineValues)).append("</div>");
        }
        html.append("</").append(element).append(">");
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
        // Drawn in a fixed coordinate space and stretched to the tile's width (no fixed pixel width,
        // which overflowed narrow tiles).
        StringBuilder svg = new StringBuilder("<svg width='100%' height='" + height + "' viewBox='0 0 " + width + " " + height
                + "' preserveAspectRatio='none' role='img' aria-label='per month, past " + n + " months'>");
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

    static final String GRADES = "ABCDE";

    /** The A-E scale with the given grade highlighted; large for the score headers, small for cards. */
    public static String gradeScale(String grade, boolean large) {
        StringBuilder html = new StringBuilder("<span class='sk-grade-scale").append(large ? " sk-grade-lg" : "")
                .append("' role='img' aria-label='grade ").append(HtmlEscapeUtils.escape(grade))
                .append("' title='grade ").append(HtmlEscapeUtils.escape(grade)).append(" (A: 8+, B: 6.5+, C: 5+, D: 3.5+, E: below)'>");
        for (char letter : GRADES.toCharArray()) {
            String g = String.valueOf(letter);
            html.append("<span class='sk-grade sk-grade-").append(g.toLowerCase()).append(g.equals(grade) ? " sk-grade-on" : "")
                    .append("'>").append(g).append("</span>");
        }
        return html.append("</span>").toString();
    }

    static String scoresCard(MaintainabilityScores scores) {
        return scoresCard(scores, SubScoreExplanations.BUILT_IN);
    }

    static String scoresCard(MaintainabilityScores scores, Map<String, SubScoreExplanations.Why> explanations) {
        StringBuilder html = new StringBuilder("<div class='sk-hotspots-card'>");
        html.append("<div class='sk-hotspots-title'>Maintainability scores*</div>");
        if (scores.isCustomFramework()) {
            html.append("<div class='sk-hotspots-intro'>How easy the code is to understand and change, from 0 to 10, by this ")
                    .append("repository's own framework (<code>analysis.maintainabilityScores.customFramework</code>). The total is a ")
                    .append("weighted geometric mean of its sub-scores")
                    .append(scores.getHuman().getCapMargin() >= 0 ? ", capped at the weakest + "
                            + margin(scores.getHuman().getCapMargin()) : "")
                    .append(". Sub-scores are ordered by <i>drag</i>, how much the uncapped mean would rise if that sub-score were 10; ")
                    .append("click one to see why it matters. ")
                    .append("<span class='sk-score-note'>* Compare only repositories scored by the same framework.</span></div>");
        } else {
            html.append("<div class='sk-hotspots-intro'>How easy the code is to understand and change, from 0 to 10. Both scores weigh ")
                    .append("the same sub-scores: people struggle most with complex logic and knowledge held by few; agents pay for every ")
                    .append("line they read, copy duplicates and need tests to check their work. The total is a weighted geometric mean, ")
                    .append("capped at the weakest code sub-score + 4, so one weak spot is not averaged away. Sub-scores are ordered by ")
                    .append("<i>drag</i>, how much the uncapped mean would rise if that sub-score were 10; click one to see why it matters. ")
                    .append("<span class='sk-score-note'>* A heuristic: compare repositories rather than read it as absolute. ")
                    .append("Set weights, or your own framework, in <code>analysis.maintainabilityScores</code>.</span></div>");
        }
        html.append("<div class='sk-scores'>");
        html.append(scoreColumn("Human", scores.getHuman(), "", explanations, false));
        html.append(scoreColumn("AI", scores.getAi(), scores.getContextLinesPerChange() > 0
                ? String.format(Locale.US, "~%,d lines (~%,d tokens) read per change, over %,d changes in the past year",
                scores.getContextLinesPerChange(), scores.getContextLinesPerChange() * 10L, scores.getChangesMeasured())
                : "", explanations, true));
        html.append("</div></div>");
        return html.toString();
    }

    private static String whyParagraph(String audience, String text) {
        return text.isBlank() ? "" : "<p><b>" + audience + ":</b> " + HtmlEscapeUtils.escape(text) + "</p>";
    }

    private static String margin(double margin) {
        return margin == Math.rint(margin) ? String.valueOf((long) margin) : String.format(Locale.US, "%.1f", margin);
    }

    // aiFirst: the AI column puts why a sub-score matters for agents first.
    private static String scoreColumn(String label, MaintainabilityScore score, String note,
                                      Map<String, SubScoreExplanations.Why> explanations, boolean aiFirst) {
        StringBuilder html = new StringBuilder("<div class='sk-score-col'>");
        html.append("<div class='sk-score-head'><span class='sk-score-name'>").append(label).append("</span>")
                .append(String.format(Locale.US, "<span class='sk-score-total'>%.1f</span>", score.getValue()))
                .append(gradeScale(score.getGrade(), true)).append("</div>");
        if (!score.getCappedBy().isEmpty()) {
            html.append("<div class='sk-score-context'>Capped by ")
                    .append(HtmlEscapeUtils.escape(score.getCappedBy())).append(" + ").append(margin(score.getCapMargin())).append(".</div>");
        }
        if (!note.isEmpty()) {
            html.append("<div class='sk-score-context'>").append(HtmlEscapeUtils.escape(note)).append("</div>");
        }
        List<SubScore> subScores = new ArrayList<>(score.getSubScores());
        subScores.sort(Comparator.comparingDouble(SubScore::getDrag).reversed());
        html.append("<ul class='sk-subscores'>");
        for (SubScore subScore : subScores) {
            long width = Math.round(10 * subScore.getScore());
            SubScoreExplanations.Why why = explanations.get(subScore.getKey());
            boolean expandable = why != null && !why.isEmpty();
            html.append("<li class='sk-subscore-item'>");
            if (expandable) {
                html.append("<details class='sk-subscore-details'><summary>");
            }
            html.append("<div class='sk-subscore' title='weight ").append(String.format(Locale.US, "%.2f", subScore.getWeight()))
                    .append(expandable ? "; click for why it matters" : "").append("'>");
            html.append("<span class='sk-subscore-label'>").append(HtmlEscapeUtils.escape(subScore.getLabel())).append("</span>");
            html.append("<span class='sk-subscore-bar'><span class='sk-subscore-").append(HealthSummary.scoreStatus(subScore.getScore()).getLabel())
                    .append("' style='width: ").append(Math.max(2, width)).append("%'></span></span>");
            html.append(String.format(Locale.US, "<span class='sk-subscore-value'>%.1f</span>", subScore.getScore()));
            html.append("<span class='sk-subscore-drag'>").append(subScore.getDrag() > 0 ? String.format(Locale.US, "−%.1f", subScore.getDrag()) : "").append("</span>");
            html.append("<span class='sk-subscore-measure'>").append(HtmlEscapeUtils.escape(subScore.getMeasureText())).append("</span>");
            html.append("</div>");
            if (expandable) {
                html.append("</summary><div class='sk-subscore-why'>");
                String humanWhy = whyParagraph("For people", why.getHuman());
                String aiWhy = whyParagraph("For AI agents", why.getAi());
                html.append(aiFirst ? aiWhy + humanWhy : humanWhy + aiWhy);
                html.append("</div></details>");
            }
            html.append("</li>");
        }
        html.append("</ul></div>");
        return html.toString();
    }

    static String hotspotsCard(List<HealthSummary.Hotspot> hotspots, boolean history, boolean viewerLinks) {
        StringBuilder html = new StringBuilder("<div class='sk-hotspots-card'>");
        html.append("<div class='sk-hotspots-title'>Where to look first</div>");
        html.append("<div class='sk-hotspots-intro'>");
        if (history) {
            html.append("Complex main files changed often in the past year, where changes are frequent, slow and error-prone. ")
                    .append("Ranked by complexity × days changed; a single contributor flags knowledge risk.");
        } else {
            html.append("The most complex main files (no git history, so no change frequency).");
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

    static String largeFilesCard(FileReadsForChanges reads, List<FileReadsForChanges.ChangedFile> files, boolean viewerLinks) {
        StringBuilder html = new StringBuilder("<div class='sk-hotspots-card'>");
        html.append("<div class='sk-hotspots-title'>Large files that change often</div>");
        html.append("<div class='sk-hotspots-intro'>Main files over ").append(String.format(Locale.US, "%,d", reads.getLargeFileLines()))
                .append(" lines changed ").append(HtmlEscapeUtils.escape(reads.windowLabel()))
                .append(". People and AI agents read them in pieces for every change; splitting them cuts that. ")
                .append("Ranked by lines × changes.</div><ol class='sk-hotspots'>");
        long max = Math.max(1, files.get(0).getReadLines());
        for (int i = 0; i < files.size(); i++) {
            FileReadsForChanges.ChangedFile file = files.get(i);
            StringBuilder row = new StringBuilder("<li class='sk-hotspot'>");
            row.append("<span class='sk-hotspot-rank'>").append(i + 1).append("</span>");
            row.append(fileCell(file.getFile(), viewerLinks));
            row.append("<div class='sk-hotspot-signals'>");
            row.append(chip(String.format(Locale.US, "%,d LOC", file.getLinesOfCode()), true));
            row.append(chip(file.getChanges() + (file.getChanges() == 1 ? " change" : " changes") + " / " + reads.windowShortLabel(), false));
            row.append(chip(reads.tokenRange(file.getLinesOfCode()) + " tokens to read", false));
            row.append("</div>");
            long percent = Math.max(2, Math.round(100.0 * file.getReadLines() / max));
            row.append("<div class='sk-hotspot-score' title='lines × changes: ").append(file.getReadLines()).append("'><span style='width: ")
                    .append(percent).append("%'></span></div>");
            row.append("</li>");
            html.append(row);
        }
        html.append("</ol>");
        html.append("<a class='sk-hotspots-more' href='FileSize.html'>Large files that change often in the File Size report →</a>");
        html.append("</div>");
        return html.toString();
    }

    // The file name (a source viewer link when the viewer caches sources) above its folder.
    private static String fileCell(SourceFile file, boolean viewerLinks) {
        String path = file.getRelativePath();
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        String folder = slash >= 0 ? path.substring(0, slash + 1) : "";
        StringBuilder html = new StringBuilder();
        html.append("<div class='sk-hotspot-file' title='").append(HtmlEscapeUtils.escape(path)).append("'>");
        if (viewerLinks) {
            html.append("<a class='sk-hotspot-name' target='_blank' href='").append(HtmlEscapeUtils.viewerFileHref("main", path)).append("'>")
                    .append(HtmlEscapeUtils.escape(name)).append("</a>");
        } else {
            html.append("<span class='sk-hotspot-name'>").append(HtmlEscapeUtils.escape(name)).append("</span>");
        }
        html.append("<div class='sk-hotspot-path'>").append(HtmlEscapeUtils.escape(folder)).append("</div></div>");
        return html.toString();
    }

    static String hotspot(int rank, HealthSummary.Hotspot hotspot, long maxScore, boolean history, boolean viewerLinks) {
        SourceFile file = hotspot.file;

        StringBuilder html = new StringBuilder("<li class='sk-hotspot'>");
        html.append("<span class='sk-hotspot-rank'>").append(rank).append("</span>");
        html.append(fileCell(file, viewerLinks));

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
