/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.aspects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.common.utils.SystemUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NamedSourceCodeAspect {
    private String name = "";
    @JsonIgnore
    private String filtering = "";

    private List<SourceFileFilter> sourceFileFilters = new ArrayList<>();
    private List<String> files = new ArrayList<>();
    @JsonIgnore
    private List<SourceFile> sourceFiles = new ArrayList<>();
    @JsonIgnore
    private Map<String, SourceFile> sourceFilesPathMap = null;

    public NamedSourceCodeAspect() {
    }

    public NamedSourceCodeAspect(String name) {
        this();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public String getFiltering() {
        return filtering;
    }

    @JsonIgnore
    public void setFiltering(String filtering) {
        this.filtering = filtering;
    }

    public List<SourceFileFilter> getSourceFileFilters() {
        return sourceFileFilters;
    }

    public void setSourceFileFilters(List<SourceFileFilter> sourceFileFilters) {
        this.sourceFileFilters = sourceFileFilters;
    }

    @JsonIgnore
    public List<SourceFile> getSourceFiles() {
        return sourceFiles;
    }

    @JsonIgnore
    public void setSourceFiles(List<SourceFile> sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    @JsonIgnore
    public int getLinesOfCode() {
        int linesOfCode = 0;
        for (SourceFile sourceFile : sourceFiles) {
            linesOfCode += sourceFile.getLinesOfCode();
        }

        return linesOfCode;
    }

    public SourceFile getSourceFile(File file) {
        for (SourceFile sourceFile : sourceFiles) {
            if (file.equals(sourceFile.getFile())) {
                return sourceFile;
            }
        }
        return null;
    }

    public void remove(NamedSourceCodeAspect aspect) {
        aspect.getSourceFiles().forEach(sourceFile -> {
            SourceFile existingSourceFile = getSourceFile(sourceFile.getFile());
            if (existingSourceFile != null) {
                sourceFiles.remove(existingSourceFile);
            }
        });
    }

    @JsonIgnore
    public String getFileSystemFriendlyName(String prefix) {
        return SystemUtils.getFileSystemFriendlyName(prefix + name);
    }

    @JsonIgnore
    public SourceFile getSourceFileByPath(String path) {
        if (sourceFilesPathMap == null) {
            sourceFilesPathMap = new HashMap<>();
            sourceFiles.forEach(sourceFile -> sourceFilesPathMap.put(sourceFile.getRelativePath(), sourceFile));
        }
        return sourceFilesPathMap.get(path);
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }
}
