/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.common.utils.ProcessingStopwatch;
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
    private final DuplicationAnalysisResults duplcationAnalysisResults;
    private final NamedSourceCodeAspect main;
    private CodeAnalysisResults analysisResults;
    private ProgressFeedback progressFeedback;

    public DuplicationAnalyzer(CodeAnalysisResults analysisResults) {
        this.duplcationAnalysisResults = analysisResults.getDuplicationAnalysisResults();
        this.codeConfiguration = analysisResults.getCodeConfiguration();
        this.metricsList = analysisResults.getMetricsList();
        this.start = analysisResults.getAnalysisStartTimeMs();
        this.textSummary = analysisResults.getTextSummary();
        this.analysisResults = analysisResults;
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

        ProcessingStopwatch.start("analysis/duplication/finding duplicated units");
        List<DuplicationInstance> duplicatedUnits = new UnitDuplicatesExtractor().findDuplicatedUnits(
                analysisResults.getUnitsAnalysisResults().getAllUnits(),
                analysisResults.getCodeConfiguration().getAnalysis().getMinDuplicationBlockLoc());

        analysisResults.getDuplicationAnalysisResults().setUnitDuplicates(duplicatedUnits);
        ProcessingStopwatch.end("analysis/duplication/finding duplicated units");


        ProcessingStopwatch.start("analysis/duplication/consolidating duplicates");
        ArrayList<DuplicationInstance> consolidatedDuplicationInstances = BUCKETED
                ? new ArrayList<>(mergeAndConsolidatePerFilePair(duplicates))
                : new ArrayList<>(consolidate(merge(duplicates)).values());
        consolidatedDuplicationInstances.sort((a, b) -> b.getBlockSize() - a.getBlockSize());
        duplcationAnalysisResults.setAllDuplicates(consolidatedDuplicationInstances);
        ProcessingStopwatch.end("analysis/duplication/consolidating duplicates");

        ProcessingStopwatch.start("analysis/duplication/counting duplicates");
        // Aggregated once and reused below (both the file count and the per-component/per-extension
        // aggregation need it); it used to be computed twice.
        List<SourceFileDuplication> duplicationPerSourceFile = DuplicationAggregator.getDuplicationPerSourceFile(duplicates);
        int numberOfDuplicates = consolidatedDuplicationInstances.size();
        int numberOfDuplicatedLines = DuplicationUtils.getNumberOfDuplicatedLines(duplicates);
        int totalNumberOfCleanedLines = DuplicationUtils.getTotalNumberOfCleanedLines(main.getSourceFiles());
        int numberOfFilesWithDuplicates = duplicationPerSourceFile.size();
        ProcessingStopwatch.end("analysis/duplication/counting duplicates");

        duplcationAnalysisResults.getOverallDuplication().setNumberOfDuplicates(numberOfDuplicates);
        duplcationAnalysisResults.getOverallDuplication().setCleanedLinesOfCode(totalNumberOfCleanedLines);
        duplcationAnalysisResults.getOverallDuplication().setDuplicatedLinesOfCode(numberOfDuplicatedLines);
        duplcationAnalysisResults.getOverallDuplication().setNumberOfFilesWithDuplicates(numberOfFilesWithDuplicates);

        addSystemDuplicationMetrics(numberOfDuplicates, numberOfDuplicatedLines, totalNumberOfCleanedLines, numberOfFilesWithDuplicates);

        List<AspectDuplication> duplicationPerLogicalComponent = DuplicationAggregator.getDuplicationPerLogicalComponent(codeConfiguration.getLogicalDecompositions(), main.getSourceFiles(),
                duplicationPerSourceFile);

        processLogicalDecompositions(progressFeedback, duplicationPerLogicalComponent);

        addMetricsAndSummary(progressFeedback, duplicationPerSourceFile);

        addMostFrequentDuplicates(duplicates);
        addLongestDuplicates(consolidatedDuplicationInstances);
    }

    private void addLongestDuplicates(List<DuplicationInstance> mergedConsolidated) {
        List<DuplicationInstance> filePairs = new ArrayList<>(mergedConsolidated);
        Collections.sort(filePairs, (o1, o2) -> -Integer.valueOf(o1.getBlockSize()).compareTo(o2.getBlockSize()));
        for (int i = 0; i < Math.min(codeConfiguration.getAnalysis().getMaxTopListSize(), filePairs.size()); i++) {
            duplcationAnalysisResults.getLongestDuplicates().add(filePairs.get(i));
        }
    }

    // Package-private for testing (same as getPairKey).
    void addMostFrequentDuplicates(List<DuplicationInstance> duplicates) {
        // Sort a copy by descending number of duplicated file blocks, then read from that sorted copy.
        // (Previously the sort was applied to a throwaway list and the unsorted original was iterated, so
        // "most frequent" was not actually ordered by frequency.)
        List<DuplicationInstance> sorted = new ArrayList<>(duplicates);
        Collections.sort(sorted, (o1, o2) -> -Integer.valueOf(o1.getDuplicatedFileBlocks().size()).compareTo(o2.getDuplicatedFileBlocks().size()));
        for (int i = 0; i < Math.min(codeConfiguration.getAnalysis().getMaxTopListSize(), sorted.size()); i++) {
            DuplicationInstance duplicate = sorted.get(i);
            if (duplicate.getDuplicatedFileBlocks().size() > 2) {
                duplcationAnalysisResults.getMostFrequentDuplicates().add(duplicate);
            }
        }
    }

    private void addMetricsAndSummary(ProgressFeedback progressFeedback, List<SourceFileDuplication> duplicationPerSourceFile) {
        List<ExtensionDuplication> duplicationPerExtension = DuplicationAggregator.getDuplicationPerExtension(main.getSourceFiles(), duplicationPerSourceFile);
        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "  - per extension:", start);
        duplicationPerExtension.forEach(extensionDuplication -> {
            String displayName = extensionDuplication.getExtension();
            duplcationAnalysisResults.getDuplicationPerExtension().add(new DuplicationMetric(displayName,
                    extensionDuplication.getCleanedLinesOfCode(), extensionDuplication.getDuplicatedLinesOfCode()));
            addExtensionDuplicationMetrics(extensionDuplication);
        });
    }

    private void processLogicalDecompositions(ProgressFeedback progressFeedback, List<AspectDuplication> duplicationPerLogicalComponent) {
        codeConfiguration.getLogicalDecompositions().forEach(logicalDecomposition -> {
            AnalysisUtils.detailedInfo(textSummary, progressFeedback, "  - per component:" + logicalDecomposition.getName(), start);
            ArrayList<DuplicationMetric> duplicationPerComponent = new ArrayList<>();
            duplcationAnalysisResults.getDuplicationPerComponent().add(duplicationPerComponent);
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

    // PROTOTYPE (SOK-dwflvoyo): -Dsokrates.duplication.bucketed=true selects the per-file-pair
    // implementation below instead of merge()+consolidate(). One jar, two code paths, so a baseline and
    // a prototype measurement cannot differ by build provenance.
    private static final boolean BUCKETED = "true".equals(System.getProperty("sokrates.duplication.bucketed"));

    /**
     * Behaviour-preserving replacement for {@link #consolidate}({@link #merge}(...)).
     *
     * merge() materialises one DuplicationInstance (plus two copied blocks and an ~89-byte composite
     * String key) for every ordered pair of blocks inside every instance, then consolidate() collapses
     * runs of adjacent pairs back into single instances. Every chain consolidate() walks is local to one
     * unordered file pair - getPairKey holds the two paths fixed and only advances the start lines - so
     * the global map is not needed. This groups the pairs by file pair, keeping each one as a packed long
     * rather than an object graph, and walks the chains inside each bucket.
     */
    private List<DuplicationInstance> mergeAndConsolidatePerFilePair(List<DuplicationInstance> duplicates) {
        Map<String, Integer> fileIds = new HashMap<>();
        Map<Long, DuplicatedFileBlock> blockByFileAndStart = new HashMap<>();
        Map<Long, LongArray> buckets = new HashMap<>();

        for (DuplicationInstance instance : duplicates) {
            List<DuplicatedFileBlock> blocks = instance.getDuplicatedFileBlocks();
            int count = blocks.size();
            int[] ids = new int[count];
            for (int i = 0; i < count; i++) {
                DuplicatedFileBlock block = blocks.get(i);
                String path = block.getSourceFile().getRelativePath();
                Integer id = fileIds.get(path);
                if (id == null) {
                    id = fileIds.size();
                    fileIds.put(path, id);
                }
                ids[i] = id;
                // Blocks sharing a file and a cleaned start line are interchangeable: every other field is
                // a function of those two plus the block size, which addFileToDuplicationInstance derives
                // from the same values. Keeping the first mirrors merge()'s first-writer-wins.
                blockByFileAndStart.putIfAbsent(pack(id, block.getCleanedStartLine()), block);
            }
            // merge() emits both orderings of every pair and lets the normalised key discard one of them;
            // emitting only i<j and normalising here reaches the same set at half the writes.
            for (int i = 0; i < count; i++) {
                for (int j = i + 1; j < count; j++) {
                    DuplicatedFileBlock block1 = blocks.get(i);
                    DuplicatedFileBlock block2 = blocks.get(j);
                    boolean inOrder = inPairKeyOrder(block1, block2);
                    DuplicatedFileBlock first = inOrder ? block1 : block2;
                    DuplicatedFileBlock second = inOrder ? block2 : block1;
                    long bucketKey = pack(inOrder ? ids[i] : ids[j], inOrder ? ids[j] : ids[i]);
                    long entry = pack(first.getCleanedStartLine(), second.getCleanedStartLine());
                    LongArray bucket = buckets.get(bucketKey);
                    if (bucket == null) {
                        bucket = new LongArray();
                        buckets.put(bucketKey, bucket);
                    }
                    bucket.add(entry);
                }
            }
        }

        List<DuplicationInstance> result = new ArrayList<>();
        for (Map.Entry<Long, LongArray> bucketEntry : buckets.entrySet()) {
            int fileA = (int) (bucketEntry.getKey() >>> 32);
            int fileB = (int) (long) bucketEntry.getKey();
            long[] entries = bucketEntry.getValue().sortedDistinct();
            for (long entry : entries) {
                int startA = (int) (entry >>> 32);
                int startB = (int) entry;
                // consolidate() absorbs forward from every surviving key, so a key starts a chain exactly
                // when its predecessor (both start lines one lower) is absent.
                if (contains(entries, pack(startA - 1, startB - 1))) {
                    continue;
                }
                int length = 1;
                while (contains(entries, pack(startA + length, startB + length))) {
                    length++;
                }
                result.add(chainInstance(blockByFileAndStart, fileA, fileB, startA, startB, length));
            }
        }
        return result;
    }

    private DuplicationInstance chainInstance(Map<Long, DuplicatedFileBlock> blockByFileAndStart,
                                              int fileA, int fileB, int startA, int startB, int length) {
        DuplicatedFileBlock blockA = copyOf(blockByFileAndStart.get(pack(fileA, startA)));
        DuplicatedFileBlock blockB = copyOf(blockByFileAndStart.get(pack(fileB, startB)));
        DuplicatedFileBlock lastA = blockByFileAndStart.get(pack(fileA, startA + length - 1));
        DuplicatedFileBlock lastB = blockByFileAndStart.get(pack(fileB, startB + length - 1));
        blockA.setEndLine(lastA.getEndLine());
        blockA.setCleanedEndLine(lastA.getCleanedEndLine());
        blockB.setEndLine(lastB.getEndLine());
        blockB.setCleanedEndLine(lastB.getCleanedEndLine());

        DuplicationInstance instance = new DuplicationInstance();
        instance.getDuplicatedFileBlocks().add(blockA);
        instance.getDuplicatedFileBlocks().add(blockB);
        instance.setBlockSize(blockA.getCleanedEndLine() - blockA.getCleanedStartLine() + 1);
        return instance;
    }

    // The ordering getPairKey applies: by relative path, and for two blocks in the same file by cleaned
    // start line. Kept as one predicate so the two implementations cannot drift apart.
    private boolean inPairKeyOrder(DuplicatedFileBlock block1, DuplicatedFileBlock block2) {
        int comparison = block1.getSourceFile().getRelativePath().compareTo(block2.getSourceFile().getRelativePath());
        return comparison < 0 || (comparison == 0 && block1.getCleanedStartLine() < block2.getCleanedStartLine());
    }

    private static long pack(int high, int low) {
        return (((long) high) << 32) | (low & 0xffffffffL);
    }

    private static boolean contains(long[] sorted, long value) {
        return Arrays.binarySearch(sorted, value) >= 0;
    }

    // A growable long array. The whole point of the bucketing is that a pair costs 8 bytes here instead
    // of the ~313-byte object graph merge() builds for it, so boxing them into a List<Long> would give
    // most of the memory straight back.
    private static final class LongArray {
        private long[] values = new long[8];
        private int size = 0;

        void add(long value) {
            if (size == values.length) {
                values = Arrays.copyOf(values, values.length * 2);
            }
            values[size++] = value;
        }

        long[] sortedDistinct() {
            long[] sorted = Arrays.copyOf(values, size);
            Arrays.sort(sorted);
            int distinct = 0;
            for (int i = 0; i < sorted.length; i++) {
                if (i == 0 || sorted[i] != sorted[i - 1]) {
                    sorted[distinct++] = sorted[i];
                }
            }
            return distinct == sorted.length ? sorted : Arrays.copyOf(sorted, distinct);
        }
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
                .value(componentDuplication.getCleanedLinesOfCode());

        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("DUPLICATION_PERCENTAGE") + suffix)
                .description("Duplication percentage")
                .value(100.0 * componentDuplication.getDuplicatedLinesOfCode() / componentDuplication.getCleanedLinesOfCode());
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

        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("UNIT_DUPLICATES_COUNT"))
                .description("Unit duplicates")
                .value(analysisResults.getDuplicationAnalysisResults().getUnitDuplicates().size());
    }

}
