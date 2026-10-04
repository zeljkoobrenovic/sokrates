/*
 * Copyright (c) 2026 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.reports.core.AgentContextSummary;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.FilesReportUtils;
import nl.obren.sokrates.reports.utils.HtmlEscapeUtils;
import nl.obren.sokrates.reports.utils.RiskDistributionStatsReportUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.FileHistoryAnalysisConfig;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.LogicalDecompositionAnalysisResults;
import nl.obren.sokrates.sourcecode.githistory.CoAuthor;
import nl.obren.sokrates.sourcecode.githistory.GitHistoryUtils;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent Context Cost: rules of thumb for how much an AI coding agent has to read to maintain the main code,
 * from file sizes and the git history only ({@link AgentContextSummary}). Laid out like the other risk
 * reports: the five size bands map onto the five risk colours, and the overall, per-component and commit
 * profiles are stacked bars. Every number is a band, a share or a rounded range; the about section says
 * which rules of thumb the page rests on and what it leaves out.
 */
public class AgentContextReportGenerator {
    // Bars are compared as shares, so both bars of an overall chart are scaled to the same total.
    private static final int SHARE_SCALE = 100_000;

    private final CodeAnalysisResults codeAnalysisResults;
    private final File sokratesConfigFolder;

    public AgentContextReportGenerator(CodeAnalysisResults codeAnalysisResults, File sokratesConfigFolder) {
        this.codeAnalysisResults = codeAnalysisResults;
        this.sokratesConfigFolder = sokratesConfigFolder;
    }

    public void addAgentContextToReport(RichTextReport report) {
        report.setDescription("How much an AI coding agent has to read to change this code: rules of thumb from file sizes and the git history.");
        AgentContextSummary summary = AgentContextSummary.withAiCommits(codeAnalysisResults, aiCommitIds());
        if (summary.isEmpty()) {
            report.startSection("Agent Context Cost Overall", "");
            report.addParagraph("No main code file was changed " + summary.windowLabel()
                    + ", so there are no changes to estimate the reading for.");
            report.endSection();
        } else {
            addOverall(report, summary);
            addPerLogicalDecomposition(report, summary);
            addCommits(report, summary);
            addHotspots(report, summary);
            addSplitCandidates(report, summary);
        }
        addAboutSection(report, summary);
    }

    private Set<String> aiCommitIds() {
        if (sokratesConfigFolder == null) {
            return Collections.emptySet();
        }
        FileHistoryAnalysisConfig historyConfig = codeAnalysisResults.getCodeConfiguration().getFileHistoryAnalysis();
        Map<String, List<CoAuthor>> coAuthors = GitHistoryUtils.getCoAuthorsBySha(historyConfig.getFilesHistoryFile(sokratesConfigFolder), historyConfig);
        return coAuthors.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(CoAuthor::isAi))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private void addOverall(RichTextReport report, AgentContextSummary summary) {
        report.startSection("Agent Context Cost Overall",
                "An agent reads the files it changes: the size of the files that change sets how much it reads.");
        report.addParagraph("An AI coding agent pays mostly for what it <b>reads</b>, not for what it writes. "
                + "Agents read a file in windows of about " + formatLines(AgentContextSummary.READ_WINDOW_LINES)
                + " lines; a larger file is read in pieces and re-read. The bars compare how the code is spread over five size bands "
                + "with where the changes " + summary.windowLabel() + " landed: the more changes in the red and orange bands, "
                + "the more an agent reads per change.");
        List<AgentContextSummary.Band> bands = summary.getFileBands();
        long totalChanges = summary.getTotalFileChanges();
        long changesInLargeFiles = bands.stream().filter(b -> b.getMinLines() > AgentContextSummary.READ_WINDOW_LINES)
                .mapToLong(AgentContextSummary.Band::getChanges).sum();
        report.startUnorderedList();
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(summary.getCommitsCount()) + " commits " + summary.windowLabel()
                + " made " + RichTextRenderingUtils.renderNumberStrong((int) totalChanges) + " changes to main code files; <b>"
                + AgentContextSummary.percentage(changesInLargeFiles, totalChanges) + "</b> of these changes were in files larger than one read window ("
                + formatLines(AgentContextSummary.READ_WINDOW_LINES) + " lines).");
        report.startUnorderedList();
        for (int i = bands.size() - 1; i >= 0; i--) {
            AgentContextSummary.Band band = bands.get(i);
            report.addListItem(RichTextRenderingUtils.renderNumberStrong(band.getCount()) + " " + band.getLabel() + " files ("
                    + linesRange(band) + " lines): " + AgentContextSummary.percentage(band.getLinesOfCode(), summary.getTotalLinesOfCode())
                    + " of the code, " + AgentContextSummary.percentage(band.getChanges(), totalChanges) + " of the changes");
        }
        report.endUnorderedList();
        report.endUnorderedList();

        List<RiskDistributionStats> bars = new ArrayList<>();
        bars.add(stats("lines of code", bands.stream().mapToLong(AgentContextSummary.Band::getLinesOfCode).toArray(), true));
        bars.add(stats("file changes " + summary.windowLabel().replace("in the ", ""),
                bands.stream().mapToLong(AgentContextSummary.Band::getChanges).toArray(), true));
        report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChartInOrder(bars, fileBandLabels(summary)));
        report.endSection();
    }

    private void addPerLogicalDecomposition(RichTextReport report, AgentContextSummary summary) {
        List<LogicalDecompositionAnalysisResults> decompositions = codeAnalysisResults.getLogicalDecompositionsAnalysisResults();
        if (decompositions.isEmpty()) {
            return;
        }
        report.startSection("File Changes per Logical Decomposition",
                "Where the changes " + summary.windowLabel() + " landed, coloured by the size band of the changed file.");
        Map<String, Integer> changesPerFile = summary.changesPerFile();
        decompositions.forEach(decomposition -> {
            List<RiskDistributionStats> bars = new ArrayList<>();
            for (AspectAnalysisResults component : decomposition.getComponents()) {
                if (component.getAspect() == null) {
                    continue;
                }
                long[] changes = new long[summary.getFileBands().size()];
                for (SourceFile file : component.getAspect().getSourceFiles()) {
                    changes[summary.fileBandIndex(file.getLinesOfCode())] += changesPerFile.getOrDefault(file.getRelativePath(), 0);
                }
                if (Arrays.stream(changes).sum() > 0) {
                    bars.add(stats(component.getName(), changes, false));
                }
            }
            report.startSubSectionText(decomposition.getKey(), "");
            if (bars.isEmpty()) {
                report.addParagraph("No component of this decomposition was changed " + summary.windowLabel() + ".");
            } else {
                report.startScrollingDiv();
                report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(bars, fileBandLabels(summary)));
                report.endDiv();
            }
            report.endSection();
        });
        report.endSection();
    }

    private void addCommits(RichTextReport report, AgentContextSummary summary) {
        report.startSection("Minimum Reading per Commit",
                "Every past commit shows the least an agent would have read to make it: the files it touched.");
        report.addParagraph("For each commit " + summary.windowLabel() + ", the lines of code of the main files it touched (their current size). "
                + "This is a lower bound: agents also read code they do not change.");
        boolean ai = summary.getAiCommitsCount() > 0;
        report.startUnorderedList();
        report.addListItem("The median commit touched files of " + AgentContextSummary.roundedLines(summary.getMedianCommitLines())
                + " lines (<b>" + AgentContextSummary.tokenRange(summary.getMedianCommitLines()) + " tokens</b>); one in ten touched more than "
                + AgentContextSummary.roundedLines(summary.getP90CommitLines()) + " lines (<b>"
                + AgentContextSummary.tokenRange(summary.getP90CommitLines()) + " tokens</b>).");
        if (ai) {
            report.addListItem(RichTextRenderingUtils.renderNumberStrong(summary.getAiCommitsCount()) + " of "
                    + RichTextRenderingUtils.renderNumberStrong(summary.getCommitsCount())
                    + " commits have an AI agent as co-author; their median commit touched files of "
                    + AgentContextSummary.roundedLines(summary.getMedianAiCommitLines()) + " lines.");
        }
        if (summary.getIgnoredLargeCommitsCount() > 0) {
            report.addListItem(RichTextRenderingUtils.renderNumberStrong(summary.getIgnoredLargeCommitsCount())
                    + " commits touching more than " + AgentContextSummary.MAX_FILES_PER_COMMIT
                    + " main files (mass renames, reformats, imports) are left out on this page.");
        }
        report.endUnorderedList();
        List<AgentContextSummary.Band> bands = summary.getCommitBands();
        List<RiskDistributionStats> bars = new ArrayList<>();
        bars.add(stats("all commits", bands.stream().mapToLong(AgentContextSummary.Band::getCount).toArray(), true));
        if (ai) {
            bars.add(stats("AI co-authored commits", bands.stream().mapToLong(AgentContextSummary.Band::getAiCount).toArray(), true));
        }
        List<String> labels = new ArrayList<>();
        for (int i = bands.size() - 1; i >= 0; i--) {
            AgentContextSummary.Band band = bands.get(i);
            labels.add(linesRange(band) + " lines (" + tokensOfBand(band) + " tokens)");
        }
        report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChartInOrder(bars, labels));
        report.endSection();
    }

    private void addHotspots(RichTextReport report, AgentContextSummary summary) {
        report.startSection("Where Agents Read Most",
                "Files that change together are read together; reading concentrates where large working sets meet frequent change.");
        List<AgentContextSummary.FileContext> changed = summary.getChangedFiles();
        List<AgentContextSummary.FileContext> withPartners = changed.stream().filter(c -> !c.getPartners().isEmpty()).collect(Collectors.toList());
        report.addParagraph("A file's <b>working set</b> is the file plus its <b>co-change partners</b>: files changed together with it in at least "
                + Math.round(AgentContextSummary.MIN_PARTNER_SHARE * 100) + "% of its commits " + summary.windowLabel()
                + " (and at least " + AgentContextSummary.MIN_SHARED_COMMITS + " times), as in the "
                + "<a href='FileTemporalDependencies.html'>Temporal Dependencies</a> report. The files below have the largest "
                + "changes × working set: there, a smaller working set saves most.");
        report.startUnorderedList();
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(withPartners.size()) + " of "
                + RichTextRenderingUtils.renderNumberStrong(changed.size()) + " changed main files ("
                + AgentContextSummary.percentage(withPartners.size(), changed.size()) + ") have at least one co-change partner.");
        if (!withPartners.isEmpty()) {
            long fileLines = withPartners.stream().mapToLong(AgentContextSummary.FileContext::getLinesOfCode).sum();
            long workingSetLines = withPartners.stream().mapToLong(AgentContextSummary.FileContext::getWorkingSetLines).sum();
            report.addListItem("For those files, the working set is " + multiple(workingSetLines, fileLines) + " the size of the file alone.");
        }
        report.endUnorderedList();

        boolean linkToFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isSaveSourceFiles();
        StringBuilder table = new StringBuilder();
        // Long folder paths wrap (the shared file cell keeps them on one line), so the numeric columns stay in view.
        table.append("<style>.sk-agent-table td:first-child > div { display: flex; }"
                + " .sk-agent-table td:first-child > div > div:last-child { min-width: 0; }"
                + " .sk-agent-table td:first-child > div > div:last-child div { white-space: normal !important; word-break: break-all; }</style>\n");
        table.append("<div style='width: 100%; overflow-x: auto;'>\n");
        table.append("<table class='sk-data-table sk-agent-table' style='width: 100%'>\n");
        table.append("<tr><th>File</th><th># lines</th><th># changes</th><th>co-change partners</th>"
                + "<th>working set<br>(lines)</th><th>≈ tokens<br>per change</th><th>split<br>candidate</th></tr>\n");
        summary.hotspots().forEach(context -> {
            table.append("<tr>\n");
            table.append(FilesReportUtils.fileNameCell(context.getFile(), linkToFiles));
            table.append("<td style='text-align: center'>" + context.getLinesOfCode() + "</td>\n");
            table.append("<td style='text-align: center'>" + context.getChanges() + "</td>\n");
            table.append("<td style='font-size: 12px; max-width: 240px'>" + partners(context) + "</td>\n");
            table.append("<td style='text-align: center' data-sort='" + context.getWorkingSetLines() + "'>"
                    + AgentContextSummary.roundedLines(context.getWorkingSetLines()) + "</td>\n");
            table.append("<td style='text-align: center; white-space: nowrap' data-sort='" + context.getWorkingSetLines() + "'>"
                    + AgentContextSummary.tokenRange(context.getWorkingSetLines()) + "</td>\n");
            table.append("<td style='text-align: center'>" + (context.isSplitCandidate() ? "<b>yes</b>" : "") + "</td>\n");
            table.append("</tr>\n");
        });
        table.append("</table>\n</div>\n");
        report.addHtmlContent(table.toString());
        report.endSection();
    }

    private static String partners(AgentContextSummary.FileContext context) {
        if (context.getPartners().isEmpty()) {
            return "<span style='color: var(--sk-text-faint, #999)'>none</span>";
        }
        List<AgentContextSummary.Partner> shown = context.getPartners().subList(0, Math.min(3, context.getPartners().size()));
        String html = shown.stream().map(partner -> {
            SourceFile file = partner.getFile();
            return "<div title='" + HtmlEscapeUtils.escape(file.getRelativePath()) + "'>"
                    + "<span style='word-break: break-all'>" + HtmlEscapeUtils.escape(new File(file.getRelativePath()).getName()) + "</span>"
                    + " <span style='color: var(--sk-text-muted, #666); white-space: nowrap'>" + Math.round(partner.getShare() * 100) + "%</span></div>";
        }).collect(Collectors.joining());
        int more = context.getPartners().size() - shown.size();
        return html + (more > 0 ? "<div style='color: var(--sk-text-muted, #666)'>+ " + more + " more</div>" : "");
    }

    private static String multiple(long whole, long part) {
        if (part <= 0) {
            return "a multiple of";
        }
        double ratio = (double) whole / part;
        if (ratio < 1.25) {
            return "about";
        } else if (ratio < 1.75) {
            return "about one and a half times";
        } else if (ratio < 2.5) {
            return "about twice";
        } else if (ratio < 4) {
            return "about three times";
        } else if (ratio < 7) {
            return "about five times";
        }
        return "more than seven times";
    }

    private void addSplitCandidates(RichTextReport report, AgentContextSummary summary) {
        report.startSection("Splitting Large Files",
                "A split pays back after a number of changes, not on day one: count the changes.");
        List<AgentContextSummary.FileContext> candidates = summary.splitCandidates();
        if (candidates.isEmpty()) {
            report.addParagraph("No main file is both larger than one read window (" + formatLines(AgentContextSummary.READ_WINDOW_LINES)
                    + " lines) and changed at least " + AgentContextSummary.MIN_CHANGES_TO_SPLIT + " times " + summary.windowLabel()
                    + ": there is no file to split for the agents' sake.");
        } else {
            report.addParagraph("Large files that change often. A split is assumed to cut what an agent reads per change to one read window ("
                    + formatLines(AgentContextSummary.READ_WINDOW_LINES) + " lines) and to cost up to about 5 million tokens, "
                    + "as in the one published experiment; the payback is an order of magnitude, not a forecast.");
            report.startUnorderedList();
            candidates.stream().limit(AgentContextSummary.MAX_HOTSPOTS).forEach(context -> {
                long saved = context.getSplitSavingLines();
                String pace = summary.paybackPace(context);
                report.addListItem("<b>" + HtmlEscapeUtils.escape(new File(context.getFile().getRelativePath()).getName()) + "</b> "
                        + "<span style='color: var(--sk-text-muted, #666)'>(" + HtmlEscapeUtils.escape(context.getFile().getRelativePath()) + ")</span>: "
                        + AgentContextSummary.roundedLines(saved) + " lines over one read window, changed "
                        + context.getChanges() + " times; a split saves " + AgentContextSummary.tokenRange(saved)
                        + " tokens per change and pays back after <b>" + AgentContextSummary.paybackMagnitude(saved) + "</b>"
                        + (pace.isEmpty() ? "" : ", " + pace) + ".");
            });
            report.endUnorderedList();
        }
        report.endSection();
    }

    private void addAboutSection(RichTextReport report, AgentContextSummary summary) {
        report.startSection("About This Analysis", "");
        report.addParagraph("The numbers on this page are <b>rules of thumb, not measurements</b>: bands, shares and rounded ranges. "
                + "The same task can cost an agent 2 to 30 times more or less from run to run, so use the page to see <i>where</i> "
                + "reading concentrates and to rank candidates, not as a budget.");
        report.addLevel3Header("The rules of thumb");
        report.startUnorderedList();
        report.addListItem("<b>An agent reads what it changes.</b> Agents read files in windows (Claude Code about 2,000 lines per read, Cursor 250); "
                + "larger files are read in pieces and re-read. When a 17,000-line file was split into modules, the input tokens of the same change "
                + "fell by 83%, but only once the largest file shrank. <i>Evidence: strong mechanism, one controlled experiment.</i>");
        report.addListItem("<b>A commit shows the minimum reading.</b> A change needs the code it changes; in benchmarks, success drops and cost "
                + "rises sharply with the number of files a change touches. <i>Evidence: strong, as a lower bound.</i>");
        report.addListItem("<b>Changes travel together.</b> Agents succeed when the facts a change depends on are at hand and wander when they are not; "
                + "the git history shows which files a change of a file has needed so far. <i>Evidence: moderate.</i>");
        report.addListItem("<b>Start where large working sets meet frequent change.</b> Cleaner code in general saves little (about 7% of input tokens "
                + "in a controlled study); the big savings come from the extremes. <i>Evidence: moderate.</i>");
        report.addListItem("<b>Payback is counted in changes.</b> In the one published experiment, a refactoring cost at most about 5 million tokens "
                + "and saved about 130 thousand input tokens per change: paid back after a few dozen changes. <i>Evidence: single study.</i>");
        report.endUnorderedList();
        report.addLevel3Header("How the numbers are made");
        report.startUnorderedList();
        report.addListItem("<b>Inputs:</b> the lines of code of the main files (as they are now) and the main files each commit touched "
                + summary.windowLabel() + " (the window of <code>analysis.maxTemporalDependenciesDepthDays</code>). Commits touching more than "
                + AgentContextSummary.MAX_FILES_PER_COMMIT + " main files are left out.");
        report.addListItem("<b>Not used:</b> static dependencies (found with regular expressions, too rough for this), "
                + "unit complexity and duplication (no study links them to agent cost yet). Test code is not counted either.");
        report.addListItem("<b>Tokens</b> are lines × " + AgentContextSummary.TOKENS_PER_LINE_LOW + " to " + AgentContextSummary.TOKENS_PER_LINE_HIGH
                + " (about 4 characters per token, 30 to 50 characters per line; newer tokenizers count more), the low end rounded down "
                + "and the high end rounded up. Prompt caching makes re-reads within a session cheap (about a tenth of the price), "
                + "so the first read of each file dominates; no money amounts are shown, as prices change quickly.");
        report.addListItem("<b>Size bands:</b> up to " + formatLines(AgentContextSummary.COMFORTABLE_LINES) + " lines (a threshold above which "
                + "a team routed reads to a cheaper model), " + formatLines(AgentContextSummary.SHORT_READ_LINES) + " lines (the start of "
                + "Sokrates' very long files), " + formatLines(AgentContextSummary.READ_WINDOW_LINES) + " lines (Claude Code's default read window) and "
                + formatLines(AgentContextSummary.MONOLITH_LINES) + " lines.");
        report.addListItem("<b>Caveats:</b> agents differ, and refactoring a complex file into many small ones can also <i>increase</i> reading.");
        report.endUnorderedList();
        report.startDetailsBlock("Sources...");
        report.startUnorderedList();
        report.addListItem("<a target='_blank' href='https://martinfowler.com/articles/exploring-gen-ai/refactoring-economic-benefit.html'>The Economic Benefit of Refactoring</a>, G. Edwards-Alexander, martinfowler.com, 2026");
        report.addListItem("<a target='_blank' href='https://arxiv.org/abs/2605.20049'>Clean vs. messy repositories, minimal pairs with Claude Code</a>, SonarSource, 2026");
        report.addListItem("<a target='_blank' href='https://arxiv.org/abs/2608.16630'>The Working Set of a Coding Agent</a>, 2026");
        report.addListItem("<a target='_blank' href='https://arxiv.org/abs/2509.16941'>SWE-bench Pro</a>, Scale AI, 2025");
        report.addListItem("<a target='_blank' href='https://arxiv.org/abs/2405.15793'>SWE-agent: Agent-Computer Interfaces</a>, NeurIPS 2024");
        report.addListItem("<a target='_blank' href='https://arxiv.org/abs/2604.22750'>How Do AI Agents Spend Your Money?</a>, 2026");
        report.addListItem("<a target='_blank' href='https://arxiv.org/abs/2601.02200'>Code for Machines, Not Just Humans</a>, CodeScene, 2026");
        report.addListItem("<a target='_blank' href='https://www.trychroma.com/research/context-rot'>Context Rot</a>, Chroma, 2025");
        report.addListItem("<a target='_blank' href='https://code.claude.com/docs/en/costs'>Manage costs effectively</a>, Claude Code documentation");
        report.endUnorderedList();
        report.endDetailsBlock();
        report.endSection();
    }

    /** A stacked-bar distribution from per-band values, lowest band first (negligible .. very high). */
    private static RiskDistributionStats stats(String key, long[] valuesLowestFirst, boolean asShares) {
        long total = Arrays.stream(valuesLowestFirst).sum();
        int[] values = new int[5];
        for (int i = 0; i < Math.min(5, valuesLowestFirst.length); i++) {
            values[i] = asShares
                    ? (total > 0 ? (int) Math.round((double) SHARE_SCALE * valuesLowestFirst[i] / total) : 0)
                    : (int) Math.min(Integer.MAX_VALUE, valuesLowestFirst[i]);
        }
        RiskDistributionStats stats = new RiskDistributionStats(key);
        stats.setNegligibleRiskValue(values[0]);
        stats.setLowRiskValue(values[1]);
        stats.setMediumRiskValue(values[2]);
        stats.setHighRiskValue(values[3]);
        stats.setVeryHighRiskValue(values[4]);
        return stats;
    }

    /** The legend labels of the file bands, highest first (the order of the risk legend). */
    private static List<String> fileBandLabels(AgentContextSummary summary) {
        List<String> labels = new ArrayList<>();
        List<AgentContextSummary.Band> bands = summary.getFileBands();
        for (int i = bands.size() - 1; i >= 0; i--) {
            labels.add(bands.get(i).getLabel() + " (" + linesRange(bands.get(i)) + ")");
        }
        return labels;
    }

    private static String linesRange(AgentContextSummary.Band band) {
        if (band.getMaxLines() < 0) {
            return formatLines(band.getMinLines()) + "+";
        }
        return formatLines(Math.max(1, band.getMinLines())) + "-" + formatLines(band.getMaxLines());
    }

    private static String tokensOfBand(AgentContextSummary.Band band) {
        if (band.getMaxLines() < 0) {
            return "over " + AgentContextSummary.tokensLow(band.getMinLines() - 1);
        }
        return "up to " + AgentContextSummary.tokensHigh(band.getMaxLines());
    }

    private static String formatLines(long lines) {
        return String.format(Locale.US, "%,d", lines);
    }
}
