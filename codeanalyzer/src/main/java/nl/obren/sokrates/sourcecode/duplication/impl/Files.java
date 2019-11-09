package nl.obren.sokrates.sourcecode.duplication.impl;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzer;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Files {
    private static final Log LOG = LogFactory.getLog(Files.class);

    private List<FileInfoForDuplication> files = new ArrayList<>();
    private Map<SourceFile, FileInfoForDuplication> filesMap = new HashMap<>();
    private LineIndexesExtractor lineIndexesExtractor = new LineIndexesExtractor();
    private Map<SourceFile, CleanedContent> pathToCleanedContent = new HashMap<>();
    private int totalCleanedLinesOfCode;
    private ProgressFeedback progressFeedback;

    public Files(ProgressFeedback progressFeedback) {
        this.progressFeedback = progressFeedback;
    }

    public void addAll(List<SourceFile> sourceFiles) {
        if (progressFeedback != null) {
            progressFeedback.setText("Transforming lines into numeric IDs");
        }
        int progressValue[] = {0};
        sourceFiles.forEach(sourceFile -> {
            if (progressFeedback != null) {
                progressFeedback.progress(progressValue[0]++, sourceFiles.size());
            }
            add(sourceFile);
        });
        clearUniqueLines();
        lineIndexesExtractor = null;
        if (progressFeedback != null) {
            progressFeedback.progress(progressValue[0]++, sourceFiles.size());
        }
    }

    private void clearUniqueLines() {
        if (progressFeedback != null) {
            progressFeedback.setText("Clearing unique lines");
        }
        int progressValue[] = {0};
        files.forEach(fileLineIndexes -> {
            if (progressFeedback != null) {
                progressFeedback.progress(progressValue[0]++, files.size());
            }
            lineIndexesExtractor.clearUniqueLines(fileLineIndexes.getLineIDs());
        });
    }

    public void add(SourceFile sourceFile) {
        try {
            FileInfoForDuplication fileInfoForDuplication = new FileInfoForDuplication();
            fileInfoForDuplication.setSourceFile(sourceFile);
            fileInfoForDuplication.setLineIDs(getLinesAsNumbers(sourceFile));

            files.add(fileInfoForDuplication);
            filesMap.put(sourceFile, fileInfoForDuplication);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Integer> getLinesAsNumbers(SourceFile sourceFile) throws IOException {
        LanguageAnalyzer languageAnalyzer = LanguageAnalyzerFactory.getInstance().getLanguageAnalyzer(sourceFile);
        CleanedContent cleanedContent = languageAnalyzer.cleanForDuplicationCalculations(sourceFile);

        pathToCleanedContent.put(sourceFile, cleanedContent);
        totalCleanedLinesOfCode += cleanedContent.getFileLineIndexes().size();

        return lineIndexesExtractor.getLineIDs(cleanedContent.getLines());
    }

    public LineIndexesExtractor getLineIndexesExtractor() {
        return lineIndexesExtractor;
    }

    public void setLineIndexesExtractor(LineIndexesExtractor lineIndexesExtractor) {
        this.lineIndexesExtractor = lineIndexesExtractor;
    }

    public List<FileInfoForDuplication> getFiles() {
        return files;
    }

    public void setFiles(List<FileInfoForDuplication> files) {
        this.files = files;
    }

    public Map<SourceFile, FileInfoForDuplication> getFilesMap() {
        return filesMap;
    }

    public void setFilesMap(Map<SourceFile, FileInfoForDuplication> filesMap) {
        this.filesMap = filesMap;
    }

    public Map<SourceFile, CleanedContent> getPathToCleanedContent() {
        return pathToCleanedContent;
    }

    public void setPathToCleanedContent(Map<SourceFile, CleanedContent> pathToCleanedContent) {
        this.pathToCleanedContent = pathToCleanedContent;
    }

    public int getTotalCleanedLinesOfCode() {
        return totalCleanedLinesOfCode;
    }

    public void setTotalCleanedLinesOfCode(int totalCleanedLinesOfCode) {
        this.totalCleanedLinesOfCode = totalCleanedLinesOfCode;
    }
}
