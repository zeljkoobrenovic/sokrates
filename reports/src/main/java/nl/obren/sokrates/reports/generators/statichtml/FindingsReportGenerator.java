package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FindingsReportGenerator {
    private RichTextReport report;
    private File codeConfigurationFile;

    public FindingsReportGenerator(File codeConfigurationFile) {

        this.codeConfigurationFile = codeConfigurationFile;
    }

    public RichTextReport generateReport(CodeAnalysisResults codeAnalysisResults, RichTextReport report) {
        this.report = report;

        File findingsFile = CodeConfigurationUtils.getDefaultSokratesFindingsFile(codeConfigurationFile.getParentFile());

        if (findingsFile.exists()) {
            try {
                String html = FileUtils.readFileToString(findingsFile, StandardCharsets.UTF_8);

                List<String> summaryItems = RegexUtils.getMatchedRegexesNoLimits(html, "<summary>.*?</summary>");

                if (summaryItems.size() > 0) {
                    report.addLevel2Header("Summary");
                    report.startUnorderedList();
                    summaryItems.forEach(item -> {
                        report.addListItem(item);
                    });
                    report.endUnorderedList();
                    report.addLineBreak();
                }
                report.addLevel2Header("Details");

                html = html.replace("<finding>", "<div class='section'>");
                html = html.replace("</finding>", "</div>");

                html = html.replace("<summary>", "<div class='sectionHeader sectionTitle'>");
                html = html.replace("</summary>", "</div>");

                html = html.replace("<body>", "<div class='sectionBody'>");
                html = html.replace("</body>", "</div>");

                report.addHtmlContent(html);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return report;
    }
}
