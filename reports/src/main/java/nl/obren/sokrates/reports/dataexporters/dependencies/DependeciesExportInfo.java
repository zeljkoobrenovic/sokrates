package nl.obren.sokrates.reports.dataexporters.dependencies;

import nl.obren.sokrates.reports.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class DependeciesExportInfo {
    private String title = "Dependencies";
    private String timestamp = DateUtils.getCurrentDateTime();

    private List<DependencyExportInfo> links = new ArrayList<>();

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

    public List<DependencyExportInfo> getLinks() {
        return links;
    }

    public void setLinks(List<DependencyExportInfo> links) {
        this.links = links;
    }
}
