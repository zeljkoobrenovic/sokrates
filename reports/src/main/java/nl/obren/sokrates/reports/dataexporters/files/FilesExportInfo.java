package nl.obren.sokrates.reports.dataexporters.files;

import nl.obren.sokrates.reports.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class FilesExportInfo {
    private String timestamp = DateUtils.getCurrentDateTime();
    private String srcRoot = "";
    private int totalFilesCount = 0;
    private int totalLinesOfCode = 0;
    private String title = "Files Explorer";
    private List<FileExportInfo> files = new ArrayList<>();

    public FilesExportInfo(String srcRoot, String title, int totalUnitsCount, int totalLinesOfCode) {
        this.srcRoot = srcRoot;
        this.title = title;
        this.totalFilesCount = totalUnitsCount;
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

    public int getTotalFilesCount() {
        return totalFilesCount;
    }

    public void setTotalFilesCount(int totalFilesCount) {
        this.totalFilesCount = totalFilesCount;
    }

    public int getTotalLinesOfCode() {
        return totalLinesOfCode;
    }

    public void setTotalLinesOfCode(int totalLinesOfCode) {
        this.totalLinesOfCode = totalLinesOfCode;
    }

    public List<FileExportInfo> getFiles() {
        return files;
    }

    public void setFiles(List<FileExportInfo> units) {
        this.files = units;
    }
}
