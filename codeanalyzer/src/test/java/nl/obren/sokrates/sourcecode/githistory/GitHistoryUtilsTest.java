package nl.obren.sokrates.sourcecode.githistory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GitHistoryUtilsTest {

    @Test
    void parseLine() {
        String line = "2019-11-09 author@github.com 0bc5d0318b3814ebd5b52605668756a8d5598e24 common/src/main/resources/components/ace/src/snippets/maze.js";
        FileUpdate fileUpdate = GitHistoryUtils.parseLine(line);

        assertEquals(fileUpdate.getDate(), "2019-11-09");
        assertEquals(fileUpdate.getAuthorEmail(), "author@github.com");
        assertEquals(fileUpdate.getCommitId(), "0bc5d0318b3814ebd5b52605668756a8d5598e24");
        assertEquals(fileUpdate.getPath(), "common/src/main/resources/components/ace/src/snippets/maze.js");
    }
}