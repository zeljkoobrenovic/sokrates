package nl.obren.sokrates.reports.generators.explorers;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.sourcecode.filehistory.CommitInfo;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import nl.obren.sokrates.sourcecode.landscape.analysis.FileExport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class FilesExplorerNewColumnsTest {

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

    private static CommitInfo commit(String id, String date, String email) {
        CommitInfo commit = new CommitInfo(id, date);
        commit.setEmail(email);
        return commit;
    }

    @Test
    void contributorsWithinCountsDistinctAuthorsOfRecentCommits() {
        FileModificationHistory history = new FileModificationHistory("a/A.java");
        history.setCommits(Arrays.asList(
                commit("1", "2026-09-25", "x@a"),
                commit("2", "2026-09-20", "x@a"),
                commit("3", "2026-08-01", "y@a"),
                commit("4", "2025-01-01", "z@a")));

        assertEquals(1, FilesExplorerGenerators.contributorsWithin(history, 30));
        assertEquals(2, FilesExplorerGenerators.contributorsWithin(history, 90));
    }

    @Test
    void theOptionalColumnsAreLeftOutOfTheJsonWhenNotComputed() throws Exception {
        FileExport landscapeFile = new FileExport("repo", "a/A.java", "main", 10);
        String json = new JsonGenerator().generate(landscapeFile);
        assertFalse(json.contains("unitsCount") || json.contains("mcCabeIndexSum") || json.contains("contributorsCount30Days"), json);

        landscapeFile.setUnitsCount(3);
        landscapeFile.setMcCabeIndexSum(12);
        String withUnits = new JsonGenerator().generate(landscapeFile);
        assertTrue(withUnits.contains("\"unitsCount\" : 3") || withUnits.contains("\"unitsCount\":3"), withUnits);
    }
}
