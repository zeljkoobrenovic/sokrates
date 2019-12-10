/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.scoping;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.ExtensionGroupExtractor;
import nl.obren.sokrates.sourcecode.SourceCodeFiles;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ScopeCreator {
    private File srcRoot;
    private File confFile;

    public ScopeCreator(File srcRoot, File confFile) {
        this.srcRoot = srcRoot;
        this.confFile = confFile;
    }

    public void createScopeFromConventions() throws IOException {
        List<String> extensions = getExtensions();

        CodeConfiguration codeConfiguration = getCodeConfiguration(extensions);
        SourceCodeFiles sourceCodeFiles = getSourceCodeFiles(extensions);

        expandScopeWithConventions(codeConfiguration, sourceCodeFiles);

        saveScope(codeConfiguration);
    }

    private List<String> getExtensions() {
        ExtensionGroupExtractor extractor = new ExtensionGroupExtractor();
        extractor.extractExtensionsInfo(srcRoot);

        return getExtensions(extractor);
    }

    private List<String> getExtensions(ExtensionGroupExtractor extractor) {
        List<String> extensions = new ArrayList<>();
        extractor.getExtensionsList()
                .stream().filter(e -> ExtensionGroupExtractor.isKnownSourceCodeExtension(e.getExtension()))
                .forEach(extensionGroup -> {
                    extensions.add(extensionGroup.getExtension());
                });
        return extensions;
    }

    private CodeConfiguration getCodeConfiguration(List<String> extensions) {
        CodeConfiguration codeConfiguration = CodeConfiguration.getDefaultConfiguration();
        codeConfiguration.setExtensions(extensions);
        return codeConfiguration;
    }

    private SourceCodeFiles getSourceCodeFiles(List<String> extensions) {
        SourceCodeFiles sourceCodeFiles = getSourceCodeFiles();
        sourceCodeFiles.createBroadScope(extensions, new ArrayList<>(), false);
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
