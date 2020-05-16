/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.landspace.statichtml;

import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.core.SummaryUtils;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeGroupAnalysisResults;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

public class LandscapeReportGenerator {
    private static final Log LOG = LogFactory.getLog(LandscapeReportGenerator.class);

    private RichTextReport landscapeReport = new RichTextReport("Landscape Report", "index.html");
    private LandscapeAnalysisResults landscapeAnalysisResults;

    public LandscapeReportGenerator(LandscapeAnalysisResults landscapeAnalysisResults) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;

        landscapeReport.startDiv("");
        addBlock(landscapeAnalysisResults, getSmallTextForNumber(landscapeAnalysisResults.getProjectsCount()), "projects");
        addBlock(landscapeAnalysisResults, getSmallTextForNumber(landscapeAnalysisResults.getMainLoc()), "lines of code");
        addBlock(landscapeAnalysisResults, getSmallTextForNumber(landscapeAnalysisResults.getMainFileCount()), "files");
        landscapeReport.endDiv();
        landscapeReport.startSection("Details", "All the details");
        landscapeReport.startUnorderedList();
        landscapeReport.addListItem(landscapeAnalysisResults.getProjectsCount() + " projects");
        landscapeReport.addListItem(landscapeAnalysisResults.getMainLoc() + " lines of code");
        landscapeReport.addListItem(landscapeAnalysisResults.getMainFileCount() + " main files");
        landscapeReport.endUnorderedList();
        landscapeReport.endSection();

        landscapeAnalysisResults.getGroupsAnalysisResults().forEach(groupAnalysisResults -> {
            addGroup(groupAnalysisResults);
        });
    }

    private void addGroup(LandscapeGroupAnalysisResults groupAnalysisResults) {
        landscapeReport.startSection(groupAnalysisResults.getGroup().getMetadata().getName(), "");

        groupAnalysisResults.getProjectsAnalysisResults().forEach(projectAnalysis -> {
            landscapeReport.startSubSection(projectAnalysis.getCodeConfiguration().getMetadata().getName(), "");
            new SummaryUtils().summarize(projectAnalysis, landscapeReport);
            landscapeReport.endSection();
        });

        landscapeReport.endSection();
    }

    private String getSmallTextForNumber(int number) {
        if (number < 1000) {
            return "<b>" + number + "</b>" + "";
        } else if (number < 10000) {
            return "<b>" + String.format("%.1f", number / 1000f) + "</b>" + "K";
        } else if (number < 1000000) {
            return "<b>" + Math.round(number / 1000f) + "</b>" + "K";
        } else {
            return "<b>" + String.format("%.1f", number / 1000000f) + "</b>" + "M";
        }
    }

    private void addBlock(LandscapeAnalysisResults landscapeAnalysisResults, String mainValue, String subtitle) {
        landscapeReport.startDiv("border-radius: 12px; margin: 12px; display: inline-block; width: 160px; height: 120px; background-color: skyblue; text-align: center; vertical-align: middle;");
        landscapeReport.addHtmlContent("<div style='font-size: 50px; margin-top: 20px'>" + mainValue + "</div>");
        landscapeReport.addHtmlContent("<div style='color: #434343; font-size: 16px'>" + subtitle + "</div>");
        landscapeReport.endDiv();
    }

    public List<RichTextReport> report() {
        List<RichTextReport> reports = new ArrayList<>();

        reports.add(this.landscapeReport);

        return reports;
    }
}
