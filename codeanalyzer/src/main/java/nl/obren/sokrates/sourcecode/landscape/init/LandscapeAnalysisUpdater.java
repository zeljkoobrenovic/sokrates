/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.init;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.LandscapeInfo;
import nl.obren.sokrates.sourcecode.landscape.SokratesRepositoryLink;
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
        File landscapeInfoFile = getLandscapeInfoFile(analysisRoot);
        LandscapeConfiguration newConfig = getNewConfig(analysisRoot, landscapeConfigFile);

        if (landscapeConfigFile.exists()) {
            try {
                String json = FileUtils.readFileToString(landscapeConfigFile, StandardCharsets.UTF_8);
                LOG.info("Updating landscape '" + landscapeConfigFile.getAbsolutePath() + "' ...");
                LandscapeConfiguration existingConfiguration = (LandscapeConfiguration) new JsonMapper().getObject(json, LandscapeConfiguration.class);
                existingConfiguration.setSubLandscapes(newConfig.getSubLandscapes());
                existingConfiguration.setRepositories(newConfig.getRepositories());
                updateMetadata(existingConfiguration, metadata);
                save(landscapeConfigFile, landscapeInfoFile, existingConfiguration);
                return existingConfiguration;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        updateMetadata(newConfig, metadata);
        save(landscapeConfigFile, landscapeInfoFile, newConfig);
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

    private File getLandscapeInfoFile(File analysisRoot) {
        File landscapeAnalysisRoot = new File(analysisRoot, "_sokrates_landscape");
        return new File(landscapeAnalysisRoot, "info.json");
    }

    private LandscapeConfiguration getNewConfig(File analysisRoot, File landscapeConfigFile) {
        LandscapeAnalysisInitiator initiator = new LandscapeAnalysisInitiator();
        LandscapeConfiguration newConfig = initiator.initConfiguration(analysisRoot, landscapeConfigFile, false);
        return newConfig;
    }

    private void save(File landscapeConfigFile, File landscapeInfoFile, LandscapeConfiguration landscapeConfiguration) {
        try {
            LandscapeInfo info = new LandscapeInfo();
            info.setSubLandscapes(landscapeConfiguration.getSubLandscapes());
            info.setRepositories(landscapeConfiguration.getRepositories());
            String jsonInfo = new JsonGenerator().generate(info);
            FileUtils.write(landscapeInfoFile, jsonInfo, StandardCharsets.UTF_8);

            String json = new JsonGenerator().generate(landscapeConfiguration);
            FileUtils.write(landscapeConfigFile, json, StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
