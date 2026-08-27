package nl.obren.sokrates.sourcecode.githistory;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtractGitHistoryFileHandlerTest {
    private final static String SAMPLE = "2019-11-09 zeljko@sokrates.dev 0bc5d0318b3814ebd5b52605668756a8d5598e24 codeexplorer/src/test/java/nl/obren/sokrates/codeexplorer/io/RegexUtilsTest.java\n" +
            "2020-07-14 zeljkoobrenovic@yahoo.com 41d8eaf1e2d00b17df0c31cdf34f1b43eada6a22 codeexplorer/src/test/java/nl/obren/sokrates/reports/landscape/statichtml/LandscapeReportGeneratorTest.java\n" +
            "2020-05-18 zeljko@sokrates.dev cff81f4b4e05ce19c5ba81fe47c06fba83bb5cdf codeexplorer/src/test/java/nl/obren/sokrates/reports/landscape/statichtml/LandscapeReportGeneratorTest.java\n" +
            "2020-05-17 zeljko@sokrates.dev bbada1d04949e6972a089780a3d044d8c9416eb4 codeexplorer/src/test/java/nl/obren/sokrates/reports/landscape/statichtml/LandscapeReportGeneratorTest.java\n" +
            "2020-01-01 zeljko@sokrates.dev be8166a99d81ad4ba056222421d6e8b46b058cee common/pom.xml\n" +
            "2019-12-10 zeljko@sokrates.dev f92899e5ad43e3af987a9f789459a6a66c8fd036 common/pom.xml\n" +
            "2019-11-09 zeljko@sokrates.dev 0bc5d0318b3814ebd5b52605668756a8d5598e24 common/pom.xml\n" +
            "2019-12-10 zeljko@sokrates.dev f92899e5ad43e3af987a9f789459a6a66c8fd036 common/src/main/java/nl/obren/sokrates/common/analysis/Finding.java\n" +
            "2019-11-09 zeljko@sokrates.dev 0bc5d0318b3814ebd5b52605668756a8d5598e24 common/src/main/java/nl/obren/sokrates/common/analysis/Finding.java\n" +
            "2019-12-10 zeljko@sokrates.dev f92899e5ad43e3af987a9f789459a6a66c8fd036 common/src/main/java/nl/obren/sokrates/common/analysis/ValidationMessage.java\n" +
            "2019-11-09 zeljko@sokrates.dev 0bc5d0318b3814ebd5b52605668756a8d5598e24 common/src/main/java/nl/obren/sokrates/common/analysis/ValidationMessage.java\n" +
            "2019-12-10 zeljko@sokrates.dev f92899e5ad43e3af987a9f789459a6a66c8fd036 common/src/main/java/nl/obren/sokrates/common/io/JsonGenerator.java\n" +
            "2019-11-17 zeljko@sokrates.dev 231c934b900aa89635f589f2c91f7f336c568ae6 common/src/main/java/nl/obren/sokrates/common/io/JsonGenerator.java\n";

    private final static String RESULT_1 =
            "2020-01-01 zeljko@sokrates.dev be8166a99d81ad4ba056222421d6e8b46b058cee pom.xml\n" +
            "2019-12-10 zeljko@sokrates.dev f92899e5ad43e3af987a9f789459a6a66c8fd036 pom.xml\n" +
            "2019-11-09 zeljko@sokrates.dev 0bc5d0318b3814ebd5b52605668756a8d5598e24 pom.xml\n" +
            "2019-12-10 zeljko@sokrates.dev f92899e5ad43e3af987a9f789459a6a66c8fd036 src/main/java/nl/obren/sokrates/common/analysis/Finding.java\n" +
            "2019-11-09 zeljko@sokrates.dev 0bc5d0318b3814ebd5b52605668756a8d5598e24 src/main/java/nl/obren/sokrates/common/analysis/Finding.java\n" +
            "2019-12-10 zeljko@sokrates.dev f92899e5ad43e3af987a9f789459a6a66c8fd036 src/main/java/nl/obren/sokrates/common/analysis/ValidationMessage.java\n" +
            "2019-11-09 zeljko@sokrates.dev 0bc5d0318b3814ebd5b52605668756a8d5598e24 src/main/java/nl/obren/sokrates/common/analysis/ValidationMessage.java\n" +
            "2019-12-10 zeljko@sokrates.dev f92899e5ad43e3af987a9f789459a6a66c8fd036 src/main/java/nl/obren/sokrates/common/io/JsonGenerator.java\n" +
            "2019-11-17 zeljko@sokrates.dev 231c934b900aa89635f589f2c91f7f336c568ae6 src/main/java/nl/obren/sokrates/common/io/JsonGenerator.java";

    private final static String RESULT_2 = "2019-11-09 zeljko@sokrates.dev 0bc5d0318b3814ebd5b52605668756a8d5598e24 src/test/java/nl/obren/sokrates/codeexplorer/io/RegexUtilsTest.java\n" +
            "2020-07-14 zeljkoobrenovic@yahoo.com 41d8eaf1e2d00b17df0c31cdf34f1b43eada6a22 src/test/java/nl/obren/sokrates/reports/landscape/statichtml/LandscapeReportGeneratorTest.java\n" +
            "2020-05-18 zeljko@sokrates.dev cff81f4b4e05ce19c5ba81fe47c06fba83bb5cdf src/test/java/nl/obren/sokrates/reports/landscape/statichtml/LandscapeReportGeneratorTest.java\n" +
            "2020-05-17 zeljko@sokrates.dev bbada1d04949e6972a089780a3d044d8c9416eb4 src/test/java/nl/obren/sokrates/reports/landscape/statichtml/LandscapeReportGeneratorTest.java";

    @Test
    void splitFile() {
        ExtractGitHistoryFileHandler handler = new ExtractGitHistoryFileHandler();

        assertEquals(handler.extractSubHistory(Arrays.asList(SAMPLE.split("\n")), "common").stream().collect(Collectors.joining("\n")), RESULT_1);
        assertEquals(handler.extractSubHistory(Arrays.asList(SAMPLE.split("\n")), "codeexplorer").stream().collect(Collectors.joining("\n")), RESULT_2);
    }

    @Test
    void splitCopiesOptionalSidecars() throws Exception {
        File folder = Files.createTempDirectory("git-history-split").toFile();
        File historyFile = new File(folder, GitHistoryUtils.GIT_HISTORY_FILE_NAME);
        Files.write(historyFile.toPath(), Arrays.asList(SAMPLE.split("\n")), StandardCharsets.UTF_8);
        Files.write(new File(folder, GitHistoryUtils.GIT_COMMIT_TRAILERS_FILE_NAME).toPath(),
                Arrays.asList("abc Co-authored-by: Claude <noreply@anthropic.com>"), StandardCharsets.UTF_8);

        new ExtractGitHistoryFileHandler().extractSubHistory(historyFile, "common");

        File splitFolder = new File(folder, "common");
        assertTrue(new File(splitFolder, GitHistoryUtils.GIT_HISTORY_FILE_NAME).exists());
        assertTrue(new File(splitFolder, GitHistoryUtils.GIT_COMMIT_TRAILERS_FILE_NAME).exists());
        // git-commits.txt was absent in the source folder, so it is not created either
        assertFalse(new File(splitFolder, GitHistoryUtils.GIT_COMMITS_FILE_NAME).exists());
        assertEquals("abc Co-authored-by: Claude <noreply@anthropic.com>",
                Files.readAllLines(new File(splitFolder, GitHistoryUtils.GIT_COMMIT_TRAILERS_FILE_NAME).toPath()).get(0));
    }
}
