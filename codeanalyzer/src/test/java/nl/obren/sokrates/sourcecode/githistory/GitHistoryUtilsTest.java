package nl.obren.sokrates.sourcecode.githistory;

import nl.obren.sokrates.sourcecode.analysis.FileHistoryAnalysisConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GitHistoryUtilsTest {

    @Test
    void parseLine() {
        String line = "2019-11-09 author@github.com 0bc5d0318b3814ebd5b52605668756a8d5598e24 common/src/main/resources/components/ace/src/snippets/maze.js";
        FileUpdate fileUpdate = GitHistoryUtils.parseLine(line, new FileHistoryAnalysisConfig());

        assertEquals(fileUpdate.getDate(), "2019-11-09");
        assertEquals(fileUpdate.getAuthorEmail(), "author@github.com");
        assertEquals(fileUpdate.getCommitId(), "0bc5d0318b3814ebd5b52605668756a8d5598e24");
        assertEquals(fileUpdate.getPath(), "common/src/main/resources/components/ace/src/snippets/maze.js");
    }

    @Test
    void shouldIgnore() {
        List<String> ignore = Arrays.asList(new String[]{".*GITHUBBOT.*", "None", "none", "DL[-].*", "[a-zA-Z]+Releaser.*", "bot", "committed[-]by[-]bot.*"});

        assertFalse(GitHistoryUtils.shouldIgnore("", ignore));
        assertFalse(GitHistoryUtils.shouldIgnore("user@org", ignore));
        assertFalse(GitHistoryUtils.shouldIgnore("user", ignore));

        assertTrue(GitHistoryUtils.shouldIgnore("DL-Dev@org.com", ignore));
        assertTrue(GitHistoryUtils.shouldIgnore("DL-Dev", ignore));
        assertTrue(GitHistoryUtils.shouldIgnore("committed-by-bot", ignore));
        assertTrue(GitHistoryUtils.shouldIgnore("committed-by-bot@org.com", ignore));
        assertTrue(GitHistoryUtils.shouldIgnore("GITHUBBOT-test@org.com", ignore));
        assertTrue(GitHistoryUtils.shouldIgnore("dev-GITHUBBOT-test@org.com", ignore));
    }
}