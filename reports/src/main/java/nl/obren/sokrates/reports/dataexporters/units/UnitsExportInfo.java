package nl.obren.sokrates.reports.dataexporters.units;

import nl.obren.sokrates.reports.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class UnitsExportInfo {
    private String timestamp = DateUtils.getCurrentDateTime();
    private String srcRoot = "";
    private int totalUnitsCount = 0;
    private int totalLinesOfCode = 0;
    private String title = "Units";
    private List<UnitInfoExport> units = new ArrayList<>();

    public UnitsExportInfo(String srcRoot, String title, int totalUnitsCount, int totalLinesOfCode) {
        this.srcRoot = srcRoot;
        this.title = title;
        this.totalUnitsCount = totalUnitsCount;
        this.totalLinesOfCode = totalLinesOfCode;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getSrcRoot() {
        return srcRoot;
    }

    public void setSrcRoot(String srcRoot) {
        this.srcRoot = srcRoot;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getTotalUnitsCount() {
        return totalUnitsCount;
    }

    public void setTotalUnitsCount(int totalUnitsCount) {
        this.totalUnitsCount = totalUnitsCount;
    }

    public int getTotalLinesOfCode() {
        return totalLinesOfCode;
    }

    public void setTotalLinesOfCode(int totalLinesOfCode) {
        this.totalLinesOfCode = totalLinesOfCode;
    }

    public List<UnitInfoExport> getUnits() {
        return units;
    }

    public void setUnits(List<UnitInfoExport> units) {
        this.units = units;
    }
}
