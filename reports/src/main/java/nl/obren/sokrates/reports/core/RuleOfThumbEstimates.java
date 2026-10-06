/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.reports.utils.HtmlEscapeUtils;
import nl.obren.sokrates.reports.utils.HtmlTemplateUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The collapsed "Rule-of-thumb estimates" block at the bottom of the At a Glance tab (repository and landscape):
 * an in-page calculator (rebuild value, maintenance effort) over the lines of main code. Only numbers go into the
 * template; the assumptions are inputs the viewer can change, starting from the configured ones when
 * {@code analysis.estimateAssumptions} is enabled. A landscape also passes its totals per repository
 * activity window, which the page offers as a "repositories" choice. (AI token costs have their own page, the
 * AI Cost Estimator.)
 */
public class RuleOfThumbEstimates {
    static final String TEMPLATE = "/templates/rule-of-thumb-estimates.html";
    /** The default landscape choice: repositories with a commit in the past year. */
    public static final String DEFAULT_WINDOW = "365";

    /** Lines of main code of the repositories in one activity window. */
    public static class Window {
        private final String id;
        private final String label;
        private final int days;
        private int repositories;
        private long mainLoc;

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
            return "{\"id\":\"" + id + "\",\"label\":\"" + label + "\",\"repositories\":" + repositories + ",\"main\":" + mainLoc + "}";
        }
    }

    public static void add(RichTextReport report, CodeAnalysisResults results) {
        report.addHtmlContent(html(results.getMainAspectAnalysisResults().getLinesOfCode(), null, EstimateAssumptions.ruleOfThumbJson(results)));
    }

    /** The landscape version: totals over the given repositories, per activity window (default: the past year). */
    public static void addForLandscape(RichTextReport report, List<CodeAnalysisResults> repositories) {
        List<Window> windows = windows(repositories);
        Window selected = windows.stream().filter(w -> w.id.equals(DEFAULT_WINDOW)).findFirst().orElse(windows.get(windows.size() - 1));
        report.addHtmlContent(html(selected.mainLoc, windows, "{}"));
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

    static String html(long mainLoc, List<Window> windows) {
        return html(mainLoc, windows, "{}");
    }

    /** {@code assumptionsJson}: the configured defaults ({@link EstimateAssumptions}), {@code {}} for none. */
    static String html(long mainLoc, List<Window> windows, String assumptionsJson) {
        String windowsJson = "";
        if (windows != null && !windows.isEmpty()) {
            StringBuilder json = new StringBuilder("[");
            windows.forEach(w -> json.append(json.length() > 1 ? "," : "").append(w.toJson()));
            windowsJson = json.append("]").toString();
        }
        return HtmlTemplateUtils.getResource(TEMPLATE)
                .replace("${mainLoc}", String.valueOf(Math.max(0, mainLoc)))
                .replace("${windows}", HtmlEscapeUtils.escape(windowsJson))
                .replace("${defaultWindow}", DEFAULT_WINDOW)
                .replace("${assumptions}", HtmlEscapeUtils.escape(assumptionsJson));
    }
}
