/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.scoping;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.ExtensionGroup;
import nl.obren.sokrates.sourcecode.ExtensionGroupExtractor;
import nl.obren.sokrates.sourcecode.SourceCodeFiles;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.ConcernsGroup;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils;
import nl.obren.sokrates.sourcecode.scoping.custom.CustomExtensionConventions;
import nl.obren.sokrates.sourcecode.scoping.custom.CustomScopingConventions;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ScopeCreator {
    private File srcRoot;
    private File confFile;
    private CustomScopingConventions customScopingConventions;

    public ScopeCreator(File srcRoot, File confFile, CustomScopingConventions customScopingConventions) {
        this.srcRoot = srcRoot;
        this.confFile = confFile;
        this.customScopingConventions = customScopingConventions;
    }

    public void createScopeFromConventions() throws IOException {
        List<String> extensions = getExtensions();

        CodeConfiguration codeConfiguration = getCodeConfiguration(extensions);

        codeConfiguration.getMetadata().setName(srcRoot.getCanonicalFile().getName());

        SourceCodeFiles sourceCodeFiles = getSourceCodeFiles(extensions, codeConfiguration.getAnalysis().getMaxLineLength());

        if (customScopingConventions == null || !customScopingConventions.isIgnoreStandardScopingConventions()) {
            expandScopeWithConventions(codeConfiguration, sourceCodeFiles);
        }
        if (customScopingConventions != null) {
            expandScopeWithCustomConventions(codeConfiguration, sourceCodeFiles);
        }

        saveScope(codeConfiguration);
    }

    private void expandScopeWithCustomConventions(CodeConfiguration codeConfiguration, SourceCodeFiles sourceCodeFiles) {
        if (customScopingConventions.getMaxLineLength() > 0) {
            codeConfiguration.getAnalysis().setMaxLineLength(customScopingConventions.getMaxLineLength());
        }
        List<SourceFile> sourceFiles = sourceCodeFiles.getFilesInBroadScope();
        ConventionUtils.addConventions(customScopingConventions.getIgnoredFilesConventions(), codeConfiguration.getIgnore(), sourceFiles);
        ConventionUtils.addConventions(customScopingConventions.getTestFilesConventions(), codeConfiguration.getTest().getSourceFileFilters(), sourceFiles);
        ConventionUtils.addConventions(customScopingConventions.getGeneratedFilesConventions(), codeConfiguration.getGenerated().getSourceFileFilters(), sourceFiles);
        ConventionUtils.addConventions(customScopingConventions.getBuildAndDeploymentFilesConventions(), codeConfiguration.getBuildAndDeployment().getSourceFileFilters(), sourceFiles);
        ConventionUtils.addConventions(customScopingConventions.getOtherFilesConventions(), codeConfiguration.getOther().getSourceFileFilters(), sourceFiles);

        List<ConcernsGroup> concernGroups = codeConfiguration.getConcernGroups();
        if (customScopingConventions.isRemoveStandardConcerns()) {
            concernGroups.clear();
        }

        if (customScopingConventions.getConcerns().size() > 0) {
            if (concernGroups.size() == 0) {
                concernGroups.add(new ConcernsGroup("general"));
            }

            concernGroups.get(0).getConcerns().addAll(customScopingConventions.getConcerns());
        }

        codeConfiguration.getFileHistoryAnalysis().getIgnoreContributors().addAll(customScopingConventions.getIgnoreContributors());
    }

    private List<String> getExtensions() {
        ExtensionGroupExtractor extractor = new ExtensionGroupExtractor();
        extractor.extractExtensionsInfo(srcRoot);

        return getExtensions(extractor);
    }

    private List<String> getExtensions(ExtensionGroupExtractor extractor) {
        List<String> extensions = new ArrayList<>();
        extractor.getExtensionsList()
                .stream()
                .filter(e -> shouldIncludeExtension(e.getExtension()))
                .forEach(extensionGroup -> {
                    extensions.add(extensionGroup.getExtension());
                });
        return extensions;
    }

    private boolean shouldIncludeExtension(String extension) {
        if (customScopingConventions != null) {
            CustomExtensionConventions customExtensions = customScopingConventions.getExtensions();
            if (customExtensions.getOnlyInclude().size() > 0) {
                for (String onlyInclude : customExtensions.getOnlyInclude()) {
                    if (onlyInclude.equalsIgnoreCase(extension)) {
                        return true;
                    }
                }
                return false;
            }
            if (customExtensions.getAlwaysExclude().size() > 0) {
                for (String alwaysExclude : customExtensions.getAlwaysExclude()) {
                    if (alwaysExclude.equalsIgnoreCase(extension)) {
                        return false;
                    }
                }
            }
        }
        return ExtensionGroupExtractor.isKnownSourceCodeExtension(extension);
    }

    private CodeConfiguration getCodeConfiguration(List<String> extensions) {
        CodeConfiguration codeConfiguration = CodeConfiguration.getDefaultConfiguration();
        codeConfiguration.setExtensions(extensions);
        return codeConfiguration;
    }

    private SourceCodeFiles getSourceCodeFiles(List<String> extensions, int maxLineLength) {
        SourceCodeFiles sourceCodeFiles = getSourceCodeFiles();
        sourceCodeFiles.createBroadScope(extensions, new ArrayList<>(), maxLineLength);
        return sourceCodeFiles;
    }

    private SourceCodeFiles getSourceCodeFiles() {
        SourceCodeFiles sourceCodeFiles = new SourceCodeFiles();
        sourceCodeFiles.load(srcRoot, new ProgressFeedback() {
        });
        return sourceCodeFiles;
    }

    private void expandScopeWithConventions(CodeConfiguration codeConfiguration, SourceCodeFiles sourceCodeFiles) {
        ScopingConventions scopingConventions = new ScopingConventions();
        scopingConventions.addConventions(codeConfiguration, sourceCodeFiles.getFilesInBroadScope());
    }

    private void saveScope(CodeConfiguration codeConfiguration) throws IOException {
        String json = new JsonGenerator().generate(codeConfiguration);
        if (confFile == null) {
            confFile = CodeConfigurationUtils.getDefaultSokratesConfigFile(srcRoot);
        }
        FileUtils.writeStringToFile(confFile, json, StandardCharsets.UTF_8);
    }

}
