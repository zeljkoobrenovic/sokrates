package nl.obren.sokrates.sourcecode.aspects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SourceCodeAspect {
    @JsonIgnore
    private String filtering = "";

    private List<SourceFileFilter> sourceFileFilters = new ArrayList<>();
    @JsonIgnore
    private List<SourceFile> sourceFiles = new ArrayList<>();

    public SourceCodeAspect() {
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
}
