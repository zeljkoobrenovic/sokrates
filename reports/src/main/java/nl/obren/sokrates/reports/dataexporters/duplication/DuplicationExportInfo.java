package nl.obren.sokrates.reports.dataexporters.duplication;

import nl.obren.sokrates.reports.utils.DateUtils;
import nl.obren.sokrates.sourcecode.metrics.DuplicationMetric;

import java.util.ArrayList;
import java.util.List;

public class DuplicationExportInfo {
    private String title;
    private String timestamp = DateUtils.getCurrentDateTime();
    private int totalLocCount = 0;
    private int duplicatedLocCount = 0;
    private List<DuplicateExportInfo> duplicates = new ArrayList<>();
    private DuplicationMetric overallDuplication;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public List<DuplicateExportInfo> getDuplicates() {
        return duplicates;
    }

    public void setDuplicates(List<DuplicateExportInfo> duplicates) {
        this.duplicates = duplicates;
    }

    public void setOverallDuplication(DuplicationMetric overallDuplication) {
        this.overallDuplication = overallDuplication;
    }

    public DuplicationMetric getOverallDuplication() {
        return overallDuplication;
    }
}
