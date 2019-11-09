package nl.obren.sokrates.reports.generators.explorers;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.reports.dataexporters.units.UnitListExporter;
import nl.obren.sokrates.reports.dataexporters.units.UnitsExportInfo;
import nl.obren.sokrates.reports.utils.HtmlTemplateUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.UnitsAnalysisResults;

public class UnitsExplorerGenerator {
    public static final String UNITS_DATA = "\"${__UNITS_DATA__}\"";
    private CodeAnalysisResults codeAnalysisResults;

    public UnitsExplorerGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public String generateExplorer() {
        return generateExplorer(getUnitsExportInfo());
    }

    private String generateExplorer(UnitsExportInfo exportInfo) {
        String html = HtmlTemplateUtils.getTemplate("/templates/Units.html");
        try {
            html = html.replace(UNITS_DATA, new JsonGenerator().generate(exportInfo));
        } catch (JsonProcessingException e) {
            html = html.replace(UNITS_DATA, "{}");
            e.printStackTrace();
        }

        return html;
    }

    private UnitsExportInfo getUnitsExportInfo() {
        UnitsAnalysisResults unitsAnalysisResults = codeAnalysisResults.getUnitsAnalysisResults();
        UnitsExportInfo exportInfo = new UnitsExportInfo("fragments/all_units",
                "Units Explorer",
                codeAnalysisResults.getMainAspectAnalysisResults().getFilesCount(),
                codeAnalysisResults.getMainAspectAnalysisResults().getLinesOfCode());
        UnitListExporter unitListExporter = new UnitListExporter(unitsAnalysisResults.getAllUnits());
        exportInfo.setUnits(unitListExporter.getAllUnitsData());
        return exportInfo;
    }
}
