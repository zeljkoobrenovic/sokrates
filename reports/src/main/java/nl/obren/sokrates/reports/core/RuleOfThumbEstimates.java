/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.reports.utils.HtmlTemplateUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.FileReadsForChangesConfig;

/**
 * The collapsed "Rule-of-thumb estimates" block at the bottom of the At a Glance tab: an in-page calculator
 * (rebuild value, maintenance effort, AI token reads) over the analysis' lines of code. Only numbers go into
 * the template; the assumptions are inputs the viewer can change.
 */
public class RuleOfThumbEstimates {
    static final String TEMPLATE = "/templates/rule-of-thumb-estimates.html";

    public static void add(RichTextReport report, CodeAnalysisResults results) {
        int otherLoc = results.getBuildAndDeployAspectAnalysisResults().getLinesOfCode()
                + results.getGeneratedAspectAnalysisResults().getLinesOfCode()
                + results.getOtherAspectAnalysisResults().getLinesOfCode();
        FileReadsForChangesConfig reads = results.getCodeConfiguration().getAnalysis().getFileReadsForChanges();
        report.addHtmlContent(html(results.getMainAspectAnalysisResults().getLinesOfCode(),
                results.getTestAspectAnalysisResults().getLinesOfCode(), otherLoc,
                reads.getTokensPerLineMin(), reads.getTokensPerLineMax()));
    }

    static String html(int mainLoc, int testLoc, int otherLoc, int tokensPerLineMin, int tokensPerLineMax) {
        int tokensMin = Math.max(1, Math.min(tokensPerLineMin, tokensPerLineMax));
        int tokensMax = Math.max(tokensMin, Math.max(tokensPerLineMin, tokensPerLineMax));
        return HtmlTemplateUtils.getResource(TEMPLATE)
                .replace("${mainLoc}", String.valueOf(Math.max(0, mainLoc)))
                .replace("${testLoc}", String.valueOf(Math.max(0, testLoc)))
                .replace("${otherLoc}", String.valueOf(Math.max(0, otherLoc)))
                .replace("${tokensMin}", String.valueOf(tokensMin))
                .replace("${tokensMax}", String.valueOf(tokensMax));
    }
}
