/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.analysis;

import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.UnitsAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.SokratesProjectLink;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;

public class LandscapeAnalyzer {
    private File landscapeConfigurationFile;
    private LandscapeConfiguration landscapeConfiguration;

    public LandscapeAnalysisResults analyze(File landscapeConfigFile) {
        this.landscapeConfigurationFile = landscapeConfigFile;

        LandscapeAnalysisResults landscapeAnalysisResults = new LandscapeAnalysisResults();

        try {
            String json = FileUtils.readFileToString(landscapeConfigurationFile, StandardCharsets.UTF_8);
            this.landscapeConfiguration = (LandscapeConfiguration) new JsonMapper().getObject(json, LandscapeConfiguration.class);
            landscapeAnalysisResults.setConfiguration(landscapeConfiguration);
            landscapeConfiguration.getProjects().forEach(link -> {
                CodeAnalysisResults projectAnalysisResults = this.getProjectAnalysisResults(link);
                if (projectAnalysisResults != null) {
                    landscapeAnalysisResults.getProjectAnalysisResults().add(new ProjectAnalysisResults(link, projectAnalysisResults));
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return landscapeAnalysisResults;
    }

    private CodeAnalysisResults getProjectAnalysisResults(SokratesProjectLink sokratesProjectLink) {
        return getProjectAnalysisResults(getProjectAnalysisFile(sokratesProjectLink));
    }

    private File getProjectAnalysisFile(SokratesProjectLink sokratesProjectLink) {
        String analysisRoot = landscapeConfiguration.getAnalysisRoot();
        if (analysisRoot.startsWith("..")) {
            analysisRoot = Paths.get(landscapeConfigurationFile.getParentFile().getPath(), analysisRoot).toFile().getPath();
        }
        return Paths.get(analysisRoot, sokratesProjectLink.getAnalysisResultsPath()).toFile();
    }

    private CodeAnalysisResults getProjectAnalysisResults(File projectAnalysisResultsFile) {
        try {
            String json = FileUtils.readFileToString(projectAnalysisResultsFile, StandardCharsets.UTF_8);
            CodeAnalysisResults codeAnalysisResults = (CodeAnalysisResults) new JsonMapper().getObject(json, CodeAnalysisResults.class);
            codeAnalysisResults.setUnitsAnalysisResults(new UnitsAnalysisResults());
            codeAnalysisResults.setFilesAnalysisResults(new FilesAnalysisResults());
            codeAnalysisResults.setAllDependencies(new ArrayList<>());
            codeAnalysisResults.setFilesExcludedByExtension(new ArrayList<>());
            codeAnalysisResults.getMainAspectAnalysisResults().getAspect().getSourceFiles().clear();
            codeAnalysisResults.getTestAspectAnalysisResults().getAspect().getSourceFiles().clear();
            codeAnalysisResults.getGeneratedAspectAnalysisResults().getAspect().getSourceFiles().clear();
            codeAnalysisResults.getBuildAndDeployAspectAnalysisResults().getAspect().getSourceFiles().clear();
            codeAnalysisResults.getOtherAspectAnalysisResults().getAspect().getSourceFiles().clear();
            return codeAnalysisResults;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
