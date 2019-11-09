package nl.obren.sokrates.sourcecode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.aspects.SourceCodeAspect;
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
    private List<SourceCodeAspect> logicalComponents = new ArrayList<>();
    private List<SourceCodeAspect> crossCuttingConcerns = new ArrayList<>();
    @JsonIgnore
    private String content;

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
        Path rootPath = Paths.get(root.getPath());
        Path sourceFilePath = Paths.get(file.getPath());

        setRelativePath(rootPath.relativize(sourceFilePath).toString());
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

    public void setLinesOfCode(int linesOfCode) {
        this.linesOfCode = linesOfCode;
    }

    public List<SourceCodeAspect> getLogicalComponents(String group) {
        List<SourceCodeAspect> filteredLogicalComponents = new ArrayList<>();
        logicalComponents.stream().filter(comp -> comp.getFiltering().equals(group)).forEach(filteredLogicalComponents::add);
        return filteredLogicalComponents;
    }

    public List<SourceCodeAspect> getLogicalComponents() {
        return logicalComponents;
    }

    public void setLogicalComponents(List<SourceCodeAspect> logicalComponents) {
        this.logicalComponents = logicalComponents;
    }

    public List<SourceCodeAspect> getCrossCuttingConcerns() {
        return crossCuttingConcerns;
    }

    public void setCrossCuttingConcerns(List<SourceCodeAspect> crossCuttingConcerns) {
        this.crossCuttingConcerns = crossCuttingConcerns;
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
}
