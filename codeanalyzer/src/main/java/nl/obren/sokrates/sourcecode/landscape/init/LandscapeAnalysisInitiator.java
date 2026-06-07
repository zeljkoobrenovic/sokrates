/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.init;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.sourcecode.Link;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.SokratesRepositoryLink;
import nl.obren.sokrates.sourcecode.landscape.SubLandscapeLink;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class LandscapeAnalysisInitiator {
    private static final Log LOG = LogFactory.getLog(LandscapeAnalysisInitiator.class);

    private boolean saveFile;

    public LandscapeConfiguration initConfiguration(File analysisRoot, File landscapeConfigFile, boolean saveFile) {
        this.saveFile = saveFile;
        LandscapeConfiguration landscapeConfiguration = new LandscapeConfiguration();
        landscapeConfiguration.setAnalysisRoot(analysisRoot.getPath());

        // Single tree walk: sub-landscape and analysis-result files are mutually exclusive (distinct
        // path endings) and feed separate lists, so dispatching on type in one pass is equivalent to
        // two walks but halves the traversal of (potentially large) landscape roots.
        try (Stream<Path> paths = Files.walk(Paths.get(analysisRoot.getPath()))) {
            paths.filter(file -> !LandscapeAnalysisUtils.isInGeneratedVirtualLandscape(file)).forEach(file -> {
                if (isSokratesLandscapeFile(file)) {
                    addSubLandscape(analysisRoot, landscapeConfiguration, file);
                } else if (isSokratesAnalysisFile(file)) {
                    processAnalysisResultFile(analysisRoot, landscapeConfiguration, file);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (landscapeConfigFile == null) {
            File landscapeAnalysisRoot = new File(analysisRoot, "_sokrates_landscape");
            landscapeConfigFile = new File(landscapeAnalysisRoot, "config.json");
        }

        Metadata metadata = landscapeConfiguration.getMetadata();
        if (StringUtils.isBlank(metadata.getName())) {
            try {
                String name = analysisRoot.getCanonicalFile().getName();
                name = StringUtils.capitalize(name.toLowerCase()) + " Landscape";
                metadata.setName(name);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (saveFile) {
            save(landscapeConfigFile, landscapeConfiguration);
        }

        return landscapeConfiguration;
    }

    private void addSubLandscape(File root, LandscapeConfiguration configuration, Path file) {
        String relativePath = root.toPath().relativize(file).toString();

        File parent = new File(relativePath).getParentFile().getParentFile();
        if (parent != null) {
            SubLandscapeLink subLandscapeLink = new SubLandscapeLink(parent.getName(), relativePath);
            configuration.getSubLandscapes().add(subLandscapeLink);
            if (saveFile) {
                LOG.info("Adding sub-landscape: " + relativePath);
            }
        }
    }

    private void save(File landscapeConfigFile, LandscapeConfiguration landscapeConfiguration) {
        try {
            LOG.info("Saving landscape configuration file in " + landscapeConfigFile.getCanonicalPath());
            String json = new JsonGenerator().generate(landscapeConfiguration);
            FileUtils.write(landscapeConfigFile, json, StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isSokratesAnalysisFile(Path file) {
        // A repository's data folder is now packaged as data/data.zip (analysisResults.json lives
        // inside it). Discover repositories by that zip; the repository link still records the
        // conceptual data/analysisResults.json path (the analyzer resolves the data folder from it).
        return file.endsWith("data/data.zip");
    }

    private boolean isSokratesLandscapeFile(Path file) {
        return file.endsWith("_sokrates_landscape/index.html");
    }

    private void processAnalysisResultFile(File root, LandscapeConfiguration configuration, Path file) {
        // file points at the discovered data/data.zip; the repository link keeps the conceptual
        // data/analysisResults.json path (its parent folder is what the analyzer reads the zip from).
        String relativePath = root.toPath().relativize(file).toString()
                .replaceAll("data\\.zip$", "analysisResults.json");
        configuration.getRepositories().add(new SokratesRepositoryLink(relativePath));

        LOG.info("Adding repositories: " + relativePath);
    }
}
