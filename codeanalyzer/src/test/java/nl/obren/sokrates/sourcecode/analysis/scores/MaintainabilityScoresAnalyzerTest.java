package nl.obren.sokrates.sourcecode.analysis.scores;

import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.MaintainabilityScoresConfig;
import nl.obren.sokrates.sourcecode.core.ScoreFrameworkConfig;
import nl.obren.sokrates.sourcecode.filehistory.CommitInfo;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import nl.obren.sokrates.sourcecode.metrics.Metric;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MaintainabilityScoresAnalyzerTest {

    @BeforeEach
    void fixTheAnalysisDate() {
        DateUtils.reset();
        DateUtils.setDateParam("2026-10-01");
    }

    @AfterEach
    void resetTheAnalysisDate() {
        DateUtils.reset();
        DateUtils.setDateParam(null);
    }

    @Test
    void interpolatesBetweenAnchorsAndIsFlatBeyondThem() {
        double[][] anchors = {{0, 10}, {10, 6}, {20, 0}};
        assertEquals(10, MaintainabilityScoresAnalyzer.interpolate(-5, anchors));
        assertEquals(8, MaintainabilityScoresAnalyzer.interpolate(5, anchors));
        assertEquals(3, MaintainabilityScoresAnalyzer.interpolate(15, anchors));
        assertEquals(0, MaintainabilityScoresAnalyzer.interpolate(99, anchors));
    }

    @Test
    void aWeakSubScoreCannotBeHiddenByStrongOnes() {
        List<SubScore> measured = Arrays.asList(
                new SubScore("a", "A", 0, "", 10), new SubScore("b", "B", 0, "", 10),
                new SubScore("c", "C", 0, "", 10), new SubScore("d", "D", 0, "", 1));
        Map<String, Double> weights = new HashMap<>();
        Arrays.asList("a", "b", "c", "d").forEach(k -> weights.put(k, 1.0));

        MaintainabilityScore score = MaintainabilityScoresAnalyzer.combine(measured, weights);

        // the arithmetic mean would be 7.75; the geometric mean is 5.6, and of its 0.6 above the weakest link (1 + 4)
        // only half counts: 5.3 (a hard cap would pile every such repository onto exactly 5.0)
        assertEquals(5.3, score.getValue());
        assertEquals("C", score.getGrade());
        assertEquals("D", score.getCappedBy());
        // drags are measured on the uncapped mean (5.6): a perfect D makes it 10
        assertEquals(4.4, score.getSubScores().get(3).getDrag());
        assertEquals(0.0, score.getSubScores().get(0).getDrag());
    }

    @Test
    void theKnowledgeSpreadDoesNotCapTheTotal() {
        List<SubScore> measured = Arrays.asList(new SubScore("a", "A", 0, "", 10), new SubScore("b", "B", 0, "", 10),
                new SubScore("c", "C", 0, "", 10), new SubScore(MaintainabilityScoresAnalyzer.KNOWLEDGE, "Knowledge", 1, "", 1));
        Map<String, Double> weights = new HashMap<>();
        Arrays.asList("a", "b", "c", MaintainabilityScoresAnalyzer.KNOWLEDGE).forEach(k -> weights.put(k, 1.0));

        MaintainabilityScore score = MaintainabilityScoresAnalyzer.combine(measured, weights);

        assertEquals(5.6, score.getValue());
        assertEquals("", score.getCappedBy());
    }

    @Test
    void zeroWeightsLeaveSubScoresOut() {
        List<SubScore> measured = Arrays.asList(new SubScore("a", "A", 0, "", 8), new SubScore("b", "B", 0, "", 0));
        Map<String, Double> weights = new HashMap<>();
        weights.put("a", 1.0);
        weights.put("b", 0.0);

        MaintainabilityScore score = MaintainabilityScoresAnalyzer.combine(measured, weights);

        assertEquals(8.0, score.getValue());
        assertEquals(1, score.getSubScores().size());
    }

    @Test
    void humanAndAiWeighTheSameKeysDifferently() {
        assertEquals(MaintainabilityScoresAnalyzer.KEYS, new ArrayList<>(MaintainabilityScoresAnalyzer.HUMAN_WEIGHTS.keySet()));
        assertEquals(MaintainabilityScoresAnalyzer.KEYS, new ArrayList<>(MaintainabilityScoresAnalyzer.AI_WEIGHTS.keySet()));
        assertTrue(MaintainabilityScoresAnalyzer.AI_WEIGHTS.get("contextPerChange") > MaintainabilityScoresAnalyzer.HUMAN_WEIGHTS.get("contextPerChange"));
        assertTrue(MaintainabilityScoresAnalyzer.HUMAN_WEIGHTS.get("knowledge") > MaintainabilityScoresAnalyzer.AI_WEIGHTS.get("knowledge"));
    }

    @Test
    void knowledgeHoldersAreThePeopleWithHalfOfTheCommits() {
        assertEquals(1, MaintainabilityScoresAnalyzer.knowledgeHolders(Arrays.asList(60, 20, 20)));
        assertEquals(2, MaintainabilityScoresAnalyzer.knowledgeHolders(Arrays.asList(30, 30, 20, 20)));
        assertEquals(3, MaintainabilityScoresAnalyzer.knowledgeHolders(Arrays.asList(10, 10, 10, 10, 10)));
    }

    @Test
    void changeStatsMeasureEntropyOverComponentsAndLinesReadPerChange() {
        // c1 stays in one component (entropy 0, 300 lines), c2 is split evenly over two (entropy 1, 400 lines),
        // c3 is too old, c4 is a bot's
        SourceFile a1 = file("a/One.java", 100, "c1:2026-09-01:x@a", "c2:2026-09-02:x@a", "c3:2024-01-01:x@a");
        SourceFile a2 = file("a/Two.java", 200, "c1:2026-09-01:x@a", "c4:2026-09-03:bot@a");
        SourceFile b1 = file("b/Three.java", 300, "c2:2026-09-02:x@a");

        MaintainabilityScoresAnalyzer.ChangeStats stats = MaintainabilityScoresAnalyzer.changeStats(
                Arrays.asList(a1, a2, b1), f -> f.getRelativePath().substring(0, 1), Collections.singleton("bot@a"));

        assertEquals(2, stats.commits);
        assertEquals(0.5, stats.entropy, 1e-9);
        assertEquals(350, stats.contextLines, 1e-9);
    }

    private static SourceFile file(String path, int loc, String... commits) {
        SourceFile file = new SourceFile();
        file.setRelativePath(path);
        file.setLinesOfCode(loc);
        FileModificationHistory history = new FileModificationHistory(path);
        List<CommitInfo> infos = new ArrayList<>();
        for (String commit : commits) {
            String[] parts = commit.split(":");
            CommitInfo info = new CommitInfo(parts[0], parts[1]);
            info.setEmail(parts[2]);
            infos.add(info);
        }
        history.setCommits(infos);
        file.setFileModificationHistory(history);
        return file;
    }

    private static Metric metric(String id, Number value) {
        Metric metric = new Metric();
        metric.id(id).value(value);
        return metric;
    }

    @Test
    void aCustomFrameworkUsesItsOwnSubScoresAnchorsAndWeights() {
        List<SubScore> measured = Arrays.asList(
                new SubScore(MaintainabilityScoresAnalyzer.VOLUME, "Volume", 100_000, "100,000 lines", 6.7),
                new SubScore(MaintainabilityScoresAnalyzer.DUPLICATION, "Duplication", 4, "4%", 8.5));
        ScoreFrameworkConfig config = new ScoreFrameworkConfig();
        config.getSubScores().add(new ScoreFrameworkConfig.SubScoreConfig(MaintainabilityScoresAnalyzer.VOLUME, 1, 0)
                .anchors(new double[]{0, 10}, new double[]{200_000, 0}));
        ScoreFrameworkConfig.SubScoreConfig todos = new ScoreFrameworkConfig.SubScoreConfig("todos", 0, 2)
                .anchors(new double[]{100, 0}, new double[]{0, 10});
        todos.setMetric("NUMBER_OF_TODOS");
        todos.setDescription("TODO comments");
        config.getSubScores().add(todos);
        ScoreFrameworkConfig.SubScoreConfig missing = new ScoreFrameworkConfig.SubScoreConfig("missing", 1, 1).anchors(new double[]{0, 10});
        missing.setMetric("NOT_COMPUTED");
        config.getSubScores().add(missing);
        config.getSubScores().add(new ScoreFrameworkConfig.SubScoreConfig("unknownKey", 1, 1));
        config.getSubScores().add(new ScoreFrameworkConfig.SubScoreConfig(MaintainabilityScoresAnalyzer.VOLUME, 1, 1));
        config.getSubScores().add(new ScoreFrameworkConfig.SubScoreConfig(MaintainabilityScoresAnalyzer.TEST_CODE, 1, 1));

        MaintainabilityScoresAnalyzer.Framework framework = MaintainabilityScoresAnalyzer.customFramework(measured, config,
                id -> id.equalsIgnoreCase("number_of_todos") ? metric("NUMBER_OF_TODOS", 25) : null);

        // duplication is not listed; the missing metric, the unknown key, the duplicate and the unmeasured test code are skipped
        assertEquals(Arrays.asList("volume", "todos"), framework.subScores.stream().map(SubScore::getKey).collect(java.util.stream.Collectors.toList()));
        assertEquals(5.0, framework.subScores.get(0).getScore(), "re-scored on its own anchors");
        assertEquals("Volume", framework.subScores.get(0).getLabel());
        assertEquals(7.5, framework.subScores.get(1).getScore(), "anchors in any order");
        assertEquals("25 TODO comments", framework.subScores.get(1).getMeasureText());
        assertEquals("todos", framework.subScores.get(1).getLabel());

        MaintainabilityScore human = MaintainabilityScoresAnalyzer.combine(framework.subScores, framework.humanWeights, framework.rules);
        MaintainabilityScore ai = MaintainabilityScoresAnalyzer.combine(framework.subScores, framework.aiWeights, framework.rules);
        assertEquals(5.0, human.getValue(), "volume only");
        assertEquals(7.5, ai.getValue(), "TODOs only");
    }

    @Test
    void aCustomFrameworkSetsTheCapAndTheGrades() {
        List<SubScore> measured = Arrays.asList(new SubScore("a", "A", 0, "", 10), new SubScore("b", "B", 0, "", 2));
        ScoreFrameworkConfig config = new ScoreFrameworkConfig();
        config.getSubScores().add(new ScoreFrameworkConfig.SubScoreConfig("x", 1, 1).anchors(new double[]{0, 10}));
        MaintainabilityScoresAnalyzer.Framework framework = MaintainabilityScoresAnalyzer.customFramework(measured, config, id -> null);
        assertTrue(framework.subScores.isEmpty(), "no metric: a key that is not built in is skipped");

        Map<String, Double> weights = new HashMap<>();
        weights.put("a", 1.0);
        weights.put("b", 1.0);
        config.setWeakestLinkMargin(-1.0);
        config.setGradeThresholds(Arrays.asList(9.0, 7.0, 4.0, 2.0));
        MaintainabilityScore uncapped = MaintainabilityScoresAnalyzer.combine(measured, weights,
                MaintainabilityScoresAnalyzer.customFramework(measured, config, id -> null).rules);
        assertEquals(4.5, uncapped.getValue(), "no cap: the geometric mean of 10 and 2");
        assertEquals("", uncapped.getCappedBy());
        assertEquals("C", uncapped.getGrade(), "C from 4 with these thresholds (D with the built-in ones)");

        config.setWeakestLinkMargin(1.5);
        config.setCapExcludes(Collections.singletonList("b"));
        MaintainabilityScore excluded = MaintainabilityScoresAnalyzer.combine(measured, weights,
                MaintainabilityScoresAnalyzer.customFramework(measured, config, id -> null).rules);
        assertEquals(4.5, excluded.getValue(), "b may not cap; a (10 + 1.5) does not");
        config.setCapExcludes(null);
        MaintainabilityScore capped = MaintainabilityScoresAnalyzer.combine(measured, weights,
                MaintainabilityScoresAnalyzer.customFramework(measured, config, id -> null).rules);
        assertEquals(4.0, capped.getValue(), "the mean 4.47 is 0.97 above 2 + 1.5; half of that counts");
        assertEquals(1.5, capped.getCapMargin());
        config.setCapStrength(1.0);
        assertEquals(3.5, MaintainabilityScoresAnalyzer.combine(measured, weights,
                MaintainabilityScoresAnalyzer.customFramework(measured, config, id -> null).rules).getValue(), "capStrength 1: a hard cap");
        config.setCapStrength(0.0);
        MaintainabilityScore free = MaintainabilityScoresAnalyzer.combine(measured, weights,
                MaintainabilityScoresAnalyzer.customFramework(measured, config, id -> null).rules);
        assertEquals(4.5, free.getValue(), "capStrength 0: no cap");
        assertEquals("", free.getCappedBy());
        config.setCapStrength(null);

        config.setGradeThresholds(Arrays.asList(5.0, 7.0, 4.0, 2.0));
        assertEquals("D", MaintainabilityScoresAnalyzer.combine(measured, weights,
                MaintainabilityScoresAnalyzer.customFramework(measured, config, id -> null).rules).getGrade(), "invalid thresholds: built-in grades");
    }

    @Test
    void invalidAnchorsSkipTheSubScore() {
        assertEquals(0, MaintainabilityScoresAnalyzer.anchors(Arrays.asList(Arrays.asList(0.0, 10.0), Collections.singletonList(5.0))).length);
        double[][] anchors = MaintainabilityScoresAnalyzer.anchors(Arrays.asList(Arrays.asList(10.0, -3.0), Arrays.asList(0.0, 12.0)));
        assertArrayEquals(new double[]{0, 10}, anchors[0], "sorted, score clamped to 0-10");
        assertArrayEquals(new double[]{10, 0}, anchors[1]);
    }

    @Test
    void theCustomFrameworkIsReadFromConfigJsonAndOffByDefault() throws IOException {
        assertFalse(new MaintainabilityScoresConfig().isUseCustomFramework());
        String json = "{\"analysis\": {\"maintainabilityScores\": {\"useCustomFramework\": true, \"customFramework\": {"
                + "\"weakestLinkMargin\": 3, \"gradeThresholds\": [9, 7, 5, 3], \"subScores\": ["
                + "{\"key\": \"duplication\", \"humanWeight\": 2, \"aiWeight\": 3},"
                + "{\"key\": \"tests\", \"metric\": \"LINES_OF_CODE_TEST\", \"anchors\": [[0, 0], [50000, 10]], \"aiWeight\": 2}]}}}}";
        CodeConfiguration configuration = (CodeConfiguration) new JsonMapper().getObject(json, CodeConfiguration.class);
        MaintainabilityScoresConfig config = configuration.getAnalysis().getMaintainabilityScores();
        assertTrue(config.isUseCustomFramework());
        ScoreFrameworkConfig framework = config.getCustomFramework();
        assertEquals(2, framework.getSubScores().size());
        assertEquals(3.0, framework.getSubScores().get(0).getAiWeight());
        assertEquals(1.0, framework.getSubScores().get(1).getHumanWeight(), "weights default to 1");
        assertEquals("LINES_OF_CODE_TEST", framework.getSubScores().get(1).getMetric());
        assertEquals(Arrays.asList(50000.0, 10.0), framework.getSubScores().get(1).getAnchors().get(1));
        assertEquals(3.0, framework.getWeakestLinkMargin());
    }

    @Test
    void theCoverageSaysHowManySubScoresWereMeasured() {
        List<SubScore> measured = Arrays.asList(new SubScore(MaintainabilityScoresAnalyzer.VOLUME, "Volume", 0, "", 8),
                new SubScore(MaintainabilityScoresAnalyzer.KNOWLEDGE, "Knowledge spread", 0, "", 5));
        MaintainabilityScore ai = MaintainabilityScoresAnalyzer.combine(measured, MaintainabilityScoresAnalyzer.AI_WEIGHTS);
        MaintainabilityScoresAnalyzer.setCoverage(ai, MaintainabilityScoresAnalyzer.expectedSubScores(MaintainabilityScoresAnalyzer.AI_WEIGHTS));
        // knowledge has AI weight 0: 9 sub-scores count, only volume was measured
        assertEquals(9, ai.getSubScoresTotal());
        assertEquals("1/9", ai.getCoverageShort());
        assertFalse(ai.isFullyMeasured());
        assertEquals("Duplication", ai.getNotMeasured().get(0));
        assertTrue(ai.getCoverageText().startsWith("measured on 1 of 9 sub-scores (not measured: Duplication, Unit size,"));

        ScoreFrameworkConfig config = new ScoreFrameworkConfig();
        config.getSubScores().add(new ScoreFrameworkConfig.SubScoreConfig(MaintainabilityScoresAnalyzer.VOLUME, 1, 0));
        ScoreFrameworkConfig.SubScoreConfig todos = new ScoreFrameworkConfig.SubScoreConfig("todos", 1, 1);
        todos.setLabel("TODOs");
        config.getSubScores().add(todos);
        MaintainabilityScore human = MaintainabilityScoresAnalyzer.combine(measured.subList(0, 1), Collections.singletonMap("volume", 1.0));
        MaintainabilityScoresAnalyzer.setCoverage(human, MaintainabilityScoresAnalyzer.expectedSubScores(config, true));
        assertEquals("measured on 1 of 2 sub-scores (not measured: TODOs)", human.getCoverageText());
        MaintainabilityScoresAnalyzer.setCoverage(human, MaintainabilityScoresAnalyzer.expectedSubScores(config, false));
        assertEquals(1, human.getSubScoresTotal(), "never below the measured count");

        assertTrue(new MaintainabilityScore().isFullyMeasured(), "unknown coverage (older analyses) is not flagged");
        assertEquals("", new MaintainabilityScore().getCoverageText());
    }

    @Test
    void testCodeScoreFollowsTheTestCodeAnchors() {
        double[][] anchors = MaintainabilityScoresAnalyzer.TEST_CODE_ANCHORS;
        assertEquals(1, MaintainabilityScoresAnalyzer.interpolate(0, anchors), "no tests: low, not zero");
        assertEquals(5, MaintainabilityScoresAnalyzer.interpolate(0.05, anchors));
        assertEquals(3, MaintainabilityScoresAnalyzer.interpolate(0.1, anchors));
        assertEquals(3.5, MaintainabilityScoresAnalyzer.interpolate(0.15, anchors), 1e-9);
        assertEquals(5, MaintainabilityScoresAnalyzer.interpolate(0.3, anchors));
        assertEquals(10, MaintainabilityScoresAnalyzer.interpolate(0.5, anchors), "half as much test code as main code earns full credit");
        assertEquals(10, MaintainabilityScoresAnalyzer.interpolate(1.5, anchors), "more test code earns nothing more");
        assertEquals("Tests presence", MaintainabilityScoresAnalyzer.LABELS.get(MaintainabilityScoresAnalyzer.TEST_CODE));
    }
}
