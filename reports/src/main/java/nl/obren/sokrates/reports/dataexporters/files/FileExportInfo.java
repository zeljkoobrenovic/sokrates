package nl.obren.sokrates.reports.dataexporters.files;

import nl.obren.sokrates.sourcecode.SourceFile;

import java.util.ArrayList;
import java.util.List;

public class FileExportInfo {
    private String relativePath;
    private String extension;
    private int linesOfCode;
    private List<String> components;
    private List<String> crossCuttingConcerns;

    public static FileExportInfo getInstance(SourceFile sourceFile) {
        FileExportInfo fileExportInfo = new FileExportInfo();
        fileExportInfo.setRelativePath(sourceFile.getRelativePath());
        fileExportInfo.setExtension(sourceFile.getExtension());
        fileExportInfo.setLinesOfCode(sourceFile.getLinesOfCode());
        ArrayList<String> components = new ArrayList<>();
        sourceFile.getLogicalComponents().forEach(component -> {
            components.add(component.getFiltering() + "::" + component.getName());
        });
        fileExportInfo.setComponents(components);

        ArrayList<String> crossCuttingConcerns = new ArrayList<>();
        sourceFile.getCrossCuttingConcerns().forEach(concern -> {
            crossCuttingConcerns.add(concern.getFiltering() + "::" + concern.getName());
        });
        fileExportInfo.setCrossCuttingConcerns(crossCuttingConcerns);

        return fileExportInfo;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public int getLinesOfCode() {
        return linesOfCode;
    }

    public void setLinesOfCode(int linesOfCode) {
        this.linesOfCode = linesOfCode;
    }

    public List<String> getComponents() {
        return components;
    }

    public void setComponents(List<String> components) {
        this.components = components;
    }

    public List<String> getCrossCuttingConcerns() {
        return crossCuttingConcerns;
    }

    public void setCrossCuttingConcerns(List<String> crossCuttingConcerns) {
        this.crossCuttingConcerns = crossCuttingConcerns;
    }
}
