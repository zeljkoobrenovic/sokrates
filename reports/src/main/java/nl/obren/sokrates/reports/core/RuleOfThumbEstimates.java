/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.reports.utils.HtmlEscapeUtils;
import nl.obren.sokrates.reports.utils.HtmlTemplateUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.core.FileReadsForChangesConfig;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The collapsed "Rule-of-thumb estimates" block at the bottom of the At a Glance tab (repository and landscape):
 * an in-page calculator (rebuild value, maintenance effort, AI token reads) over the lines of code. Only numbers
 * go into the template; the assumptions are inputs the viewer can change. A landscape also passes its totals per
 * repository activity window, which the page offers as a "repositories" choice.
 */
public class RuleOfThumbEstimates {
    static final String TEMPLATE = "/templates/rule-of-thumb-estimates.html";
    /** The default landscape choice: repositories with a commit in the past year. */
    public static final String DEFAULT_WINDOW = "365";
    /** The churn periods (days) the write-token estimate offers. */
    static final int[] CHURN_PERIODS = {30, 90, 365};

    /** Lines of code of the repositories in one activity window. */
    public static class Window {
        private final String id;
        private final String label;
        private final int days;
        private int repositories;
        private long mainLoc;
        private long testLoc;
        private long buildLoc;
        private long generatedLoc;
        private long otherLoc;
        private final long[][] churn = new long[CHURN_PERIODS.length][SCOPES.length];

        Window(String id, String label, int days) {
            this.id = id;
            this.label = label;
            this.days = days;
        }

        /** Whether a repository whose latest commit is on that date belongs to this window (all time: always). */
        boolean includes(String latestCommitDate) {
            return days <= 0 || (StringUtils.isNotBlank(latestCommitDate) && DateUtils.isDateWithinRange(latestCommitDate, days));
        }

        void add(CodeAnalysisResults results) {
            repositories++;
            mainLoc += Math.max(0, results.getMainAspectAnalysisResults().getLinesOfCode());
            testLoc += Math.max(0, results.getTestAspectAnalysisResults().getLinesOfCode());
            buildLoc += Math.max(0, results.getBuildAndDeployAspectAnalysisResults().getLinesOfCode());
            generatedLoc += Math.max(0, results.getGeneratedAspectAnalysisResults().getLinesOfCode());
            otherLoc += Math.max(0, results.getOtherAspectAnalysisResults().getLinesOfCode());
            long[][] repositoryChurn = churn(results.getContributorsAnalysisResults());
            for (int p = 0; p < CHURN_PERIODS.length; p++) {
                for (int s = 0; s < SCOPES.length; s++) {
                    churn[p][s] += repositoryChurn[p][s];
                }
            }
        }

        public long[][] getChurn() {
            return churn;
        }

        public String getId() {
            return id;
        }

        public int getRepositories() {
            return repositories;
        }

        public long getMainLoc() {
            return mainLoc;
        }

        String toJson() {
            return "{\"id\":\"" + id + "\",\"label\":\"" + label + "\",\"repositories\":" + repositories
                    + ",\"main\":" + mainLoc + ",\"test\":" + testLoc + ",\"build\":" + buildLoc
                    + ",\"generated\":" + generatedLoc + ",\"other\":" + otherLoc + ",\"churn\":" + churnJson(churn) + "}";
        }
    }

    public static void add(RichTextReport report, CodeAnalysisResults results) {
        FileReadsForChangesConfig reads = results.getCodeConfiguration().getAnalysis().getFileReadsForChanges();
        report.addHtmlContent(html(new long[]{results.getMainAspectAnalysisResults().getLinesOfCode(),
                        results.getTestAspectAnalysisResults().getLinesOfCode(),
                        results.getBuildAndDeployAspectAnalysisResults().getLinesOfCode(),
                        results.getGeneratedAspectAnalysisResults().getLinesOfCode(),
                        results.getOtherAspectAnalysisResults().getLinesOfCode()},
                churn(results.getContributorsAnalysisResults()),
                reads.getTokensPerLineMin(), reads.getTokensPerLineMax(), null));
    }

    /** The landscape version: totals over the given repositories, per activity window (default: the past year). */
    public static void addForLandscape(RichTextReport report, List<CodeAnalysisResults> repositories) {
        List<Window> windows = windows(repositories);
        Window selected = windows.stream().filter(w -> w.id.equals(DEFAULT_WINDOW)).findFirst().orElse(windows.get(windows.size() - 1));
        FileReadsForChangesConfig reads = new FileReadsForChangesConfig();
        report.addHtmlContent(html(new long[]{selected.mainLoc, selected.testLoc, selected.buildLoc, selected.generatedLoc, selected.otherLoc},
                selected.churn, reads.getTokensPerLineMin(), reads.getTokensPerLineMax(), windows));
    }

    /** The activity windows (by the repository's latest commit date), the last one all repositories. */
    public static List<Window> windows(List<CodeAnalysisResults> repositories) {
        List<Window> windows = new ArrayList<>();
        windows.add(new Window("30", "repositories active in the past 30 days", 30));
        windows.add(new Window("90", "repositories active in the past 3 months", 90));
        windows.add(new Window("180", "repositories active in the past 6 months", 180));
        windows.add(new Window(DEFAULT_WINDOW, "repositories active in the past year", 365));
        windows.add(new Window("730", "repositories active in the past 2 years", 730));
        windows.add(new Window("all", "all repositories", 0));
        repositories.forEach(results -> {
            String latest = results.getContributorsAnalysisResults().getLatestCommitDate();
            windows.stream().filter(w -> w.includes(latest)).forEach(w -> w.add(results));
        });
        return windows;
    }

    /** Lines of code per scope, in the order {@link #SCOPES}. */
    static final String[] SCOPES = {"main", "test", "build", "generated", "other"};

    /** Lines added + deleted per {@link #CHURN_PERIODS} period and {@link #SCOPES} scope, from the per-scope day slots. */
    public static long[][] churn(ContributorsAnalysisResults contributors) {
        long[][] churn = new long[CHURN_PERIODS.length][SCOPES.length];
        if (contributors == null || contributors.getContributorsPerDayByScope() == null) {
            return churn;
        }
        for (int s = 0; s < SCOPES.length; s++) {
            List<ContributionTimeSlot> perDay = contributors.getContributorsPerDayByScope().get(SCOPES[s]);
            if (perDay == null) {
                continue;
            }
            for (ContributionTimeSlot slot : perDay) {
                for (int p = 0; p < CHURN_PERIODS.length; p++) {
                    if (DateUtils.isCommittedLessThanDaysAgo(slot.getTimeSlot(), CHURN_PERIODS[p])) {
                        churn[p][s] += Math.max(0, slot.getLinesAdded()) + Math.max(0, slot.getLinesDeleted());
                    }
                }
            }
        }
        return churn;
    }

    static String churnJson(long[][] churn) {
        StringBuilder json = new StringBuilder("{");
        for (int p = 0; p < CHURN_PERIODS.length; p++) {
            json.append(p > 0 ? "," : "").append("\"").append(CHURN_PERIODS[p]).append("\":[");
            for (int s = 0; s < SCOPES.length; s++) {
                json.append(s > 0 ? "," : "").append(churn != null && p < churn.length && s < churn[p].length ? churn[p][s] : 0);
            }
            json.append("]");
        }
        return json.append("}").toString();
    }

    static String html(long[] scopeLoc, int tokensPerLineMin, int tokensPerLineMax, List<Window> windows) {
        return html(scopeLoc, null, tokensPerLineMin, tokensPerLineMax, windows);
    }

    static String html(long[] scopeLoc, long[][] churn, int tokensPerLineMin, int tokensPerLineMax, List<Window> windows) {
        int tokensMin = Math.max(1, Math.min(tokensPerLineMin, tokensPerLineMax));
        int tokensMax = Math.max(tokensMin, Math.max(tokensPerLineMin, tokensPerLineMax));
        String windowsJson = "";
        if (windows != null && !windows.isEmpty()) {
            StringBuilder json = new StringBuilder("[");
            windows.forEach(w -> json.append(json.length() > 1 ? "," : "").append(w.toJson()));
            windowsJson = json.append("]").toString();
        }
        String html = HtmlTemplateUtils.getResource(TEMPLATE);
        for (int i = 0; i < SCOPES.length; i++) {
            html = html.replace("${" + SCOPES[i] + "Loc}", String.valueOf(i < scopeLoc.length ? Math.max(0, scopeLoc[i]) : 0));
        }
        return html
                .replace("${tokensMin}", String.valueOf(tokensMin))
                .replace("${tokensMax}", String.valueOf(tokensMax))
                .replace("${churn}", HtmlEscapeUtils.escape(churnJson(churn)))
                .replace("${windows}", HtmlEscapeUtils.escape(windowsJson))
                .replace("${defaultWindow}", DEFAULT_WINDOW);
    }
}
