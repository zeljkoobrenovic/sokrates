package nl.obren.sokrates.sourcecode.aspects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.*;

public class SourceCodeAspect {
    private String name = "";

    @JsonIgnore
    private String filtering = "";

    private List<SourceFileFilter> sourceFileFilters = new ArrayList<>();
    @JsonIgnore
    private List<SourceFile> sourceFiles = new ArrayList<>();

    public SourceCodeAspect() {
    }

    public SourceCodeAspect(String name) {
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

    @JsonIgnore
    public List<SourceCodeAspect> getAspectsPerExtensions() {
        Map<String, SourceCodeAspect> map = new HashMap<>();

        sourceFiles.forEach(sourceFile -> {
            String extension = FilenameUtils.getExtension(sourceFile.getFile().getPath()).toLowerCase();
            SourceCodeAspect extensionAspect = map.get(extension);
            if (extensionAspect == null) {
                extensionAspect = new SourceCodeAspect("  *." + extension);
                map.put(extension, extensionAspect);
            }
            extensionAspect.getSourceFiles().add(sourceFile);
        });

        List<SourceCodeAspect> list = new ArrayList<>();
        map.values().forEach(list::add);
        Collections.sort(list, (o1, o2) -> o1.getLinesOfCode() > o2.getLinesOfCode() ? -1 : (o1.getLinesOfCode() < o2.getLinesOfCode() ? 1 : 0));

        return list;
    }

    public void remove(SourceCodeAspect aspect) {
        aspect.getSourceFiles().forEach(sourceFile -> {
            SourceFile existingSourceFile = getSourceFile(sourceFile.getFile());
            if (existingSourceFile != null) {
                sourceFiles.remove(existingSourceFile);
            }
        });
    }
}
