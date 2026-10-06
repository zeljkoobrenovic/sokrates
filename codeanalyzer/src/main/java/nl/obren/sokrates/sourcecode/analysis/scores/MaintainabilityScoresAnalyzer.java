/*
 * Copyright (c) 2026 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.scores;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.AnalysisUtils;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.LogicalDecompositionAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.core.MaintainabilityScoresConfig;
import nl.obren.sokrates.sourcecode.core.ScoreFrameworkConfig;
import nl.obren.sokrates.sourcecode.filehistory.CommitInfo;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import nl.obren.sokrates.sourcecode.metrics.DuplicationMetric;
import nl.obren.sokrates.sourcecode.metrics.Metric;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.function.Function;

/**
 * Computes the Human and AI maintainability scores ({@link MaintainabilityScores}) from the metrics the other
 * analyzers produced; run after them, before the controls, so goals and controls can use the score metrics.
 * <p>
 * Each sub-score maps one measure to 0–10 by piecewise-linear interpolation over fixed anchor points (the shares
 * are taken above the configured "high" risk thresholds, so they follow the analysis' thresholds). A sub-score
 * whose analysis did not run (no units, no history, no duplication) is left out and
 * the weights of the others carry the score. The two scores weigh the same sub-scores differently
 * ({@link #HUMAN_WEIGHTS}, {@link #AI_WEIGHTS}): people struggle most with complex logic and with knowledge held
 * by one person; an agent pays for every line it reads (large files, scattered changes, the lines read per
 * change), copies duplicates, and needs tests to check its work.
 * <p>
 * The total is the weighted geometric mean of the sub-scores (so a strong sub-score cannot hide a weak one the
 * way an average would), capped at the weakest sub-score about the code (not the knowledge spread, which is about
 * people) + {@link #WEAKEST_LINK_MARGIN}.
 * <p>
 * With {@code analysis.maintainabilityScores.useCustomFramework}, the configured framework ({@link ScoreFrameworkConfig})
 * replaces the sub-scores, anchors, weights, cap and grades ({@link #customFramework}).
 */
public class MaintainabilityScoresAnalyzer {
    private static final Log LOG = LogFactory.getLog(MaintainabilityScoresAnalyzer.class);

    public static final String VOLUME = "volume";
    public static final String DUPLICATION = "duplication";
    public static final String UNIT_SIZE = "unitSize";
    public static final String UNIT_COMPLEXITY = "unitComplexity";
    public static final String FILE_SIZE = "fileSize";
    public static final String FILE_COMPLEXITY = "fileComplexity";
    public static final String TEST_CODE = "testCode";
    public static final String CHANGE_ENTROPY = "changeEntropy";
    public static final String CONTEXT_PER_CHANGE = "contextPerChange";
    public static final String KNOWLEDGE = "knowledge";

    public static final List<String> KEYS = Arrays.asList(VOLUME, DUPLICATION, UNIT_SIZE, UNIT_COMPLEXITY, FILE_SIZE,
            FILE_COMPLEXITY, TEST_CODE, CHANGE_ENTROPY, CONTEXT_PER_CHANGE, KNOWLEDGE);

    public static final Map<String, Double> HUMAN_WEIGHTS = weights(1, 1, 1.5, 2, 0.75, 1, 0.75, 1, 0.5, 1.5);
    public static final Map<String, Double> AI_WEIGHTS = weights(0.75, 1.5, 1, 1, 1.75, 0.75, 1.75, 1.5, 2, 0);

    static final double WEAKEST_LINK_MARGIN = 4;
    // A zero sub-score would zero the geometric mean whatever the rest is.
    static final double SCORE_FLOOR = 0.5;
    static final int WINDOW_DAYS = 365;
    // Commits touching more main files than this are bulk changes (renames, formatting, imports), not tasks.
    static final int MAX_FILES_PER_COMMIT = 300;
    static final int MIN_COMMITS = 10;

    // {measure, score} anchors, measures ascending.
    static final double[][] VOLUME_ANCHORS = {{0, 10}, {10_000, 9}, {50_000, 7.5}, {200_000, 5}, {1_000_000, 2}, {5_000_000, 0}};
    static final double[][] DUPLICATION_ANCHORS = {{0, 10}, {3, 9}, {5, 8}, {10, 6}, {20, 3}, {40, 0}};
    static final double[][] UNIT_SIZE_ANCHORS = {{0, 10}, {10, 8}, {25, 5}, {50, 2}, {75, 0}};
    static final double[][] UNIT_COMPLEXITY_ANCHORS = {{0, 10}, {5, 8}, {15, 5}, {30, 2}, {50, 0}};
    static final double[][] FILE_SIZE_ANCHORS = {{0, 10}, {10, 8}, {30, 5}, {50, 2}, {75, 0}};
    static final double[][] FILE_COMPLEXITY_ANCHORS = {{0, 10}, {10, 8}, {25, 5}, {50, 2}, {75, 0}};
    static final double[][] TEST_CODE_ANCHORS = {{0, 0}, {0.1, 3}, {0.3, 6}, {0.6, 8.5}, {1, 10}};
    static final double[][] CHANGE_ENTROPY_ANCHORS = {{0, 10}, {0.25, 8.5}, {0.5, 7}, {1, 4}, {2, 1}, {3, 0}};
    static final double[][] CONTEXT_ANCHORS = {{0, 10}, {500, 9}, {2_000, 7}, {5_000, 5}, {10_000, 3}, {25_000, 1}, {50_000, 0}};
    static final double[][] KNOWLEDGE_ANCHORS = {{1, 3}, {2, 5.5}, {3, 7.5}, {5, 10}};
    // Lowest totals for the grades A, B, C and D.
    static final double[] GRADE_THRESHOLDS = {8, 6.5, 5, 3.5};

    /** How sub-scores combine into a total: the weakest-link cap and the grades. */
    public static class Rules {
        // Negative: no cap.
        double weakestLinkMargin = WEAKEST_LINK_MARGIN;
        Set<String> capExcludes = new HashSet<>(Collections.singletonList(KNOWLEDGE));
        double[] gradeThresholds = GRADE_THRESHOLDS;
    }

    /** A configured framework resolved against this analysis. */
    static class Framework {
        final List<SubScore> subScores = new ArrayList<>();
        final Map<String, Double> humanWeights = new LinkedHashMap<>();
        final Map<String, Double> aiWeights = new LinkedHashMap<>();
        final Rules rules = new Rules();
    }

    private final CodeAnalysisResults results;

    public MaintainabilityScoresAnalyzer(CodeAnalysisResults results) {
        this.results = results;
    }

    public void analyze() {
        MaintainabilityScoresConfig config = results.getCodeConfiguration().getAnalysis().getMaintainabilityScores();
        if (!config.isEnabled() || results.getMainAspectAnalysisResults().getLinesOfCode() == 0) {
            return;
        }
        ChangeStats changes = changeStats();
        List<SubScore> measured = measure(changes);
        MaintainabilityScores scores = new MaintainabilityScores();
        if (config.isUseCustomFramework()) {
            Framework framework = customFramework(measured, config.getCustomFramework(), results.getMetricsList()::getMetricById);
            if (framework.subScores.isEmpty()) {
                LOG.warn("maintainabilityScores: the custom framework has no sub-score that could be measured; no scores");
                return;
            }
            scores.setHuman(combine(framework.subScores, framework.humanWeights, framework.rules));
            scores.setAi(combine(framework.subScores, framework.aiWeights, framework.rules));
            scores.setFramework(MaintainabilityScores.CUSTOM);
        } else {
            scores.setHuman(combine(measured, merge(HUMAN_WEIGHTS, config.getHumanWeights())));
            scores.setAi(combine(measured, merge(AI_WEIGHTS, config.getAiWeights())));
        }
        scores.setContextLinesPerChange((int) Math.round(changes.contextLines));
        scores.setChangesMeasured(changes.commits);
        results.setMaintainabilityScores(scores);

        results.getMetricsList().addMetric().id(AnalysisUtils.getMetricId("MAINTAINABILITY_SCORE_HUMAN"))
                .description("Human maintainability score (0-10)").value(scores.getHuman().getValue());
        results.getMetricsList().addMetric().id(AnalysisUtils.getMetricId("MAINTAINABILITY_SCORE_AI"))
                .description("AI maintainability score (0-10)").value(scores.getAi().getValue());
        if (changes.commits > 0) {
            results.getMetricsList().addMetric().id(AnalysisUtils.getMetricId("AI_CONTEXT_LINES_PER_CHANGE"))
                    .description("Lines of main code read per change, past year").value(scores.getContextLinesPerChange());
        }
    }

    private List<SubScore> measure(ChangeStats changes) {
        List<SubScore> measured = new ArrayList<>();
        int mainLoc = results.getMainAspectAnalysisResults().getLinesOfCode();
        measured.add(new SubScore(VOLUME, "Volume", mainLoc, String.format(Locale.US, "%,d lines of main code", mainLoc),
                interpolate(mainLoc, VOLUME_ANCHORS)));

        DuplicationMetric duplication = results.getDuplicationAnalysisResults().getOverallDuplication();
        if (!results.skipDuplicationAnalysis() && duplication != null && duplication.getCleanedLinesOfCode() > 0) {
            double percentage = duplication.getDuplicationPercentage().doubleValue();
            measured.add(new SubScore(DUPLICATION, "Duplication", percentage, percentage(percentage) + " of main code duplicated",
                    interpolate(percentage, DUPLICATION_ANCHORS)));
        }
        if (results.getUnitsAnalysisResults().getTotalNumberOfUnits() > 0) {
            RiskDistributionStats unitSize = results.getUnitsAnalysisResults().getUnitSizeRiskDistribution();
            addShare(measured, UNIT_SIZE, "Unit size", unitSize, "of unit code in units > " + unitSize.getHighRiskThreshold() + " lines", UNIT_SIZE_ANCHORS);
            RiskDistributionStats complexity = results.getUnitsAnalysisResults().getConditionalComplexityRiskDistribution();
            addShare(measured, UNIT_COMPLEXITY, "Unit complexity", complexity, "of unit code in units with McCabe > " + complexity.getHighRiskThreshold(), UNIT_COMPLEXITY_ANCHORS);
        }
        RiskDistributionStats fileSize = results.getFilesAnalysisResults().getOverallFileSizeDistribution();
        addShare(measured, FILE_SIZE, "File size", fileSize, fileSize == null ? "" : "of main code in files > " + fileSize.getHighRiskThreshold() + " lines", FILE_SIZE_ANCHORS);
        RiskDistributionStats fileComplexity = results.getFilesAnalysisResults().getOverallFileComplexityDistribution();
        addShare(measured, FILE_COMPLEXITY, "File complexity", fileComplexity, fileComplexity == null ? "" : "of main code in files with McCabe sum > " + fileComplexity.getHighRiskThreshold(), FILE_COMPLEXITY_ANCHORS);

        int testLoc = results.getTestAspectAnalysisResults().getLinesOfCode();
        double testRatio = (double) testLoc / mainLoc;
        measured.add(new SubScore(TEST_CODE, "Test code", testRatio,
                String.format(Locale.US, "%,d test lines per 100 main lines", Math.round(100 * testRatio)), interpolate(testRatio, TEST_CODE_ANCHORS)));

        if (changes.commits >= MIN_COMMITS) {
            measured.add(new SubScore(CHANGE_ENTROPY, "Change entropy", changes.entropy,
                    String.format(Locale.US, "%.2f bits per change across %s (past year)", changes.entropy, changes.componentsLabel),
                    interpolate(changes.entropy, CHANGE_ENTROPY_ANCHORS)));
            measured.add(new SubScore(CONTEXT_PER_CHANGE, "Context per change", changes.contextLines,
                    String.format(Locale.US, "~%,d lines (~%,d tokens) read per change (past year)",
                            Math.round(changes.contextLines), Math.round(changes.contextLines * 10)),
                    interpolate(changes.contextLines, CONTEXT_ANCHORS)));
        }

        List<Integer> commitsPerPerson = new ArrayList<>();
        results.getContributorsAnalysisResults().getContributors().stream()
                .filter(c -> !c.isBot() && c.getCommitsCount365Days() > 0)
                .forEach(c -> commitsPerPerson.add(c.getCommitsCount365Days()));
        if (!commitsPerPerson.isEmpty()) {
            int holders = knowledgeHolders(commitsPerPerson);
            measured.add(new SubScore(KNOWLEDGE, "Knowledge spread", holders,
                    holders + (holders == 1 ? " person makes" : " people make") + " half of the commits (past year)",
                    interpolate(holders, KNOWLEDGE_ANCHORS)));
        }
        return measured;
    }

    /**
     * Resolves a configured framework: each sub-score is a built-in measure (by key, from {@code measured}; one whose
     * analysis did not run is left out) or an analysis metric (by id, via {@code metrics}), scored on its own anchors
     * when given. Invalid entries are logged and skipped; rules left out keep the built-in ones.
     */
    static Framework customFramework(List<SubScore> measured, ScoreFrameworkConfig config, Function<String, Metric> metrics) {
        Framework framework = new Framework();
        for (ScoreFrameworkConfig.SubScoreConfig definition : config.getSubScores()) {
            String key = definition.getKey().trim();
            if (key.isEmpty() || framework.humanWeights.containsKey(key)) {
                LOG.warn("maintainabilityScores: skipping a custom sub-score with " + (key.isEmpty() ? "no key" : "the duplicate key \"" + key + "\""));
                continue;
            }
            double[][] anchors = anchors(definition.getAnchors());
            if (!definition.getAnchors().isEmpty() && anchors.length == 0) {
                LOG.warn("maintainabilityScores: skipping \"" + key + "\", its anchors are not [measure, score] number pairs");
                continue;
            }
            String label = definition.getLabel().isBlank() ? null : definition.getLabel();
            SubScore subScore;
            if (!definition.getMetric().isBlank()) {
                Metric metric = metrics.apply(definition.getMetric().trim());
                if (anchors.length == 0) {
                    LOG.warn("maintainabilityScores: skipping \"" + key + "\", a metric sub-score needs anchors");
                    continue;
                }
                if (metric == null || metric.getValue() == null) {
                    LOG.warn("maintainabilityScores: skipping \"" + key + "\", no metric \"" + definition.getMetric() + "\" in this analysis");
                    continue;
                }
                double value = metric.getValue().doubleValue();
                String what = definition.getDescription().isBlank() ? definition.getMetric().trim() : definition.getDescription();
                subScore = new SubScore(key, label != null ? label : key, value, number(value) + " " + what, interpolate(value, anchors));
            } else {
                if (!KEYS.contains(key)) {
                    LOG.warn("maintainabilityScores: skipping \"" + key + "\", neither a built-in sub-score (" + String.join(", ", KEYS) + ") nor a metric");
                    continue;
                }
                SubScore builtIn = measured.stream().filter(s -> s.getKey().equals(key)).findFirst().orElse(null);
                if (builtIn == null) {
                    continue;
                }
                subScore = new SubScore(key, label != null ? label : builtIn.getLabel(), builtIn.getMeasure(), builtIn.getMeasureText(),
                        anchors.length > 0 ? interpolate(builtIn.getMeasure(), anchors) : builtIn.getScore());
            }
            framework.subScores.add(subScore);
            framework.humanWeights.put(key, Math.max(0, definition.getHumanWeight()));
            framework.aiWeights.put(key, Math.max(0, definition.getAiWeight()));
        }
        if (config.getWeakestLinkMargin() != null) {
            framework.rules.weakestLinkMargin = config.getWeakestLinkMargin();
        }
        if (config.getCapExcludes() != null) {
            framework.rules.capExcludes = new HashSet<>(config.getCapExcludes());
        }
        List<Double> grades = config.getGradeThresholds();
        if (grades != null) {
            boolean valid = grades.size() == 4 && grades.stream().allMatch(Objects::nonNull);
            for (int i = 1; valid && i < 4; i++) {
                valid = grades.get(i) <= grades.get(i - 1);
            }
            if (valid) {
                framework.rules.gradeThresholds = grades.stream().mapToDouble(Double::doubleValue).toArray();
            } else {
                LOG.warn("maintainabilityScores: gradeThresholds needs 4 descending numbers (A, B, C, D); using the built-in ones");
            }
        }
        return framework;
    }

    // Valid [measure, score] pairs, sorted by measure, scores kept within 0-10; empty if there are none.
    static double[][] anchors(List<List<Double>> points) {
        List<double[]> valid = new ArrayList<>();
        for (List<Double> point : points) {
            if (point != null && point.size() == 2 && point.get(0) != null && point.get(1) != null
                    && Double.isFinite(point.get(0)) && Double.isFinite(point.get(1))) {
                valid.add(new double[]{point.get(0), Math.max(0, Math.min(10, point.get(1)))});
            }
        }
        if (valid.size() < points.size()) {
            return new double[0][];
        }
        valid.sort(Comparator.comparingDouble(p -> p[0]));
        return valid.toArray(new double[0][]);
    }

    private static String number(double value) {
        return value == Math.rint(value) && Math.abs(value) < 1e15
                ? String.format(Locale.US, "%,d", (long) value) : String.format(Locale.US, "%,.2f", value);
    }

    private static void addShare(List<SubScore> measured, String key, String label, RiskDistributionStats stats, String text, double[][] anchors) {
        if (stats == null || stats.getTotalValue() <= 0) {
            return;
        }
        double share = 100.0 * (stats.getHighRiskValue() + stats.getVeryHighRiskValue()) / stats.getTotalValue();
        measured.add(new SubScore(key, label, share, percentage(share) + " " + text, interpolate(share, anchors)));
    }

    // The first logical decomposition with at least two components, or null.
    private LogicalDecompositionAnalysisResults componentDecomposition() {
        return results.getLogicalDecompositionsAnalysisResults().stream()
                .filter(d -> d.getComponents().size() >= 2).findFirst().orElse(null);
    }

    static class ChangeStats {
        int commits;
        double entropy;
        double contextLines;
        String componentsLabel = "folders";
    }

    private ChangeStats changeStats() {
        List<SourceFile> mainFiles = results.getMainAspectAnalysisResults().getAspect() == null
                ? Collections.emptyList() : results.getMainAspectAnalysisResults().getAspect().getSourceFiles();
        Set<String> bots = new HashSet<>();
        results.getContributorsAnalysisResults().getContributors().stream().filter(Contributor::isBot)
                .forEach(c -> bots.add(c.getEmail()));

        LogicalDecompositionAnalysisResults decomposition = componentDecomposition();
        Map<String, String> componentByPath = new HashMap<>();
        if (decomposition != null) {
            for (AspectAnalysisResults component : decomposition.getComponents()) {
                if (component.getAspect() != null) {
                    component.getAspect().getSourceFiles().forEach(f -> componentByPath.putIfAbsent(f.getRelativePath(), component.getName()));
                }
            }
        }
        Function<SourceFile, String> componentOf = file -> componentByPath.getOrDefault(file.getRelativePath(), topFolder(file.getRelativePath()));
        ChangeStats stats = changeStats(mainFiles, componentOf, bots);
        if (decomposition != null && !componentByPath.isEmpty()) {
            stats.componentsLabel = "\"" + decomposition.getKey() + "\" components";
        }
        return stats;
    }

    /**
     * Groups the main files' changes of the past year by commit (bots and bulk commits left out) and returns the
     * mean Shannon entropy (bits) of each commit's files over their components — 0 when a change stays in one
     * component, 1 when it is split evenly over two — and the mean current lines of code of the files a commit
     * touched, what a change makes one read.
     */
    static ChangeStats changeStats(List<SourceFile> mainFiles, Function<SourceFile, String> componentOf, Set<String> bots) {
        Map<String, List<SourceFile>> filesByCommit = new HashMap<>();
        for (SourceFile file : mainFiles) {
            FileModificationHistory history = file.getFileModificationHistory();
            if (history == null) {
                continue;
            }
            Set<String> seen = new HashSet<>();
            for (CommitInfo commit : history.getCommits()) {
                if (bots.contains(commit.getEmail()) || !DateUtils.isDateWithinRange(commit.getDate(), WINDOW_DAYS)
                        || !seen.add(commit.getId())) {
                    continue;
                }
                filesByCommit.computeIfAbsent(commit.getId(), id -> new ArrayList<>()).add(file);
            }
        }
        ChangeStats stats = new ChangeStats();
        double entropySum = 0;
        double linesSum = 0;
        for (List<SourceFile> files : filesByCommit.values()) {
            if (files.size() > MAX_FILES_PER_COMMIT) {
                continue;
            }
            Map<String, Integer> perComponent = new HashMap<>();
            files.forEach(f -> perComponent.merge(componentOf.apply(f), 1, Integer::sum));
            entropySum += entropy(perComponent.values(), files.size());
            linesSum += files.stream().mapToInt(SourceFile::getLinesOfCode).sum();
            stats.commits++;
        }
        if (stats.commits > 0) {
            stats.entropy = entropySum / stats.commits;
            stats.contextLines = linesSum / stats.commits;
        }
        return stats;
    }

    static double entropy(Collection<Integer> counts, int total) {
        double entropy = 0;
        for (int count : counts) {
            double p = (double) count / total;
            entropy -= p * Math.log(p) / Math.log(2);
        }
        return Math.max(0, entropy);
    }

    /** The smallest number of people who together made at least half of the commits. */
    static int knowledgeHolders(List<Integer> commitsPerPerson) {
        List<Integer> sorted = new ArrayList<>(commitsPerPerson);
        sorted.sort(Comparator.reverseOrder());
        int total = sorted.stream().mapToInt(Integer::intValue).sum();
        int sum = 0;
        int holders = 0;
        for (int commits : sorted) {
            sum += commits;
            holders++;
            if (2 * sum >= total) {
                break;
            }
        }
        return holders;
    }

    private static String topFolder(String path) {
        if (path == null) {
            return "";
        }
        int slash = path.indexOf('/');
        return slash > 0 ? path.substring(0, slash) : "";
    }

    /**
     * The weighted total of the measured sub-scores (in {@link #KEYS} order); sub-scores with weight 0 are left out.
     * A sub-score's drag is measured on the uncapped geometric mean, so it ranks the sub-scores also while the
     * weakest-link cap holds the total down; {@link MaintainabilityScore#getCappedBy()} names the capping one.
     */
    public static MaintainabilityScore combine(List<SubScore> measured, Map<String, Double> weights) {
        return combine(measured, weights, new Rules());
    }

    static MaintainabilityScore combine(List<SubScore> measured, Map<String, Double> weights, Rules rules) {
        List<SubScore> weighted = new ArrayList<>();
        for (SubScore subScore : measured) {
            double weight = weights.getOrDefault(subScore.getKey(), 0.0);
            if (weight > 0) {
                weighted.add(subScore.withWeight(weight));
            }
        }
        MaintainabilityScore score = new MaintainabilityScore();
        double mean = geometricMean(weighted);
        SubScore weakest = rules.weakestLinkMargin < 0 ? null : weakestCappingSubScore(weighted, rules.capExcludes);
        double total = mean;
        if (weakest != null && weakest.getScore() + rules.weakestLinkMargin < mean) {
            total = weakest.getScore() + rules.weakestLinkMargin;
            score.setCappedBy(weakest.getLabel());
        }
        score.setCapMargin(rules.weakestLinkMargin);
        score.setValue(round1(total));
        score.setGrade(grade(total, rules.gradeThresholds));
        for (int i = 0; i < weighted.size(); i++) {
            List<SubScore> perfect = new ArrayList<>(weighted);
            perfect.set(i, new SubScore("", "", 0, "", 10).withWeight(weighted.get(i).getWeight()));
            weighted.get(i).setDrag(round1(geometricMean(perfect) - mean));
        }
        score.setSubScores(weighted);
        return score;
    }

    static double geometricMean(List<SubScore> weighted) {
        double weightSum = 0;
        double logSum = 0;
        for (SubScore subScore : weighted) {
            weightSum += subScore.getWeight();
            logSum += subScore.getWeight() * Math.log(Math.max(SCORE_FLOOR, subScore.getScore()) / 10);
        }
        return weightSum == 0 ? 10 : 10 * Math.exp(logSum / weightSum);
    }

    // The weakest sub-score that may cap the total (built-in: about the code itself, as how knowledge is spread over
    // people does not cap it).
    private static SubScore weakestCappingSubScore(List<SubScore> weighted, Set<String> excludes) {
        return weighted.stream().filter(s -> !excludes.contains(s.getKey()))
                .min(Comparator.comparingDouble(SubScore::getScore)).orElse(null);
    }

    public static String grade(double score) {
        return grade(score, GRADE_THRESHOLDS);
    }

    static String grade(double score, double[] thresholds) {
        return score >= thresholds[0] ? "A" : score >= thresholds[1] ? "B" : score >= thresholds[2] ? "C" : score >= thresholds[3] ? "D" : "E";
    }

    /** Piecewise-linear over ascending {measure, score} anchors, flat beyond the ends. */
    static double interpolate(double x, double[][] anchors) {
        if (x <= anchors[0][0]) {
            return anchors[0][1];
        }
        for (int i = 1; i < anchors.length; i++) {
            if (x <= anchors[i][0]) {
                double t = (x - anchors[i - 1][0]) / (anchors[i][0] - anchors[i - 1][0]);
                return round1(anchors[i - 1][1] + t * (anchors[i][1] - anchors[i - 1][1]));
            }
        }
        return anchors[anchors.length - 1][1];
    }

    private static Map<String, Double> merge(Map<String, Double> defaults, Map<String, Double> overrides) {
        Map<String, Double> merged = new LinkedHashMap<>(defaults);
        overrides.forEach((key, value) -> {
            if (value != null && value >= 0) {
                merged.put(key, value);
            }
        });
        return merged;
    }

    private static Map<String, Double> weights(double... values) {
        Map<String, Double> weights = new LinkedHashMap<>();
        for (int i = 0; i < KEYS.size(); i++) {
            weights.put(KEYS.get(i), values[i]);
        }
        return Collections.unmodifiableMap(weights);
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private static String percentage(double value) {
        return value > 0 && value < 1 ? "<1%" : Math.round(value) + "%";
    }
}
