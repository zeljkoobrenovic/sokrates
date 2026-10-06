package nl.obren.sokrates.reports.landscape.ai;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.reports.generators.explorers.AiCostEstimatorData;
import nl.obren.sokrates.reports.generators.explorers.AiCostEstimatorGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class AiCostEstimatorAggregatorTest {

    private static AiCostEstimatorData repository(String start, String... emails) {
        AiCostEstimatorData data = new AiCostEstimatorData();
        for (String email : emails) {
            data.getAuthors().add(new AiCostEstimatorData.Author(email, email));
        }
        AiCostEstimatorData.Task task = new AiCostEstimatorData.Task();
        task.setStart(start);
        task.setEnd(start);
        task.getCommits().add(new AiCostEstimatorData.TaskCommit("abc", start, emails.length - 1));
        task.getSessions().add(new int[]{1, 100, 5, 5, 0});
        data.getTasks().add(task);
        data.getNoise().setBotCommits(2);
        data.setTotalCommitsCount(10);
        data.setLinesInScopes(300);
        data.getTicketPrefixes().add("PROJ");
        return data;
    }

    @Test
    void mergesTasksAuthorsAndNoise() {
        AiCostEstimatorData merged = AiCostEstimatorAggregator.merge(Arrays.asList("a", "b"), Arrays.asList("../a/reports/html/index.html#ai-cost", "../b"),
                Arrays.asList(1000L, 500L), Arrays.asList(repository("2024-01-01", "x@y.com"), repository("2024-02-01", "z@y.com", "X@y.com")), 10);

        assertEquals(2, merged.getRepositories().size());
        assertEquals("../a/reports/html/index.html#ai-cost", merged.getRepositories().get(0).getUrl());
        assertEquals(2, merged.getAuthors().size(), "authors merged by email, case-insensitively");
        assertEquals(4, merged.getNoise().getBotCommits());
        assertEquals(20, merged.getTotalCommitsCount());
        assertEquals(Arrays.asList("PROJ"), merged.getTicketPrefixes());
        // Newest first; the second repository's task points at the merged index of X@y.com (0).
        assertEquals("2024-02-01", merged.getTasks().get(0).getStart());
        assertEquals(1, merged.getTasks().get(0).getRepo());
        assertEquals(0, merged.getTasks().get(0).getCommits().get(0).getAuthor());
        assertEquals(2, merged.getTotalTasksCount());
        assertEquals(1500, merged.getMainLinesOfCode());
        assertEquals(600, merged.getLinesInScopes(), "summed over the repositories with history data");
        assertEquals(500, merged.getRepositories().get(1).getMainLinesOfCode());
    }

    @Test
    void aRepositoryWithoutHistoryDataCountsOnlyInTheRebuild() {
        AiCostEstimatorData merged = AiCostEstimatorAggregator.merge(Arrays.asList("a", "old"), Arrays.asList("", ""),
                Arrays.asList(1000L, 7000L), Arrays.asList(repository("2024-01-01", "x@y.com"), null), 10);
        assertEquals(2, merged.getRepositories().size());
        assertEquals(8000, merged.getMainLinesOfCode());
        assertEquals(300, merged.getLinesInScopes(), "a repository without history data adds no lines to the hindsight measure");
        assertEquals(1, merged.getTasks().size());
        assertEquals(10, merged.getTotalCommitsCount());
    }

    @Test
    void keepsTheNewestTasks() {
        AiCostEstimatorData merged = AiCostEstimatorAggregator.merge(Arrays.asList("a", "b"), Arrays.asList("", ""),
                Arrays.asList(0L, 0L), Arrays.asList(repository("2024-01-01", "x@y.com"), repository("2024-02-01", "z@y.com")), 1);
        assertEquals(1, merged.getTasks().size());
        assertEquals("2024-02-01", merged.getTasks().get(0).getStart());
        assertEquals(2, merged.getTotalTasksCount());
    }

    @Test
    void readsTheDataFromDataZip(@TempDir Path folder) throws Exception {
        File data = folder.resolve("data").toFile();
        data.mkdirs();
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(new File(data, "data.zip")))) {
            zip.putNextEntry(new ZipEntry(AiCostEstimatorGenerator.DATA_FILE_NAME));
            zip.write(new JsonGenerator().generate(repository("2024-01-01", "x@y.com")).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        AiCostEstimatorData read = AiCostEstimatorAggregator.read(data);
        assertNotNull(read);
        assertEquals(1, read.getTasks().size());
        assertArrayEquals(new int[]{1, 100, 5, 5, 0}, read.getTasks().get(0).getSessions().get(0));
        assertNull(read.getTasks().get(0).getRepo(), "no repository index in a repository's own data");

        assertNull(AiCostEstimatorAggregator.read(folder.resolve("missing").toFile()), "older repositories have no data");
    }
}
