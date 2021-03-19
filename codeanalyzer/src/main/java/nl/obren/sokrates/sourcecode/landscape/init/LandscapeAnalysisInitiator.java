/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.init;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.SokratesProjectLink;
import nl.obren.sokrates.sourcecode.landscape.SubLandscapeLink;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class LandscapeAnalysisInitiator {
    private boolean saveFile;

    public LandscapeConfiguration initConfiguration(File analysisRoot, File landscapeConfigFile, boolean saveFile) {
        this.saveFile = saveFile;
        LandscapeConfiguration landscapeConfiguration = new LandscapeConfiguration();
        landscapeConfiguration.setAnalysisRoot(analysisRoot.getPath());

        try (Stream<Path> paths = Files.walk(Paths.get(analysisRoot.getPath()))) {
            paths.filter(file -> isSokratesLandscapeFile(file)).forEach(file -> {
                addSubLandscape(analysisRoot, landscapeConfiguration, file);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (Stream<Path> paths = Files.walk(Paths.get(analysisRoot.getPath()))) {
            paths.filter(file -> isSokratesAnalysisFile(file)).forEach(file -> {
                processAnalysisResultFile(analysisRoot, landscapeConfiguration, file);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (landscapeConfigFile == null) {
            File landscapeAnalysisRoot = new File(analysisRoot, "_sokrates_landscape");
            landscapeConfigFile = new File(landscapeAnalysisRoot, "config.json");
        }

        if (StringUtils.isBlank(landscapeConfiguration.getMetadata().getName())) {
            try {
                landscapeConfiguration.getMetadata().setName(analysisRoot.getCanonicalFile().getName());
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
                System.out.println("Adding sub-landscape: " + relativePath);
            }
        }
    }

    private void save(File landscapeConfigFile, LandscapeConfiguration landscapeConfiguration) {
        try {
            System.out.println("Saving landscape configuration file in " + landscapeConfigFile.getCanonicalPath());
            String json = new JsonGenerator().generate(landscapeConfiguration);
            FileUtils.write(landscapeConfigFile, json, StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isSokratesAnalysisFile(Path file) {
        return file.endsWith("data/analysisResults.json");
    }

    private boolean isSokratesLandscapeFile(Path file) {
        return file.endsWith("_sokrates_landscape/index.html");
    }

    private void processAnalysisResultFile(File root, LandscapeConfiguration configuration, Path file) {
        String relativePath = root.toPath().relativize(file).toString();
        configuration.getProjects().add(new SokratesProjectLink(relativePath));

        System.out.println("Adding project: " + relativePath);
    }
}
