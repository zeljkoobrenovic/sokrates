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
import java.util.IdentityHashMap;
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

            // Map units that have a saved source-code fragment to its viewer URL. Fragments are
            // bundled (only when saveCodeFragments is on) into src/fragments/<type>.json for the
            // longest-units and most-complex-units lists; the viewer link carries the 1-based list
            // position as &i=, so we mirror that indexing here. Identity (==) match, as the explorer
            // reuses the same UnitInfo objects. Longest-unit fragments take precedence when a unit is
            // in both lists.
            Map<UnitInfo, String> fragmentLinks = new IdentityHashMap<>();
            if (codeAnalysisResults.getCodeConfiguration().getAnalysis().isSaveCodeFragments()) {
                addFragmentLinks(fragmentLinks, codeAnalysisResults.getUnitsAnalysisResults().getLongestUnits(), "longest_unit");
                addFragmentLinks(fragmentLinks, codeAnalysisResults.getUnitsAnalysisResults().getMostComplexUnits(), "most_complex_units");
            }

            List<UnitExport> unitExports = units.stream().map(unit -> {
                UnitExport export = new UnitExport(unit);
                String link = fragmentLinks.get(unit);
                if (link != null) {
                    export.setFragmentLink(link);
                }
                return export;
            }).collect(Collectors.toList());

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

    /**
     * Records, per unit, the relative fragment URL (from the explorers/ folder) for a saved-fragment
     * list, mirroring DataExporter's naming: {@code <type>/<type>_<1-based index>.<ext>.html}.
     * Does not overwrite an existing mapping, so the first list passed wins for shared units.
     */
    private void addFragmentLinks(Map<UnitInfo, String> links, List<UnitInfo> units, String fragmentType) {
        if (units == null) {
            return;
        }
        for (int i = 0; i < units.size(); i++) {
            UnitInfo unit = units.get(i);
            if (links.containsKey(unit) || unit.getSourceFile() == null) {
                continue;
            }
            String url = "../src/viewer.html?bundle=fragments/" + fragmentType + ".json&i=" + (i + 1);
            links.put(unit, url);
        }
    }

    private String thresholdsJson(Thresholds thresholds) {
        return "{\"low\": " + thresholds.getLow()
                + ", \"medium\": " + thresholds.getMedium()
                + ", \"high\": " + thresholds.getHigh()
                + ", \"veryHigh\": " + thresholds.getVeryHigh() + "}";
    }

}
