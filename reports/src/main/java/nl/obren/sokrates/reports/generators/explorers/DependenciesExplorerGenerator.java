/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.explorers;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.reports.dataexporters.dependencies.DependenciesExportInfo;
import nl.obren.sokrates.reports.dataexporters.dependencies.DependenciesExporter;
import nl.obren.sokrates.reports.utils.HtmlTemplateUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;

public class DependenciesExplorerGenerator {
    public static final String DEPENDENCIES_DATA = "\"${__DEPENDENCIES_DATA__}\"";
    private CodeAnalysisResults codeAnalysisResults;

    public DependenciesExplorerGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public String generateExplorer() {
        return generateExplorer(getDuplicationExportInfo());
    }

    private String generateExplorer(DependenciesExportInfo exportInfo) {
        String html = HtmlTemplateUtils.getResource("/templates/Dependencies.html");
        try {
            html = html.replace(DEPENDENCIES_DATA, new JsonGenerator().generate(exportInfo));
        } catch (JsonProcessingException e) {
            html = html.replace(DEPENDENCIES_DATA, "{}");
            e.printStackTrace();
        }

        return html;
    }

    private DependenciesExportInfo getDuplicationExportInfo() {
        DependenciesExporter dependenciesExporter = new DependenciesExporter(codeAnalysisResults.getAllDependencies());

        DependenciesExportInfo exportInfo = new DependenciesExportInfo();
        exportInfo.setLinks(dependenciesExporter.getDependenciesExportInfo());

        return exportInfo;
    }
}
