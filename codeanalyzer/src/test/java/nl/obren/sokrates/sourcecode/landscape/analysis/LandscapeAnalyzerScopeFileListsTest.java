/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.analysis;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A repository's scope file lists are read under the names its own aspects produce, not under five
 * literals.
 *
 * <p>The literals and the default names agree, which is why the difference stayed invisible: it shows
 * only once somebody renames a scope aspect in config.json, which the configuration model allows. A
 * missing entry reads as null here, so the scope contributed no files and the landscape reported a
 * smaller repository - without an exception or a line in the log.
 */
public class LandscapeAnalyzerScopeFileListsTest {

    /** One file per scope, main with two, in the order the analyzer reads the scopes. */
    private static final String ALL_SCOPES = "[main/src/A.java, main/src/B.java, test/src/ATest.java, "
            + "generated/gen/G.java, build/build/deploy.sh, other/docs/notes.md]";

    @Test
    public void aRenamedScopeAspectStillContributesItsFiles() throws Exception {
        // The guard. Before the fix this repository contributed 0 main files instead of 2.
        CodeConfiguration configuration = CodeConfiguration.getDefaultConfiguration();
        configuration.getMain().setName("production code");
        File landscapeConfigFile = landscapeOverRepository(configuration);

        List<FileExport> files = filesOfTheOnlyRepository(landscapeConfigFile);

        assertEquals(ALL_SCOPES, describe(files));
    }

    @Test
    public void theDefaultAspectNamesAreReadAsBefore() throws Exception {
        // Every existing landscape reads these names; a derivation that produced something subtly
        // different - "aspect_build_and_deployment" is getSafeFileName's doing, not an obvious
        // transformation of "build and deployment" - would empty every repository in it.
        CodeConfiguration configuration = CodeConfiguration.getDefaultConfiguration();
        File landscapeConfigFile = landscapeOverRepository(configuration);

        List<FileExport> files = filesOfTheOnlyRepository(landscapeConfigFile);

        assertEquals(ALL_SCOPES, describe(files));
    }

    @Test
    public void aRepositoryWithoutAConfigurationIsReadUnderTheDefaultNames() throws Exception {
        // Nothing guarantees a readable config.json next to the analysis results: a data folder can
        // be missing one, and an unparseable one leaves the configuration null just the same -
        // JsonMapper turns a parse error into an IllegalArgumentException, which the catch in
        // getRepositoryAnalysisResults swallows. The default names are then the best remaining guess,
        // and what such a repository was read under before the names followed the configuration.
        File landscapeConfigFile = landscapeOverRepository(CodeConfiguration.getDefaultConfiguration());
        assertTrue(configFileOf(landscapeConfigFile).delete());

        List<FileExport> files = filesOfTheOnlyRepository(landscapeConfigFile);

        assertEquals(ALL_SCOPES, describe(files));
    }

    @Test
    public void aScopeSetToNullInTheConfigurationIsReadUnderItsDefaultName() throws Exception {
        // config.json is hand-edited, and of the five scope setters only setMain rejects a null - so
        // "test": null reaches the analyzer as a null aspect. Asking it for its name would throw out
        // of analyze(), whose only handler catches IOException, taking the whole landscape with it.
        File landscapeConfigFile = landscapeOverRepository(CodeConfiguration.getDefaultConfiguration());
        write(configFileOf(landscapeConfigFile), "{\"test\": null}");

        List<FileExport> files = filesOfTheOnlyRepository(landscapeConfigFile);

        assertEquals(ALL_SCOPES, describe(files));
    }

    @Test
    public void theFileListNamesFollowTheAspectNames() {
        CodeConfiguration configuration = CodeConfiguration.getDefaultConfiguration();

        assertEquals("aspect_main.txt", LandscapeAnalyzer.scopeFileListName(configuration.getMain()));
        assertEquals("aspect_build_and_deployment.txt",
                LandscapeAnalyzer.scopeFileListName(configuration.getBuildAndDeployment()));

        configuration.getMain().setName("production code");
        assertEquals("aspect_production_code.txt", LandscapeAnalyzer.scopeFileListName(configuration.getMain()));
    }

    /**
     * A landscape over one repository holding a file in each scope, exported the way the given
     * configuration names its file lists. The loose-file layout (no data.zip) is one the analyzer
     * still reads, and keeps the fixture to plain files.
     */
    private File landscapeOverRepository(CodeConfiguration configuration) throws IOException {
        File root = Files.createTempDirectory("landscape-scope-file-lists").toFile();
        root.deleteOnExit();

        File dataFolder = new File(root, "repository/_sokrates/reports/data");
        write(new File(dataFolder, "analysisResults.json"), "{\"metadata\": {\"name\": \"Repository\"}}");
        write(new File(dataFolder, "config.json"), new JsonGenerator().generate(configuration));

        writeFileList(dataFolder, configuration.getMain(), "src/A.java\t10\nsrc/B.java\t20\n");
        writeFileList(dataFolder, configuration.getTest(), "src/ATest.java\t5\n");
        writeFileList(dataFolder, configuration.getGenerated(), "gen/G.java\t7\n");
        writeFileList(dataFolder, configuration.getBuildAndDeployment(), "build/deploy.sh\t3\n");
        writeFileList(dataFolder, configuration.getOther(), "docs/notes.md\t1\n");

        File landscapeConfigFile = new File(root, "_sokrates_landscape/config.json");
        write(landscapeConfigFile, "{\"analysisRoot\": \"" + root.getAbsolutePath() + "\"}");
        // The repositories of a landscape live in info.json, next to its config.json.
        write(new File(landscapeConfigFile.getParentFile(), "info.json"),
                "{\"repositories\": [{\"analysisResultsPath\":"
                        + " \"repository/_sokrates/reports/data/analysisResults.json\"}]}");
        return landscapeConfigFile;
    }

    /** Written under the name the exporter derives from the aspect, not under a literal. */
    private void writeFileList(File dataFolder, NamedSourceCodeAspect aspect, String rows) throws IOException {
        String fileName = "aspect_" + aspect.getFileSystemFriendlyName("") + ".txt";
        write(new File(dataFolder, "text/" + fileName), "Path\tLines of Code\n" + rows);
    }

    private File configFileOf(File landscapeConfigFile) {
        return new File(landscapeConfigFile.getParentFile().getParentFile(),
                "repository/_sokrates/reports/data/config.json");
    }

    private List<FileExport> filesOfTheOnlyRepository(File landscapeConfigFile) {
        List<RepositoryAnalysisResults> repositories =
                new LandscapeAnalyzer().analyze(landscapeConfigFile).getRepositoryAnalysisResults();
        assertEquals(1, repositories.size());
        return repositories.get(0).getFiles();
    }

    /** Scope and path of each file, so a file found under the wrong scope fails too. */
    private String describe(List<FileExport> files) {
        return files.stream().map(file -> file.getScope() + "/" + file.getPath()).collect(Collectors.toList()).toString();
    }

    private void write(File file, String content) throws IOException {
        FileUtils.write(file, content, StandardCharsets.UTF_8);
    }
}
