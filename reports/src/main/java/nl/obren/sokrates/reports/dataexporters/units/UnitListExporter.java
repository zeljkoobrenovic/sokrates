package nl.obren.sokrates.reports.dataexporters.units;

import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.ArrayList;
import java.util.List;

public class UnitListExporter {
    private List<UnitInfo> units = new ArrayList<>();

    public UnitListExporter(List<UnitInfo> units) {
        this.units = units;
    }

    public List<UnitInfoExport> getAllUnitsData() {
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
