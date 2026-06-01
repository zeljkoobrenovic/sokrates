package nl.obren.sokrates.reports.generators.explorers;

import nl.obren.sokrates.common.renderingutils.ExplorerTemplate;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;
import nl.obren.sokrates.sourcecode.units.UnitExport;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generates the client-rendered units explorer (units-explorer.html) for an individual repository
 * report — the unit-level counterpart of {@link FilesExplorerGenerators}. Each unit (method/function)
 * is exported as a {@link UnitExport} embedded as JSON into the template, which renders a searchable,
 * sortable table (language, file, unit, lines of code, McCabe complexity, parameters). Unit-size and
 * conditional-complexity thresholds are passed so the template can colour-code the risk bands.
 */
public class UnitsExplorerGenerators {

    private File reportsFolder;

    public UnitsExplorerGenerators(File reportsFolder) {
        this.reportsFolder = reportsFolder;
    }

    public void exportJson(CodeAnalysisResults codeAnalysisResults) {
        try {
            List<UnitInfo> units = codeAnalysisResults.getUnitsAnalysisResults().getAllUnits();
            List<UnitExport> unitExports = units.stream().map(UnitExport::new).collect(Collectors.toList());

            ExplorerTemplate explorerTemplate = new ExplorerTemplate();

            List<String> unitLangs = unitExports.stream().map(UnitExport::getMainLang).collect(Collectors.toList());
            String unitLangIcons = DataImageUtils.getLangDataImageMapJson(unitLangs);

            Thresholds sizeThresholds = codeAnalysisResults.getCodeConfiguration().getAnalysis().getUnitSizeThresholds();
            Thresholds complexityThresholds = codeAnalysisResults.getCodeConfiguration().getAnalysis().getConditionalComplexityThresholds();

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("langIcons", unitLangIcons);
            placeholders.put("sizeThresholds", thresholdsJson(sizeThresholds));
            placeholders.put("complexityThresholds", thresholdsJson(complexityThresholds));

            String unitsExplorer = explorerTemplate.render("units-explorer.html", unitExports, placeholders);
            File folder = new File(reportsFolder, "explorers");
            folder.mkdirs();
            FileUtils.write(new File(folder, "units-explorer.html"), unitsExplorer, UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String thresholdsJson(Thresholds thresholds) {
        return "{\"low\": " + thresholds.getLow()
                + ", \"medium\": " + thresholds.getMedium()
                + ", \"high\": " + thresholds.getHigh()
                + ", \"veryHigh\": " + thresholds.getVeryHigh() + "}";
    }

}
