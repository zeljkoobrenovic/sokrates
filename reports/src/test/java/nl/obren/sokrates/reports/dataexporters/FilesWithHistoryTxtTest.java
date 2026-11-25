package nl.obren.sokrates.reports.dataexporters;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.filehistory.CommitInfo;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** text/mainFilesWithHistory.txt: the days changed in the hotspot window, as the last column. */
class FilesWithHistoryTxtTest {
    @BeforeEach
    void fixDate() {
        DateUtils.reset();
        DateUtils.setDateParam("2026-11-20");
    }

    @AfterEach
    void resetDate() {
        DateUtils.setDateParam(null);
        DateUtils.reset();
    }

    @Test
    void appendsTheDaysChangedInThePastYear() {
        SourceFile file = fileChangedOn("ada@example.com", "2026-11-01", "2026-02-01", "2025-06-01");
        String[] lines = FileListsDataExporter.getFilesWithHistoryAsTxt(List.of(file)).split("\n");
        List<String> header = Arrays.asList(lines[0].split("\t"));
        String[] row = lines[1].split("\t", -1);

        assertEquals(FileListsDataExporter.ACTIVE_DAYS_365_COLUMN, header.get(header.size() - 1), "appended last");
        assertEquals(header.size(), row.length);
        assertEquals("2", row[header.indexOf(FileListsDataExporter.ACTIVE_DAYS_365_COLUMN)]);
        assertEquals("3", row[header.indexOf("# active days")]);
        assertEquals("src/A.java", row[header.indexOf("path")]);
    }

    @Test
    void listsEachFilesContributorsMostActiveInThePastYearFirst() {
        SourceFile file = fileChangedOn("bob@example.com", "2020-01-01", "2021-03-01");
        CommitInfo recent = new CommitInfo("sha-ada", "2026-10-01");
        recent.setEmail("ada@example.com");
        file.getFileModificationHistory().getCommits().add(recent);
        CommitInfo again = new CommitInfo("sha-ada", "2026-10-01"); // the same commit seen twice counts once
        again.setEmail("ada@example.com");
        file.getFileModificationHistory().getCommits().add(again);

        String[] lines = FileListsDataExporter.getFileContributorsAsTxt(List.of(file)).split("\n");
        assertEquals("path\tcontributor\t# commits\t# commits (365d)\tfirst commit\tlast commit", lines[0]);
        assertEquals("src/A.java\tada@example.com\t1\t1\t2026-10-01\t2026-10-01", lines[1]);
        assertEquals("src/A.java\tbob@example.com\t2\t0\t2020-01-01\t2021-03-01", lines[2]);
        assertEquals(3, lines.length);
    }

    private static SourceFile fileChangedOn(String email, String... dates) {
        SourceFile file = new SourceFile(new File("src/A.java"));
        file.setRelativePath("src/A.java");
        file.setLinesOfCode(120);
        FileModificationHistory history = new FileModificationHistory("src/A.java");
        history.setDates(new ArrayList<>(Arrays.asList(dates)));
        List<CommitInfo> commits = new ArrayList<>();
        for (String date : dates) {
            CommitInfo commit = new CommitInfo("sha-" + date, date);
            commit.setEmail(email);
            commits.add(commit);
        }
        history.setCommits(commits);
        file.setFileModificationHistory(history);
        return file;
    }

    @Test
    void leavesBulkCommitsOut() {
        List<SourceFile> files = new ArrayList<>();
        int count = nl.obren.sokrates.sourcecode.analysis.scores.MaintainabilityScoresAnalyzer.MAX_FILES_PER_COMMIT + 1;
        for (int i = 0; i < count; i++) {
            SourceFile file = fileChangedOn("team@example.com", "2026-01-06"); // one copyright-year commit over every file
            file.setRelativePath("src/F" + i + ".java");
            file.getFileModificationHistory().getCommits().forEach(c -> c.setId("bulk"));
            files.add(file);
        }
        CommitInfo real = new CommitInfo("feature", "2026-03-01");
        real.setEmail("ada@example.com");
        files.get(0).getFileModificationHistory().getCommits().add(real);

        String[] lines = FileListsDataExporter.getFileContributorsAsTxt(files).split("\n");
        assertEquals(2, lines.length, "only ada's feature commit is left");
        assertEquals("src/F0.java\tada@example.com\t1\t1\t2026-03-01\t2026-03-01", lines[1]);
    }
}
