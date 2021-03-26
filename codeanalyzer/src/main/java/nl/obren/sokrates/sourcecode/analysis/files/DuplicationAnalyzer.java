/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.analysis.AnalysisUtils;
import nl.obren.sokrates.sourcecode.analysis.Analyzer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.DuplicationAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.duplication.*;
import nl.obren.sokrates.sourcecode.metrics.DuplicationMetric;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;

import java.util.*;

public class DuplicationAnalyzer extends Analyzer {
    private final StringBuffer textSummary;
    private final CodeConfiguration codeConfiguration;
    private final MetricsList metricsList;
    private final long start;
    private final DuplicationAnalysisResults analysisResults;
    private final NamedSourceCodeAspect main;
    private ProgressFeedback progressFeedback;

    public DuplicationAnalyzer(CodeAnalysisResults analysisResults) {
        this.analysisResults = analysisResults.getDuplicationAnalysisResults();
        this.codeConfiguration = analysisResults.getCodeConfiguration();
        this.metricsList = analysisResults.getMetricsList();
        this.start = analysisResults.getAnalysisStartTimeMs();
        this.textSummary = analysisResults.getTextSummary();
        this.main = codeConfiguration.getMain();
    }

    public void analyze(ProgressFeedback progressFeedback) {
        if (skipDuplicationAnalysis()) {
            return;
        }

        this.progressFeedback = progressFeedback;
        progressFeedback.start();
        progressFeedback.setDetailedText("");
        AnalysisUtils.info(textSummary, progressFeedback, "Analysing duplication...", start);
        List<DuplicationInstance> duplicates = new DuplicationEngine().findDuplicates(main.getSourceFiles(),
                codeConfiguration.getAnalysis().getMinDuplicationBlockLoc(), new ProgressFeedback());

        Map<String, DuplicationInstance> mergedConsolidated = consolidate(merge(duplicates));
        ArrayList<DuplicationInstance> consolidatedDuplicationInstances = new ArrayList<>(mergedConsolidated.values());
        consolidatedDuplicationInstances.sort((a, b) -> b.getBlockSize() - a.getBlockSize());
        analysisResults.setAllDuplicates(consolidatedDuplicationInstances);

        // analysisResults.setAllDuplicates(duplicates);

        int numberOfDuplicates = duplicates.size();
        int numberOfDuplicatedLines = DuplicationUtils.getNumberOfDuplicatedLines(duplicates);
        int totalNumberOfCleanedLines = DuplicationUtils.getTotalNumberOfCleanedLines(main.getSourceFiles());
        int numberOfFilesWithDuplicates = DuplicationAggregator.getDuplicationPerSourceFile(duplicates).size();

        analysisResults.getOverallDuplication().setNumberOfDuplicates(numberOfDuplicates);
        analysisResults.getOverallDuplication().setCleanedLinesOfCode(totalNumberOfCleanedLines);
        analysisResults.getOverallDuplication().setDuplicatedLinesOfCode(numberOfDuplicatedLines);
        analysisResults.getOverallDuplication().setNumberOfFilesWithDuplicates(numberOfFilesWithDuplicates);

        addSystemDuplicationMetrics(numberOfDuplicates, numberOfDuplicatedLines, totalNumberOfCleanedLines, numberOfFilesWithDuplicates);

        List<SourceFileDuplication> duplicationPerSourceFile = DuplicationAggregator.getDuplicationPerSourceFile(duplicates);
        List<AspectDuplication> duplicationPerLogicalComponent = DuplicationAggregator.getDuplicationPerLogicalComponent(codeConfiguration.getLogicalDecompositions(), main.getSourceFiles(),
                duplicationPerSourceFile);

        processLogicalDecompositions(progressFeedback, duplicationPerLogicalComponent);

        addMetricsAndSummary(progressFeedback, duplicationPerSourceFile);

        addMostFrequentDuplicates(duplicates);
        addLongestDuplicates(mergedConsolidated);
    }

    private void addLongestDuplicates(Map<String, DuplicationInstance> mergedConsolidated) {
        List<DuplicationInstance> filePairs = new ArrayList<>(mergedConsolidated.values());
        Collections.sort(filePairs, (o1, o2) -> -Integer.valueOf(o1.getBlockSize()).compareTo(o2.getBlockSize()));
        for (int i = 0; i < Math.min(codeConfiguration.getAnalysis().getMaxTopListSize(), filePairs.size()); i++) {
            analysisResults.getLongestDuplicates().add(filePairs.get(i));
        }
    }

    private void addMostFrequentDuplicates(List<DuplicationInstance> duplicates) {
        Collections.sort(new ArrayList<>(duplicates), (o1, o2) -> -Integer.valueOf(o1.getDuplicatedFileBlocks().size()).compareTo(o2.getDuplicatedFileBlocks().size()));
        for (int i = 0; i < Math.min(codeConfiguration.getAnalysis().getMaxTopListSize(), duplicates.size()); i++) {
            DuplicationInstance duplicate = duplicates.get(i);
            if (duplicate.getDuplicatedFileBlocks().size() > 2) {
                analysisResults.getMostFrequentDuplicates().add(duplicate);
            }
        }
    }

    private void addMetricsAndSummary(ProgressFeedback progressFeedback, List<SourceFileDuplication> duplicationPerSourceFile) {
        List<ExtensionDuplication> duplicationPerExtension = DuplicationAggregator.getDuplicationPerExtension(main.getSourceFiles(), duplicationPerSourceFile);
        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "  - per extension:", start);
        duplicationPerExtension.forEach(extensionDuplication -> {
            String displayName = extensionDuplication.getExtension();
            analysisResults.getDuplicationPerExtension().add(new DuplicationMetric(displayName,
                    extensionDuplication.getCleanedLinesOfCode(), extensionDuplication.getDuplicatedLinesOfCode()));
            addExtensionDuplicationMetrics(extensionDuplication);
        });
    }

    private void processLogicalDecompositions(ProgressFeedback progressFeedback, List<AspectDuplication> duplicationPerLogicalComponent) {
        codeConfiguration.getLogicalDecompositions().forEach(logicalDecomposition -> {
            AnalysisUtils.detailedInfo(textSummary, progressFeedback, "  - per component:" + logicalDecomposition.getName(), start);
            ArrayList<DuplicationMetric> duplicationPerComponent = new ArrayList<>();
            analysisResults.getDuplicationPerComponent().add(duplicationPerComponent);
            duplicationPerLogicalComponent.stream()
                    .filter(componentDuplication -> componentDuplication.getAspect().getFiltering().equalsIgnoreCase(logicalDecomposition.getName()))
                    .forEach(componentDuplication -> {
                        String displayName = componentDuplication.getAspect().getName();
                        String key = displayName;
                        duplicationPerComponent.add(new DuplicationMetric(key,
                                componentDuplication.getCleanedLinesOfCode(), componentDuplication.getDuplicatedLinesOfCode()));

                        addComponentDuplicationMetrics(logicalDecomposition, componentDuplication, displayName);
                    });
        });
    }

    private Map<String, DuplicationInstance> consolidate(Map<String, DuplicationInstance> merged) {
        Map<String, DuplicationInstance> mergedConsolidated = new HashMap<>(merged);
        merged.keySet().forEach(key -> {
            if (!mergedConsolidated.containsKey(key)) {
                return;
            }
            DuplicationInstance currentInstance = mergedConsolidated.get(key);
            DuplicatedFileBlock file1 = currentInstance.getDuplicatedFileBlocks().get(0);
            DuplicatedFileBlock file2 = currentInstance.getDuplicatedFileBlocks().get(1);
            int offset = 1;
            while (true) {
                String nextKey = getPairKey(file1, file2, offset);
                DuplicationInstance nextInstance = mergedConsolidated.get(nextKey);
                if (nextInstance != null) {
                    DuplicatedFileBlock nextBlock1 = nextInstance.getDuplicatedFileBlocks().get(0);
                    DuplicatedFileBlock nextBlock2 = nextInstance.getDuplicatedFileBlocks().get(1);
                    if (!nextBlock1.getSourceFile().getRelativePath().equals(file1.getSourceFile().getRelativePath())) {
                        DuplicatedFileBlock temp = nextBlock1;
                        nextBlock1 = nextBlock2;
                        nextBlock2 = temp;
                    }

                    file1.setEndLine(nextBlock1.getEndLine());
                    file1.setCleanedEndLine(nextBlock1.getCleanedEndLine());

                    file2.setEndLine(nextBlock2.getEndLine());
                    file2.setCleanedEndLine(nextBlock2.getCleanedEndLine());

                    mergedConsolidated.remove(nextKey);
                    currentInstance.setBlockSize(file1.getCleanedEndLine() - file1.getCleanedStartLine() + 1);
                    offset += 1;
                } else {
                    break;
                }
            }
        });
        return mergedConsolidated;
    }

    private Map<String, DuplicationInstance> merge(List<DuplicationInstance> duplicates) {
        Map<String, DuplicationInstance> merged = new HashMap<>();
        duplicates.forEach(d -> {
            d.getDuplicatedFileBlocks().forEach(file1 -> {
                d.getDuplicatedFileBlocks().stream()
                        .filter(file2 -> !(file1 == file2 && file1.getStartLine() == file2.getStartLine()))
                        .forEach(file2 -> {
                            String key = getPairKey(file1, file2, 0);
                            if (!merged.containsKey(key)) {
                                DuplicationInstance duplicationInstance = new DuplicationInstance();
                                String path1 = file1.getSourceFile().getRelativePath();
                                String path2 = file2.getSourceFile().getRelativePath();
                                if (path1.compareTo(path2) <= 0) {
                                    duplicationInstance.getDuplicatedFileBlocks().add(copyOf(file1));
                                    duplicationInstance.getDuplicatedFileBlocks().add(copyOf(file2));
                                } else {
                                    duplicationInstance.getDuplicatedFileBlocks().add(copyOf(file2));
                                    duplicationInstance.getDuplicatedFileBlocks().add(copyOf(file1));
                                }
                                duplicationInstance.setBlockSize(d.getBlockSize());
                                merged.put(key, duplicationInstance);
                            }
                        });
            });
        });
        return merged;
    }

    private DuplicatedFileBlock copyOf(DuplicatedFileBlock block) {
        DuplicatedFileBlock newBlock = new DuplicatedFileBlock();
        newBlock.setStartLine(block.getStartLine());
        newBlock.setEndLine(block.getEndLine());
        newBlock.setCleanedStartLine(block.getCleanedStartLine());
        newBlock.setCleanedEndLine(block.getCleanedEndLine());
        newBlock.setSourceFile(block.getSourceFile());
        newBlock.setSourceFileCleanedLinesOfCode(block.getSourceFileCleanedLinesOfCode());
        return newBlock;
    }

    public String getPairKey(DuplicatedFileBlock block1, DuplicatedFileBlock block2, int offset) {
        String relPath1 = block1.getSourceFile().getRelativePath();
        String relPath2 = block2.getSourceFile().getRelativePath();
        DuplicatedFileBlock file1;
        DuplicatedFileBlock file2;
        String path1;
        String path2;

        if (relPath1.compareTo(relPath2) < 0) {
            file1 = block1;
            file2 = block2;
            path1 = relPath1;
            path2 = relPath2;
        } else if (relPath1.compareTo(relPath2) == 0 && block1.getCleanedStartLine() < block2.getCleanedStartLine()) {
            file1 = block1;
            file2 = block2;
            path1 = relPath1;
            path2 = relPath2;
        } else {
            file1 = block2;
            file2 = block1;
            path1 = relPath2;
            path2 = relPath1;
        }

        return path1 + ":" + (file1.getCleanedStartLine() + offset) + "::" + path2 + ":" + (file2.getCleanedStartLine() + offset) + "";
    }

    private boolean skipDuplicationAnalysis() {
        return codeConfiguration.getAnalysis().isSkipDuplication() || main.getLinesOfCode() > codeConfiguration.getAnalysis().getLocDuplicationThreshold();
    }

    private void addExtensionDuplicationMetrics(ExtensionDuplication extensionDuplication) {
        String suffix = "_" + extensionDuplication.getExtension();
        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("DUPLICATION_NUMBER_OF_DUPLICATED_LINES") + suffix)
                .description("Number of duplicated lines")
                .value(extensionDuplication.getDuplicatedLinesOfCode());

        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("DUPLICATION_NUMBER_OF_CLEANED_LINES") + suffix)
                .description("Number of lines after cleaning for duplication calculations")
                .value(extensionDuplication.getCleanedLinesOfCode());

        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("DUPLICATION_PERCENTAGE") + suffix)
                .description("Duplication percentage")
                .value(100.0 * extensionDuplication.getDuplicatedLinesOfCode() / extensionDuplication.getCleanedLinesOfCode());

        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "     - \"" + extensionDuplication.getExtension() + "\": " + extensionDuplication.getDuplicatedLinesOfCode() + " duplicated lines vs. " +
                extensionDuplication.getCleanedLinesOfCode
                        () + " total lines", start);
    }

    private void addComponentDuplicationMetrics(LogicalDecomposition logicalDecomposition, AspectDuplication componentDuplication, String displayName) {
        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "     - \"" + displayName + "\": " + componentDuplication.getDuplicatedLinesOfCode() + " duplicated lines vs. " + componentDuplication
                .getCleanedLinesOfCode
                        () + " total" +
                " lines", start);

        String suffix = "_" + logicalDecomposition.getName() + "_" + displayName;

        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("DUPLICATION_NUMBER_OF_DUPLICATED_LINES") + suffix)
                .description("Number of duplicated lines")
                .value(componentDuplication.getDuplicatedLinesOfCode());

        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("DUPLICATION_NUMBER_OF_CLEANED_LINES") + suffix)
                .description("Number of lines after cleaning for duplication calculations")
                .value(componentDuplication.getDuplicatedLinesOfCode());

        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("DUPLICATION_PERCENTAGE") + suffix)
                .description("Duplication percentage")
                .value(100.0 * componentDuplication.getDuplicatedLinesOfCode() / componentDuplication.getDuplicatedLinesOfCode());
    }

    private void addSystemDuplicationMetrics(int numberOfDuplicates, int numberOfDuplicatedLines, int totalNumberOfCleanedLines, int numberOfFilesWithDuplicates) {
        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "  - found " + numberOfDuplicates + " duplicates (" + numberOfDuplicatedLines + " duplicated lines vs. "
                + totalNumberOfCleanedLines + " cleaned lines) in "
                + numberOfFilesWithDuplicates + " files", start);

        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("DUPLICATION_NUMBER_OF_DUPLICATES"))
                .description("Number of duplicates")
                .value(numberOfDuplicates);

        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("DUPLICATION_NUMBER_OF_FILES_WITH_DUPLICATES"))
                .description("Number of files with duplicates")
                .value(numberOfFilesWithDuplicates);

        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("DUPLICATION_NUMBER_OF_DUPLICATED_LINES"))
                .description("Number of duplicated lines")
                .value(numberOfDuplicatedLines);

        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("DUPLICATION_NUMBER_OF_CLEANED_LINES"))
                .description("Number of lines after cleaning for duplication calculations")
                .value(totalNumberOfCleanedLines);

        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("DUPLICATION_PERCENTAGE"))
                .description("Duplication percentage")
                .value(100.0 * numberOfDuplicatedLines / totalNumberOfCleanedLines);
    }

}
