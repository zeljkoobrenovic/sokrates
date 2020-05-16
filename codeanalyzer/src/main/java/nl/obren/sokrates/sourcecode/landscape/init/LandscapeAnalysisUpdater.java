/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.init;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.LandscapeGroup;
import nl.obren.sokrates.sourcecode.landscape.SokratesProjectLink;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LandscapeAnalysisUpdater {
    public static final String NEW_PROJECTS_GROUP_NAME = "New Projects";
    public static final String REMOVED_PROJECTS_GROUP_NAME = "Removed Projects";

    public LandscapeConfiguration updateConfiguration(File analysisRoot, File landscapeConfigFile) {
        landscapeConfigFile = getLandscapeConfigFile(analysisRoot, landscapeConfigFile);
        LandscapeConfiguration newConfig = getNewConfig(analysisRoot, landscapeConfigFile);
        if (landscapeConfigFile.exists()) {
            try {
                String json = FileUtils.readFileToString(landscapeConfigFile, StandardCharsets.UTF_8);
                LandscapeConfiguration existingConfiguration = (LandscapeConfiguration) new JsonMapper().getObject(json, LandscapeConfiguration.class);
                removeNonExistingProjects(existingConfiguration);
                addNewProjects(newConfig, existingConfiguration);
                save(landscapeConfigFile, existingConfiguration);
                return existingConfiguration;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        save(landscapeConfigFile, newConfig);
        return newConfig;
    }

    private File getLandscapeConfigFile(File analysisRoot, File landscapeConfigFile) {
        if (landscapeConfigFile == null) {
            File landscapeAnalysisRoot = new File(analysisRoot, "_sokrates_landscape");
            landscapeConfigFile = new File(landscapeAnalysisRoot, "config.json");
        }
        return landscapeConfigFile;
    }

    private void addNewProjects(LandscapeConfiguration newConfig, LandscapeConfiguration existingConfiguration) {
        List<SokratesProjectLink> allProjects = getAllProjects(newConfig.getGroups());
        List<SokratesProjectLink> allPreviousProjects = getAllProjects(existingConfiguration.getGroups());

        List<SokratesProjectLink> newProjects = getNewProjects(allProjects, allPreviousProjects);

        if (newProjects.size() > 0) {
            LandscapeGroup newProjectsGroup = new LandscapeGroup();
            newProjectsGroup.getMetadata().setName(NEW_PROJECTS_GROUP_NAME);
            existingConfiguration.getGroups().add(0, newProjectsGroup);
            newProjectsGroup.getProjects().addAll(newProjects);
        }
    }

    private List<SokratesProjectLink> getNewProjects(List<SokratesProjectLink> allProjects, List<SokratesProjectLink> allPreviousProjects) {
        List<SokratesProjectLink> newProjects = new ArrayList<>();

        allProjects.forEach(project -> {
            if (!(allPreviousProjects.stream().filter(prevProject -> prevProject.getAnalysisResultsPath().equalsIgnoreCase(project.getAnalysisResultsPath())).findAny().isPresent())) {
                newProjects.add(project);
            }
        });

        return newProjects;
    }

    private List<SokratesProjectLink> getAllProjects(List<LandscapeGroup> groups) {
        List<SokratesProjectLink> projects = new ArrayList<>();

        groups.forEach(group -> {
            projects.addAll(group.getProjects());
            if (group.getSubGroups() != null) {
                projects.addAll(getAllProjects(group.getSubGroups()));
            }
        });

        return projects;
    }

    private void removeNonExistingProjects(LandscapeConfiguration existingConfiguration) {
        List<SokratesProjectLink> removedProjects = removeNonExistingProjects(existingConfiguration.getAnalysisRoot(), existingConfiguration.getGroups());
        if (removedProjects.size() > 0) {
            Optional<LandscapeGroup> deletedProjectsGroupOptional = existingConfiguration.getGroups().stream().filter(group -> group.getMetadata().getName().equalsIgnoreCase(REMOVED_PROJECTS_GROUP_NAME)).findAny();
            LandscapeGroup deletedProjectsGroup;
            if (deletedProjectsGroupOptional.isPresent()) {
                deletedProjectsGroup = deletedProjectsGroupOptional.get();
            } else {
                deletedProjectsGroup = new LandscapeGroup();
                deletedProjectsGroup.getMetadata().setName(REMOVED_PROJECTS_GROUP_NAME);
                existingConfiguration.getGroups().add(deletedProjectsGroup);
            }
            deletedProjectsGroup.getProjects().addAll(removedProjects);
        }
    }

    private List<SokratesProjectLink> removeNonExistingProjects(String analysisRoot, List<LandscapeGroup> groups) {
        List<SokratesProjectLink> removedProjects = new ArrayList<>();
        groups.forEach(group -> {
            group.getProjects().forEach(project -> {
                File analysisFile = Paths.get(analysisRoot, project.getAnalysisResultsPath()).toFile();
                if (!analysisFile.exists()) {
                    removedProjects.add(project);
                }
            });
            removedProjects.forEach(removedProject -> group.getProjects().remove(removedProject));
            if (group.getSubGroups() != null) {
                removedProjects.addAll(removeNonExistingProjects(analysisRoot, group.getSubGroups()));
            }
        });

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
