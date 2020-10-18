/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.duplication.impl.Blocks;
import nl.obren.sokrates.sourcecode.duplication.impl.Files;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzer;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DuplicationEngine {
    private static final Log LOG = LogFactory.getLog(DuplicationEngine.class);

    private int threshold = 6;
    private List<DuplicationInstance> duplicates = new ArrayList<>();

    private int totalCleanedLinesOfCode = 0;
    private int numberOfDuplicatedLines;

    public List<DuplicationInstance> findDuplicates(List<SourceFile> sourceFiles, ProgressFeedback progressFeedback) {
        progressFeedback.setText(System.currentTimeMillis() / 1000 + "");
        duplicates = new ArrayList<>();

        Files files = new Files(progressFeedback);
        files.addAll(sourceFiles);

        Blocks blocks = new Blocks(files, threshold);
        System.out.println(System.currentTimeMillis());
        duplicates = blocks.extractDuplicatedBlocks(progressFeedback);
        System.out.println(System.currentTimeMillis());

        totalCleanedLinesOfCode = files.getTotalCleanedLinesOfCode();

        numberOfDuplicatedLines = DuplicationUtils.getNumberOfDuplicatedLines(duplicates);

        progressFeedback.setText(System.currentTimeMillis() / 1000 + "");

        return duplicates;
    }

    private void populatePathToContentMap(List<SourceFile> files, Map<String, CleanedContent> pathToContent, ProgressFeedback progressFeedback) {
        files.forEach(sourceFile -> {
            LanguageAnalyzer analyzer = LanguageAnalyzerFactory.getInstance().getLanguageAnalyzer(sourceFile);
            progressFeedback.setText("Loading " + sourceFile.getRelativePath() + "...");
            pathToContent.put(sourceFile.getFile().getPath(), analyzer.cleanForDuplicationCalculations(sourceFile));
        });
    }

    public List<DuplicationInstance> getDuplicates() {
        return duplicates;
    }

    public int getTotalCleanedLinesOfCode() {
        return totalCleanedLinesOfCode;
    }

    public void setTotalCleanedLinesOfCode(int totalCleanedLinesOfCode) {
        this.totalCleanedLinesOfCode = totalCleanedLinesOfCode;
    }

    public int getNumberOfDuplicatedLines() {
        return numberOfDuplicatedLines;
    }
}
