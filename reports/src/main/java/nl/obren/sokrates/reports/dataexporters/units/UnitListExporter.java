/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.dataexporters.units;

import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UnitListExporter {
    private List<UnitInfo> units = new ArrayList<>();

    public UnitListExporter(List<UnitInfo> units) {
        this.units = units;
    }

    /**
     * Builds export objects for at most {@code limit} units (the largest by lines of code; a
     * non-positive limit means no cap). On very large repositories the full unit list (millions of
     * units, each expanding into a UnitInfoExport with component strings) can exhaust the heap, so
     * the data/text unit exports cap the INPUT before materialising any export objects.
     */
    public List<UnitInfoExport> getAllUnitsData(int limit) {
        List<UnitInfo> source = units;
        if (limit > 0 && units.size() > limit) {
            source = units.stream()
                    .sorted((a, b) -> b.getLinesOfCode() - a.getLinesOfCode())
                    .limit(limit)
                    .collect(Collectors.toList());
        }
        return toExports(source);
    }

    public List<UnitInfoExport> getAllUnitsData() {
        return toExports(units);
    }

    private List<UnitInfoExport> toExports(List<UnitInfo> units) {
        List<UnitInfoExport> unitInfoExports = new ArrayList<>();

        units.forEach(unit -> {
            UnitInfoExport unitInfoExport = new UnitInfoExport();
            unitInfoExport.setShortName(unit.getShortName());
            unitInfoExport.setLongName(unit.getLongName());
            unitInfoExport.setLinesOfCode(unit.getLinesOfCode());
            unitInfoExport.setMcCabeIndex(unit.getMcCabeIndex());
            unitInfoExport.setNumberOfExpressions(unit.getNumberOfExpressions());
            unitInfoExport.setNumberOfLiterals(unit.getNumberOfLiterals());
            unitInfoExport.setNumberOfParameters(unit.getNumberOfParameters());
            unitInfoExport.setNumberOfStatements(unit.getNumberOfStatements());
            unitInfoExport.setRelativeFileName(unit.getSourceFile().getRelativePath());
            unitInfoExport.setFileLinesCount(unit.getSourceFile().getLines().size());
            unitInfoExport.setStartLine(unit.getStartLine());
            unitInfoExport.setEndLine(unit.getEndLine());
            ArrayList<String> components = new ArrayList<>();
            unit.getSourceFile().getLogicalComponents().forEach(component -> {
                components.add(component.getFiltering() + "::" + component.getName());
            });
            unitInfoExport.setComponents(components);
            unitInfoExports.add(unitInfoExport);
        });

        return unitInfoExports;
    }
}
