/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.dataexporters.files;

import nl.obren.sokrates.sourcecode.SourceFile;

import java.util.ArrayList;
import java.util.List;

public class FileExportInfo {
    private String relativePath;
    private String extension;
    private int linesOfCode;
    private List<String> components;
    private List<String> concerns;

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

        ArrayList<String> concerns = new ArrayList<>();
        sourceFile.getConcerns().forEach(concern -> {
            concerns.add(concern.getFiltering() + "::" + concern.getName());
        });
        fileExportInfo.setConcerns(concerns);

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

    public List<String> getConcerns() {
        return concerns;
    }

    public void setConcerns(List<String> concerns) {
        this.concerns = concerns;
    }
}
