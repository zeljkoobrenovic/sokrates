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
import nl.obren.sokrates.sourcecode.metrics.Metric;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DuplicationAnalyzer extends Analyzer {
    public static final int LIST_LIMIT = 50;
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
        if (codeConfiguration.getAnalysis().isSkipDuplication()) {
            return;
        }

        this.progressFeedback = progressFeedback;
        progressFeedback.start();
        progressFeedback.setDetailedText("");
        AnalysisUtils.info(textSummary, progressFeedback, "Analysing duplication...", start);
        List<DuplicationInstance> duplicates = new DuplicationEngine().findDuplicates(main.getSourceFiles(), new ProgressFeedback());

        analysisResults.setAllDuplicates(duplicates);

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

        codeConfiguration.getLogicalDecompositions().forEach(logicalDecomposition -> {
            AnalysisUtils.detailedInfo(textSummary, progressFeedback, "  - per component:" + logicalDecomposition.getName(), start);
            ArrayList<DuplicationMetric> duplicationPerComponent = new ArrayList<>();
            analysisResults.getDuplicationPerComponent().add(duplicationPerComponent);
            duplicationPerLogicalComponent.stream()
                    .filter(componentDuplication -> componentDuplication.getAspect().getFiltering().equalsIgnoreCase(logicalDecomposition.getName()))
                    .forEach(componentDuplication -> {
                        String group = componentDuplication.getAspect().getFiltering();
                        String displayName = componentDuplication.getAspect().getName();
                        String key = displayName;
                        duplicationPerComponent.add(new DuplicationMetric(key,
                                componentDuplication.getCleanedLinesOfCode(), componentDuplication.getDuplicatedLinesOfCode()));

                        addComponentDuplicationMetrics(logicalDecomposition, componentDuplication, displayName);
                    });
        });

        List<ExtensionDuplication> duplicationPerExtension = DuplicationAggregator.getDuplicationPerExtension(main.getSourceFiles(), duplicationPerSourceFile);
        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "  - per extension:", start);
        duplicationPerExtension.forEach(extensionDuplication -> {
            String displayName = extensionDuplication.getExtension();
            analysisResults.getDuplicationPerExtension().add(new DuplicationMetric(displayName,
                    extensionDuplication.getCleanedLinesOfCode(), extensionDuplication.getDuplicatedLinesOfCode()));
            addExtensionDuplicationMetrics(extensionDuplication);
        });

        Collections.sort(duplicates, (o1, o2) -> -new Integer(o1.getDuplicatedFileBlocks().size()).compareTo(o2.getDuplicatedFileBlocks().size()));
        for (int i = 0; i < Math.min(LIST_LIMIT, duplicates.size()); i++) {
            DuplicationInstance duplicate = duplicates.get(i);
            if (duplicate.getDuplicatedFileBlocks().size() > 2) {
                analysisResults.getMostFrequentDuplicates().add(duplicate);
            }
        }

        Collections.sort(duplicates, (o1, o2) -> -new Integer(o1.getBlockSize()).compareTo(o2.getBlockSize()));
        for (int i = 0; i < Math.min(LIST_LIMIT, duplicates.size()); i++) {
            analysisResults.getLongestDuplicates().add(duplicates.get(i));
        }
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
