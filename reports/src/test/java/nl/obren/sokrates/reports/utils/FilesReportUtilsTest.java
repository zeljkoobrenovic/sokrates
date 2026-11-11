package nl.obren.sokrates.reports.utils;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.filehistory.CommitInfo;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilesReportUtilsTest {

    private SourceFile fileWithChurn(String path, int added, int deleted) {
        SourceFile sourceFile = new SourceFile(new File(path));
        sourceFile.setRelativePath(path);
        sourceFile.setLinesOfCode(100);
        FileModificationHistory history = new FileModificationHistory(path);
        CommitInfo commit = new CommitInfo("c1", "2020-01-01");
        commit.setEmail("alice@org.com");
        history.getCommits().add(commit);
        history.addDateIfAbsent("2020-01-01");
        history.addChurn(added, deleted);
        sourceFile.setFileModificationHistory(history);
        return sourceFile;
    }

    @Test
    void churnColumnShownOnlyWhenRequested() {
        List<SourceFile> files = List.of(fileWithChurn("src/A.java", 12, 4));

        String withChurn = FilesReportUtils.getFilesTable(files, false, true, false, 500, true);
        assertTrue(withChurn.contains("line<br>churn"), "churn header should be present");
        assertTrue(withChurn.contains("+12"), "added lines should render");
        assertTrue(withChurn.contains("-4"), "deleted lines should render");

        String withoutChurn = FilesReportUtils.getFilesTable(files, false, true, false, 500);
        assertFalse(withoutChurn.contains("line<br>churn"), "churn header must be absent by default");
    }

    @Test
    void escapesRepositoryControlledNamesAndEncodesViewerLink() {
        SourceFile sourceFile = fileWithChurn("src/pwn\" onmouseover=\"XSSATTR/<img src=x onerror=XSSNAME>.ts", 1, 0);
        sourceFile.getFileModificationHistory().getCommits().get(0).setEmail("x\"y@example.com");

        String table = FilesReportUtils.getFilesTable(List.of(sourceFile), true, true, false, 500);

        assertFalse(table.contains("<img src=x"), table);
        assertFalse(table.contains("onmouseover=\"XSSATTR"), table);
        assertFalse(table.contains("x\"y@"), table);
        assertTrue(table.contains("href='../src/viewer.html#aspect=main&file=src/pwn%22%20onmouseover%3D%22XSSATTR/%3Cimg%20src%3Dx%20onerror%3DXSSNAME%3E.ts'>&lt;img src=x onerror=XSSNAME&gt;.ts</a>"), table);
        assertTrue(table.contains(">in src/pwn&quot; onmouseover=&quot;XSSATTR</div>"), table);
        assertTrue(table.contains(">x&quot;y@example.com</td>"), table);
    }

    @Test
    void rootLevelFileSaysInRoot() {
        String table = FilesReportUtils.getFilesTable(List.of(fileWithChurn("pom.xml", 1, 0)), true, false, false, 500);
        assertTrue(table.contains(">in root</div>"), table);
    }

    @Test
    void ordinaryNamesRenderUnchanged() {
        String table = FilesReportUtils.getFilesTable(List.of(fileWithChurn("src/main/java/Foo.java", 1, 0)), true, false, false, 500);
        assertTrue(table.contains("href='../src/viewer.html#aspect=main&file=src/main/java/Foo.java'>Foo.java</a>"), table);
        assertTrue(table.contains(">in src/main/java</div>"), table);
    }

    @Test
    void churnPlaceholderWhenNoChurnData() {
        SourceFile sourceFile = new SourceFile(new File("src/B.java"));
        sourceFile.setRelativePath("src/B.java");
        sourceFile.setLinesOfCode(50);
        FileModificationHistory history = new FileModificationHistory("src/B.java");
        CommitInfo commit = new CommitInfo("c1", "2020-01-01");
        commit.setEmail("bob@org.com");
        history.getCommits().add(commit);
        history.addDateIfAbsent("2020-01-01");
        sourceFile.setFileModificationHistory(history);

        String table = FilesReportUtils.getFilesTable(List.of(sourceFile), false, true, false, 500, true);
        assertTrue(table.contains("line<br>churn"), "churn header still present");
        // No +/- churn spans for a file with zero churn.
        assertFalse(table.contains("+0</span>"), "zero churn should not render as +0");
    }
}
