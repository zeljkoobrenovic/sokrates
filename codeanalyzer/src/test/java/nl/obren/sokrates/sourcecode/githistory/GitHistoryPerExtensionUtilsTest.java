package nl.obren.sokrates.sourcecode.githistory;

import nl.obren.sokrates.sourcecode.analysis.FileHistoryAnalysisConfig;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GitHistoryPerExtensionUtilsTest {

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

    @Test
    void committersAndFilesAreDistinctPerExtension() throws Exception {
        resetHistoryCache();

        // Two contributors, two .java files, one .xml file; alice touches A.java in two commits.
        String history =
                "2020-01-01 alice@org.com c1 src/A.java\n" +
                "2020-01-02 alice@org.com c2 src/A.java\n" +   // same author + file again
                "2020-01-03 bob@org.com c3 src/B.java\n" +
                "2020-01-04 alice@org.com c4 build/pom.xml\n";

        File file = writeHistory(history);
        List<CommitsPerExtension> perExtension =
                new GitHistoryPerExtensionUtils().getCommitsPerExtensions(file, new FileHistoryAnalysisConfig());

        CommitsPerExtension java = perExtension.stream().filter(e -> e.getExtension().equals("java")).findFirst().orElseThrow();
        // distinct committers (alice, bob) and distinct files (A.java, B.java) - not 3 commits worth
        assertEquals(2, java.getCommitters().size());
        assertEquals(2, java.getFilesCount());
        assertEquals(3, java.getCommitsCount());

        CommitsPerExtension xml = perExtension.stream().filter(e -> e.getExtension().equals("xml")).findFirst().orElseThrow();
        assertEquals(1, xml.getCommitters().size());
        assertEquals(1, xml.getFilesCount());
        assertEquals(1, xml.getCommitsCount());
    }
}
