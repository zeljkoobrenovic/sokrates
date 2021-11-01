/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.explorers;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.reports.dataexporters.duplication.DuplicationExportInfo;
import nl.obren.sokrates.reports.dataexporters.duplication.DuplicationExporter;
import nl.obren.sokrates.reports.utils.HtmlTemplateUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.DuplicationAnalysisResults;

public class DuplicationExplorerGenerator {
    public static final String DUPLICATION_DATA = "\"${__DUPLICATION_DATA__}\"";
    private CodeAnalysisResults codeAnalysisResults;

    public DuplicationExplorerGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public String generateExplorer() {
        return generateExplorer(getDuplicationExportInfo());
    }

    private String generateExplorer(DuplicationExportInfo exportInfo) {
        String html = HtmlTemplateUtils.getResource("/templates/Duplicates.html");
        try {
            html = html.replace(DUPLICATION_DATA, new JsonGenerator().generate(exportInfo));
        } catch (JsonProcessingException e) {
            html = html.replace(DUPLICATION_DATA, "{}");
            e.printStackTrace();
        }

        return html;
    }

    private DuplicationExportInfo getDuplicationExportInfo() {
        DuplicationAnalysisResults duplicationAnalysisResults = codeAnalysisResults.getDuplicationAnalysisResults();
        DuplicationExporter duplicationExporter = new DuplicationExporter(duplicationAnalysisResults.getAllDuplicates());

        DuplicationExportInfo exportInfo = new DuplicationExportInfo();
        exportInfo.setTitle("Duplication");
        exportInfo.setOverallDuplication(duplicationAnalysisResults.getOverallDuplication());
        exportInfo.setDuplicates(duplicationExporter.getDuplicatesExportInfo());

        return exportInfo;
    }
}
