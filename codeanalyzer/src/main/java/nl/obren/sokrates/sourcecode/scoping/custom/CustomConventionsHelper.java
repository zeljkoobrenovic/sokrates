/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.scoping.custom;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.scoping.ScopingConventions;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CustomConventionsHelper {
    public static CustomScopingConventions exportStandardConventions() {
        ScopingConventions standardConventions = new ScopingConventions();
        CodeConfiguration defaultCodeConfiguration = CodeConfiguration.getDefaultConfiguration();
        CustomScopingConventions conventions = new CustomScopingConventions();

        conventions.setMaxLineLength(defaultCodeConfiguration.getAnalysis().getMaxLineLength());

        defaultCodeConfiguration.getConcernGroups().forEach(concernsGroup -> {
            conventions.getConcerns().addAll(concernsGroup.getConcerns());
        });

        conventions.setTestFilesConventions(standardConventions.getTestFilesConventions());
        conventions.setGeneratedFilesConventions(standardConventions.getGeneratedFilesConventions());
        conventions.setBuildAndDeploymentFilesConventions(standardConventions.getBuildAndDeploymentFilesConventions());
        conventions.setOtherFilesConventions(standardConventions.getOtherFilesConventions());

        return conventions;
    }

    public static void saveStandardConventionsToFile(File file) {
        try {
            String json = new JsonGenerator().generate(exportStandardConventions());
            FileUtils.writeStringToFile(file, json, StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static CustomScopingConventions readFromFile(File file) {
        try {
            String json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            return (CustomScopingConventions) new JsonMapper().getObject(json, CustomScopingConventions.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
