/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.age.FileModificationHistory;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzer;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SourceFile {
    private static final Log LOG = LogFactory.getLog(SourceFile.class);

    @JsonIgnore
    private File file;
    private String relativePath;
    private String extension;
    private int linesOfCode;
    @JsonIgnore
    private FileModificationHistory fileModificationHistory = null;
    private int unitsCount = 0;
    private int unitsMcCabeIndexSum = 0;
    private List<NamedSourceCodeAspect> logicalComponents = new ArrayList<>();
    private List<NamedSourceCodeAspect> crossCuttingConcerns = new ArrayList<>();

    @JsonIgnore
    private String content;

    private int linesOfCodeInUnits;

    public SourceFile() {
    }

    public SourceFile(File file) {
        setFile(file);
    }

    public SourceFile(File file, String content) {
        setFile(file);
        setContent(content);
    }

    @JsonIgnore
    public File getFile() {
        return file;
    }

    @JsonIgnore
    public void setFile(File file) {
        this.file = file;
        this.extension = FilenameUtils.getExtension(file.getPath());
    }


    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public SourceFile relativize(File root) {
        try {
            Path rootPath = Paths.get(root.getPath());
            Path sourceFilePath = Paths.get(file.getPath());

            setRelativePath(rootPath.relativize(sourceFilePath).toString());
        } catch (InvalidPathException e) {
            e.printStackTrace();
        }
        return this;
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

    public FileModificationHistory getFileModificationHistory() {
        return fileModificationHistory;
    }

    public void setFileModificationHistory(FileModificationHistory fileModificationHistory) {
        this.fileModificationHistory = fileModificationHistory;
    }

    public void setLinesOfCode(int linesOfCode) {
        this.linesOfCode = linesOfCode;
    }

    public List<NamedSourceCodeAspect> getLogicalComponents(String filer) {
        List<NamedSourceCodeAspect> filteredLogicalComponents = new ArrayList<>();
        logicalComponents.stream().filter(comp -> comp.getFiltering().equals(filer)).forEach(filteredLogicalComponents::add);
        return filteredLogicalComponents;
    }

    public List<NamedSourceCodeAspect> getLogicalComponents() {
        return logicalComponents;
    }

    public void setLogicalComponents(List<NamedSourceCodeAspect> logicalComponents) {
        this.logicalComponents = logicalComponents;
    }

    public List<NamedSourceCodeAspect> getCrossCuttingConcerns() {
        return crossCuttingConcerns;
    }

    public void setCrossCuttingConcerns(List<NamedSourceCodeAspect> crossCuttingConcerns) {
        this.crossCuttingConcerns = crossCuttingConcerns;
    }

    public int getUnitsCount() {
        return unitsCount;
    }

    public void setUnitsCount(int unitsCount) {
        this.unitsCount = unitsCount;
    }

    public int getUnitsMcCabeIndexSum() {
        return unitsMcCabeIndexSum;
    }

    public void setUnitsMcCabeIndexSum(int unitsMcCabeIndexSum) {
        this.unitsMcCabeIndexSum = unitsMcCabeIndexSum;
    }

    @JsonIgnore
    public String getContent() {
        try {
            return StringUtils.isNotBlank(content) ? content : getFile() != null ? FileUtils.readFileToString(getFile(), StandardCharsets.UTF_8) : "";
        } catch (IOException e) {
            LOG.debug(e);
        }

        return "";
    }

    @JsonIgnore
    public void setContent(String content) {
        this.content = content;
    }

    @JsonIgnore
    public List<String> getLines() {
        try {
            List<String> lines = StringUtils.isNotBlank(content)
                    ? SourceCodeCleanerUtils.splitInLines(content) : getFile() != null
                    ? FileUtils.readLines(getFile(), StandardCharsets.UTF_8) : new ArrayList<>();
            return lines;
        } catch (IOException e) {
            LOG.debug(e);
            return new ArrayList<>();
        }
    }

    @JsonIgnore
    public List<String> getCleanedLines() {
        LanguageAnalyzer languageAnalyzer = LanguageAnalyzerFactory.getInstance().getLanguageAnalyzer(this);
        return SourceCodeCleanerUtils.splitInLines(languageAnalyzer.cleanForLinesOfCodeCalculations(this).getCleanedContent());
    }

    @JsonIgnore
    public List<String> getCleanedLinesForDuplication() {
        return SourceCodeCleanerUtils.splitInLines(LanguageAnalyzerFactory.getInstance().getLanguageAnalyzer(this).cleanForDuplicationCalculations(this).getCleanedContent());
    }

    @JsonIgnore
    public void setLinesOfCodeFromContent() {
        linesOfCode = getCleanedLines().size();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SourceFile)) {
            return false;
        }

        return ((SourceFile) obj).getFile().equals(this.getFile());
    }

    public void setLinesOfCodeInUnits(int linesOfCodeInUnits) {
        this.linesOfCodeInUnits = linesOfCodeInUnits;
    }

    public int getLinesOfCodeInUnits() {
        return linesOfCodeInUnits;
    }
}
