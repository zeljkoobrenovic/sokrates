package nl.obren.sokrates.sourcecode.filehistory;

import nl.obren.sokrates.sourcecode.analysis.FileHistoryAnalysisConfig;
import nl.obren.sokrates.sourcecode.githistory.GitHistoryUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GitHistoryUtilTest {

    // Clear the static history cache in GitHistoryUtils so each test reads its own file.
    private void resetHistoryCache() throws Exception {
        Field updates = GitHistoryUtils.class.getDeclaredField("updates");
        updates.setAccessible(true);
        updates.set(null, null);
    }

    private File writeHistory(String content) throws Exception {
        File file = File.createTempFile("git-history", ".txt");
        file.deleteOnExit();
        FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private FileModificationHistory forPath(List<FileModificationHistory> history, String path) {
        return history.stream().filter(h -> h.getPath().equals(path)).findFirst().orElseThrow();
    }

    @Test
    void perFileChurnIsSummedAcrossCommits() throws Exception {
        resetHistoryCache();

        // git-history columns: date email commit path name added deleted
        String content =
                "2020-01-01 alice@org.com c1 src/A.java alice 10 2\n" +
                "2020-01-02 bob@org.com c2 src/A.java bob 5 3\n" +   // A.java touched twice
                "2020-01-03 alice@org.com c3 src/B.java alice 7 0\n";
        File file = writeHistory(content);

        List<FileModificationHistory> history =
                new GitHistoryUtil().importGitLsFilesExport(file, new FileHistoryAnalysisConfig());

        FileModificationHistory a = forPath(history, "src/A.java");
        assertEquals(15, a.getLinesAdded());   // 10 + 5
        assertEquals(5, a.getLinesDeleted());   // 2 + 3
        assertEquals(20, a.getChurn());

        FileModificationHistory b = forPath(history, "src/B.java");
        assertEquals(7, b.getLinesAdded());
        assertEquals(0, b.getLinesDeleted());
        assertEquals(7, b.getChurn());
    }

    @Test
    void churnDefaultsToZeroWithoutChurnColumns() throws Exception {
        resetHistoryCache();

        // Older history format with no churn columns.
        String content = "2020-01-01 alice@org.com c1 src/A.java\n";
        File file = writeHistory(content);

        List<FileModificationHistory> history =
                new GitHistoryUtil().importGitLsFilesExport(file, new FileHistoryAnalysisConfig());

        FileModificationHistory a = forPath(history, "src/A.java");
        assertEquals(0, a.getChurn());
    }
}
