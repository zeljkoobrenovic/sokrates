package nl.obren.sokrates.sourcecode.landscape.init;

import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class LandscapeAnalysisInitiatorTest {

    @Test
    void storedAnalysisRootIsDotForTheDefaultLayout(@TempDir Path tmp) throws Exception {
        // A relative root (what the CLI passes when run from the parent folder: ./acme) must not be
        // stored as such — the analyzer resolves roots starting with "." against the landscape's
        // parent, so "./acme" would become "./acme/./acme" and no repository would be found.
        File relativeRoot = new File("target/initiator-test-root-" + System.nanoTime());
        try {
            assertEquals(".", LandscapeAnalysisInitiator.analysisRootPath(relativeRoot, null));
            assertEquals(".", LandscapeAnalysisInitiator.analysisRootPath(relativeRoot, new File(relativeRoot, "_sokrates_landscape/config.json")));
            assertEquals(".", LandscapeAnalysisInitiator.analysisRootPath(tmp.toFile(), new File(tmp.toFile(), "_sokrates_landscape/config.json")));
            // A configuration kept elsewhere keeps the root as given.
            assertEquals(tmp.toString(), LandscapeAnalysisInitiator.analysisRootPath(tmp.toFile(), new File(relativeRoot, "elsewhere/config.json")));

            LandscapeConfiguration created = new LandscapeAnalysisUpdater().updateConfiguration(relativeRoot, null, new Metadata());
            assertEquals(".", created.getAnalysisRoot());

            // An existing configuration with the broken relative form is healed on the next update.
            File configFile = new File(relativeRoot, "_sokrates_landscape/config.json");
            FileUtils.write(configFile, FileUtils.readFileToString(configFile, UTF_8).replace("\"analysisRoot\" : \".\"", "\"analysisRoot\" : \"./acme\""), UTF_8);
            assertTrue(FileUtils.readFileToString(configFile, UTF_8).contains("./acme"), "precondition");
            LandscapeConfiguration updated = new LandscapeAnalysisUpdater().updateConfiguration(relativeRoot, null, new Metadata());
            assertEquals(".", updated.getAnalysisRoot());
            assertTrue(FileUtils.readFileToString(configFile, UTF_8).contains("\"analysisRoot\" : \".\""));
        } finally {
            FileUtils.deleteDirectory(relativeRoot);
        }
    }

    @Test
    void aLandscapesOwnDataZipIsNotARepository(@TempDir Path tmp) throws Exception {
        File root = tmp.toFile();
        FileUtils.write(new File(root, "alpha/reports/data/data.zip"), "", UTF_8);
        FileUtils.write(new File(root, "legacy/reports/data/analysisResults.json"), "{}", UTF_8);
        FileUtils.write(new File(root, "_sokrates_landscape/data/data.zip"), "", UTF_8);                 // the landscape itself (re-run)
        FileUtils.write(new File(root, "sub/_sokrates_landscape/data/data.zip"), "", UTF_8);             // a folder sub-landscape
        FileUtils.write(new File(root, "sub/_sokrates_landscape/index.html"), "", UTF_8);
        FileUtils.write(new File(root, "sub/beta/reports/data/data.zip"), "", UTF_8);

        LandscapeConfiguration configuration = new LandscapeAnalysisInitiator().initConfiguration(root, null, false);

        List<String> repositories = configuration.getRepositories().stream()
                .map(r -> r.getAnalysisResultsPath().replace("\\", "/")).sorted().collect(Collectors.toList());
        assertEquals(List.of("alpha/reports/data/analysisResults.json", "legacy/reports/data/analysisResults.json", "sub/beta/reports/data/analysisResults.json"), repositories);
        assertEquals(1, configuration.getSubLandscapes().size());
        assertEquals("sub/_sokrates_landscape/index.html", configuration.getSubLandscapes().get(0).getIndexFilePath().replace("\\", "/"));
    }
}
