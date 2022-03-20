/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.init;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.SokratesProjectLink;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LandscapeAnalysisUpdater {
    private static final Log LOG = LogFactory.getLog(LandscapeAnalysisUtils.class);

    public LandscapeConfiguration updateConfiguration(File analysisRoot, File landscapeConfigFile, Metadata metadata) {
        landscapeConfigFile = getLandscapeConfigFile(analysisRoot, landscapeConfigFile);
        LandscapeConfiguration newConfig = getNewConfig(analysisRoot, landscapeConfigFile);
        if (landscapeConfigFile.exists()) {
            try {
                String json = FileUtils.readFileToString(landscapeConfigFile, StandardCharsets.UTF_8);
                LandscapeConfiguration existingConfiguration = (LandscapeConfiguration) new JsonMapper().getObject(json, LandscapeConfiguration.class);
                existingConfiguration.setSubLandscapes(newConfig.getSubLandscapes());
                existingConfiguration.setProjects(newConfig.getProjects());
                updateMetadata(existingConfiguration, metadata);
                save(landscapeConfigFile, existingConfiguration);
                return existingConfiguration;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        updateMetadata(newConfig, metadata);
        save(landscapeConfigFile, newConfig);
        return newConfig;
    }

    private void updateMetadata(LandscapeConfiguration configuration, Metadata metadata) {
        if (metadata != null) {
            if (StringUtils.isNotBlank(metadata.getName())) {
                configuration.getMetadata().setName(metadata.getName());
            }
            if (StringUtils.isNotBlank(metadata.getDescription())) {
                configuration.getMetadata().setDescription(metadata.getDescription());
            }
            if (StringUtils.isNotBlank(metadata.getLogoLink())) {
                configuration.getMetadata().setLogoLink(metadata.getLogoLink());
            }
            if (metadata.getLinks().size() > 0) {
                configuration.getMetadata().setLinks(metadata.getLinks());
            }
        }
    }

    private File getLandscapeConfigFile(File analysisRoot, File landscapeConfigFile) {
        if (landscapeConfigFile == null) {
            File landscapeAnalysisRoot = new File(analysisRoot, "_sokrates_landscape");
            landscapeConfigFile = new File(landscapeAnalysisRoot, "config.json");
        }
        return landscapeConfigFile;
    }

    private void addNewProjects(LandscapeConfiguration newConfig, LandscapeConfiguration existingConfiguration) {
        List<SokratesProjectLink> allProjects = newConfig.getProjects();
        List<SokratesProjectLink> allPreviousProjects = existingConfiguration.getProjects();

        List<SokratesProjectLink> newProjects = getNewProjects(allProjects, allPreviousProjects);

        if (newProjects.size() > 0) {
            existingConfiguration.getProjects().addAll(newProjects);
        }
    }

    private List<SokratesProjectLink> getNewProjects(List<SokratesProjectLink> allProjects, List<SokratesProjectLink> allPreviousProjects) {
        List<SokratesProjectLink> newProjects = new ArrayList<>();

        allProjects.forEach(project -> {
            if (!(allPreviousProjects.stream().filter(prevProject -> prevProject.getAnalysisResultsPath().equalsIgnoreCase(project.getAnalysisResultsPath())).findAny().isPresent())) {
                LOG.info("Adding project: " + project.getAnalysisResultsPath());
                newProjects.add(project);
            }
        });

        return newProjects;
    }

    private List<SokratesProjectLink> removeNonExistingProjects(LandscapeConfiguration landscapeConfiguration) {
        String analysisRoot = landscapeConfiguration.getAnalysisRoot();
        List<SokratesProjectLink> removedProjects = new ArrayList<>();
        landscapeConfiguration.getProjects().forEach(project -> {
            File analysisFile = Paths.get(analysisRoot, project.getAnalysisResultsPath()).toFile();
            if (!analysisFile.exists()) {
                LOG.info("Removing project: " + project.getAnalysisResultsPath());
                removedProjects.add(project);
            }
        });

        removedProjects.forEach(removedProject -> landscapeConfiguration.getProjects().remove(removedProject));

        return removedProjects;
    }

    private LandscapeConfiguration getNewConfig(File analysisRoot, File landscapeConfigFile) {
        LandscapeAnalysisInitiator initiator = new LandscapeAnalysisInitiator();
        LandscapeConfiguration newConfig = initiator.initConfiguration(analysisRoot, landscapeConfigFile, false);
        return newConfig;
    }

    private void save(File landscapeConfigFile, LandscapeConfiguration landscapeConfiguration) {
        try {
            String json = new JsonGenerator().generate(landscapeConfiguration);
            FileUtils.write(landscapeConfigFile, json, StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
