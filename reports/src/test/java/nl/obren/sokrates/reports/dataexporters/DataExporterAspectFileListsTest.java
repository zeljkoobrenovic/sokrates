/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.dataexporters;

import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * The five scope file lists that go into all_files.zip are named by the aspects, not by five literals.
 *
 * <p>The literals and the default names agree, which is why the difference stayed invisible: it appears
 * only once somebody renames a scope aspect in config.json, which the configuration model allows.
 */
public class DataExporterAspectFileListsTest {

    @Test
    public void theDefaultAspectNamesProduceTheHistoricalFileNames() throws Exception {
        // The regression guard. Every existing repository uses these names, and a fix that derived
        // something subtly different - "aspect_build_and_deployment" is getSafeFileName's doing, not an
        // obvious transformation of "build and deployment" - would change what every report contains.
        CodeConfiguration configuration = CodeConfiguration.getDefaultConfiguration();
        File folder = writeFileListsFor(configuration);

        List<String> names = namesOf(DataExporter.aspectFileLists(configuration, folder));

        assertEquals("[aspect_main.txt, aspect_test.txt, aspect_generated.txt, "
                + "aspect_build_and_deployment.txt, aspect_other.txt]", names.toString());
    }

    @Test
    public void aRenamedScopeAspectIsReadUnderTheNameItWasWrittenWith() throws Exception {
        // Before this, the read was a literal: a rename left it opening a file nothing had written, the
        // NoSuchFileException went into the enclosing catch, and the rest of that block went with it -
        // costing all_files.zip and git-history.zip while the report still exited 0.
        CodeConfiguration configuration = CodeConfiguration.getDefaultConfiguration();
        configuration.getBuildAndDeployment().setName("buildAndDeployment");
        File folder = writeFileListsFor(configuration);

        List<String> names = namesOf(DataExporter.aspectFileLists(configuration, folder));

        assertEquals("[aspect_main.txt, aspect_test.txt, aspect_generated.txt, "
                + "aspect_buildAndDeployment.txt, aspect_other.txt]", names.toString());
    }

    @Test
    public void theContentOfEachFileIsCarriedIntoTheEntry() throws Exception {
        // Names alone would pass while the zip carried empty entries.
        CodeConfiguration configuration = CodeConfiguration.getDefaultConfiguration();
        File folder = writeFileListsFor(configuration);

        String[][] entries = DataExporter.aspectFileLists(configuration, folder);

        assertEquals("contents of aspect_main.txt", entries[0][1]);
    }

    /** A text folder holding exactly the files the exporter writes for this configuration. */
    private File writeFileListsFor(CodeConfiguration configuration) throws IOException {
        File folder = Files.createTempDirectory("aspect-file-lists").toFile();
        folder.deleteOnExit();
        for (NamedSourceCodeAspect aspect : new NamedSourceCodeAspect[]{
                configuration.getMain(), configuration.getTest(), configuration.getGenerated(),
                configuration.getBuildAndDeployment(), configuration.getOther()}) {
            String name = DataExportUtils.getAspectFileListFileName(aspect, "");
            FileUtils.write(new File(folder, name), "contents of " + name, StandardCharsets.UTF_8);
        }
        return folder;
    }

    private List<String> namesOf(String[][] entries) {
        List<String> names = new ArrayList<>();
        for (String[] entry : entries) {
            names.add(entry[0]);
        }
        return names;
    }
}
