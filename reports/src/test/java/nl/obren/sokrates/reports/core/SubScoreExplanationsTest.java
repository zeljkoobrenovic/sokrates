package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.scores.MaintainabilityScore;
import nl.obren.sokrates.sourcecode.analysis.scores.MaintainabilityScores;
import nl.obren.sokrates.sourcecode.analysis.scores.MaintainabilityScoresAnalyzer;
import nl.obren.sokrates.sourcecode.analysis.scores.SubScore;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.MaintainabilityScoresConfig;
import nl.obren.sokrates.sourcecode.core.ScoreFrameworkConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SubScoreExplanationsTest {
    @Test
    void everyBuiltInSubScoreSaysWhyItMattersForPeopleAndAgents() {
        MaintainabilityScoresAnalyzer.KEYS.forEach(key -> {
            SubScoreExplanations.Why why = SubScoreExplanations.BUILT_IN.get(key);
            assertNotNull(why, key);
            assertFalse(why.getHuman().isBlank(), key);
            assertFalse(why.getAi().isBlank(), key);
        });
    }

    private static MaintainabilityScore score(SubScore... subScores) {
        MaintainabilityScore score = new MaintainabilityScore();
        score.setValue(7);
        score.setGrade("B");
        score.setSubScores(Arrays.asList(subScores));
        return score;
    }

    @Test
    void rowsOpenOnAClickWithTheColumnsOwnAudienceFirst() {
        MaintainabilityScores scores = new MaintainabilityScores();
        scores.setHuman(score(new SubScore(MaintainabilityScoresAnalyzer.DUPLICATION, "Duplication", 4, "4%", 8.5).withWeight(1)));
        scores.setAi(score(new SubScore(MaintainabilityScoresAnalyzer.DUPLICATION, "Duplication", 4, "4%", 8.5).withWeight(1),
                new SubScore("todos", "<b>TODOs</b>", 3, "3 TODOs", 9).withWeight(1)));

        String html = ReportHealthSection.scoresCard(scores);

        assertEquals(2, html.split("<details class='sk-subscore-details'>", -1).length - 1, "one per row with a text");
        int humanColumn = html.indexOf("sk-score-name'>Human");
        int aiColumn = html.indexOf("sk-score-name'>AI");
        assertTrue(html.indexOf("For people", humanColumn) < html.indexOf("For AI agents", humanColumn));
        assertTrue(html.indexOf("For AI agents", aiColumn) < html.indexOf("For people", aiColumn));
        assertTrue(html.contains("&lt;b&gt;TODOs&lt;/b&gt;"), "labels escaped");
        assertFalse(html.substring(html.indexOf("TODOs&lt;")).contains("<details"), "no text: a plain row");
    }

    @Test
    void aCustomFrameworkCanExplainItsOwnSubScores() {
        CodeConfiguration configuration = new CodeConfiguration();
        MaintainabilityScoresConfig config = configuration.getAnalysis().getMaintainabilityScores();
        ScoreFrameworkConfig.SubScoreConfig todos = new ScoreFrameworkConfig.SubScoreConfig("todos", 1, 1);
        todos.setMetric("NUMBER_OF_TODOS");
        todos.setWhyHuman("Open <ends>.");
        config.getCustomFramework().getSubScores().add(todos);
        ScoreFrameworkConfig.SubScoreConfig duplication = new ScoreFrameworkConfig.SubScoreConfig(MaintainabilityScoresAnalyzer.DUPLICATION, 1, 1);
        duplication.setWhyAi("Own AI text.");
        config.getCustomFramework().getSubScores().add(duplication);
        CodeAnalysisResults results = new CodeAnalysisResults();
        results.setCodeConfiguration(configuration);

        assertNull(SubScoreExplanations.of(results).get("todos"), "only with the switch on");
        config.setUseCustomFramework(true);
        Map<String, SubScoreExplanations.Why> explanations = SubScoreExplanations.of(results);
        assertEquals("Open <ends>.", explanations.get("todos").getHuman());
        assertEquals("", explanations.get("todos").getAi());
        assertEquals("Own AI text.", explanations.get(MaintainabilityScoresAnalyzer.DUPLICATION).getAi());
        assertEquals(SubScoreExplanations.BUILT_IN.get(MaintainabilityScoresAnalyzer.DUPLICATION).getHuman(),
                explanations.get(MaintainabilityScoresAnalyzer.DUPLICATION).getHuman(), "a built-in key keeps the text it does not replace");
    }

    @Test
    void aPartlyMeasuredScoreSaysSoOnTheTileAndTheCard() {
        MaintainabilityScore score = score(new SubScore(MaintainabilityScoresAnalyzer.VOLUME, "Volume", 0, "", 8).withWeight(1));
        score.setSubScoresTotal(3);
        score.setNotMeasured(Arrays.asList("Unit size", "Unit complexity"));
        HealthSummary.Tile tile = HealthSummary.scoreTile("human score*", score, "for people to understand and change");
        assertEquals("measured on 1 of 3 sub-scores", tile.caption);
        assertTrue(tile.tooltip.contains("not measured: Unit size, Unit complexity"));

        MaintainabilityScores scores = new MaintainabilityScores();
        scores.setHuman(score);
        scores.setAi(score(new SubScore(MaintainabilityScoresAnalyzer.VOLUME, "Volume", 0, "", 8).withWeight(1)));
        String html = ReportHealthSection.scoresCard(scores);
        assertTrue(html.contains("sk-score-partial'>Measured on 1 of 3 sub-scores (not measured: Unit size, Unit complexity).</div>"));

        score.setSubScoresTotal(1);
        score.setNotMeasured(java.util.Collections.emptyList());
        assertEquals("for people to understand and change", HealthSummary.scoreTile("human score*", score, "for people to understand and change").caption);
    }
}
