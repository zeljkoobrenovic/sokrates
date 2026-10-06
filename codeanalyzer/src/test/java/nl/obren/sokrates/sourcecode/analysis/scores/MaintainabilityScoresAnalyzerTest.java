package nl.obren.sokrates.sourcecode.analysis.scores;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.filehistory.CommitInfo;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

        // the arithmetic mean would be 7.75; the geometric mean is 5.6, the weakest link caps at 1 + 4
        assertEquals(5.0, score.getValue());
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
}
