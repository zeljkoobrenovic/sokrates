/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.aspects;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import nl.obren.sokrates.sourcecode.operations.ComplexOperation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

interface MetaRulesProcessorCallback {
    NamedSourceCodeAspect getInstance(String name);

    void updateSourceFile(SourceFile sourceFile, NamedSourceCodeAspect aspect);
}

public class MetaRulesProcessor<T extends NamedSourceCodeAspect> {
    private static final Log LOG = LogFactory.getLog(MetaRulesProcessor.class);
    private List<T> concerns = new ArrayList<>();
    private List<SourceFile> alreadyAddedFiles = new ArrayList<>();
    private Map<String, T> map = new HashMap<>();
    private MetaRulesProcessorCallback sourceCodeAspectFactory;
    private boolean uniqueClassification;

    private MetaRulesProcessor(boolean uniqueClassification,
                               MetaRulesProcessorCallback sourceCodeAspectFactory) {
        this.uniqueClassification = uniqueClassification;
        this.sourceCodeAspectFactory = sourceCodeAspectFactory;
    }

    public static MetaRulesProcessor getConcernsInstance() {
        return new MetaRulesProcessor<Concern>(false, new MetaRulesProcessorCallback() {
            @Override
            public NamedSourceCodeAspect getInstance(String name) {
                return new Concern(name);
            }

            @Override
            public void updateSourceFile(SourceFile sourceFile, NamedSourceCodeAspect aspect) {
                sourceFile.getConcerns().add(aspect);
            }
        });
    }

    public static MetaRulesProcessor getLogicalDecompositionInstance() {
        return new MetaRulesProcessor<NamedSourceCodeAspect>(true, new MetaRulesProcessorCallback() {
            @Override
            public NamedSourceCodeAspect getInstance(String name) {
                return new NamedSourceCodeAspect(name);
            }

            @Override
            public void updateSourceFile(SourceFile sourceFile, NamedSourceCodeAspect aspect) {
                sourceFile.getLogicalComponents().add(aspect);
            }
        });
    }

    public boolean isUniqueClassification() {
        return uniqueClassification;
    }

    public void setUniqueClassification(boolean uniqueClassification) {
        this.uniqueClassification = uniqueClassification;
    }

    public List<T> extractAspects(List<SourceFile> sourceFiles, List<MetaRule> metaRules) {
        concerns = new ArrayList<>();
        map = new HashMap<>();
        alreadyAddedFiles = new ArrayList<>();

        sourceFiles.forEach(sourceFile -> {
            processSourceFile(metaRules, sourceFile);
        });

        return concerns;
    }

    private void processSourceFile(List<MetaRule> metaRules, SourceFile sourceFile) {
        metaRules.forEach(metaRule -> {
            SourceFileFilter sourceFileFilter = new SourceFileFilter(metaRule.getPathPattern(), metaRule.getContentPattern());
            if (sourceFileFilter.pathMatches(sourceFile.getRelativePath())) {
                processSourceFileContent(sourceFile, metaRule);
            }
        });
    }

    private void processSourceFileContent(SourceFile sourceFile, MetaRule metaRule) {
        if (stopProcessing(metaRule)) {
            return;
        }
        getLines(sourceFile, metaRule).forEach(line -> {
            if (matches(metaRule, line)) {
                if (shouldProcessFile(sourceFile)) {
                    processMatchingString(sourceFile, metaRule, line);
                }
            }
        });
    }

    private boolean stopProcessing(MetaRule metaRule) {
        if (metaRule.getUse().equalsIgnoreCase("path")) {
            return StringUtils.isBlank(metaRule.getPathPattern());
        } else {
            return StringUtils.isBlank(metaRule.getContentPattern());
        }
    }

    private boolean matches(MetaRule metaRule, String line) {
        if (metaRule.getUse().equalsIgnoreCase("path")) {
            return RegexUtils.matchesEntirely(metaRule.getPathPattern(), line);
        } else {
            return RegexUtils.matchesEntirely(metaRule.getContentPattern(), line);
        }
    }

    private List<String> getLines(SourceFile sourceFile, MetaRule metaRule) {
        if (metaRule.getUse().equalsIgnoreCase("path")) {
            return Arrays.asList(sourceFile.getRelativePath());
        } else {
            return metaRule.isIgnoreComments() ? sourceFile.getCleanedLines() : sourceFile.getLines();
        }
    }

    private boolean shouldProcessFile(SourceFile sourceFile) {
        return !uniqueClassification || !alreadyAddedFiles.contains(sourceFile);
    }

    private void processMatchingString(SourceFile sourceFile, MetaRule metaRule, String matchingString) {
        updateAlreadyProcessedFiles(sourceFile);
        String name = new ComplexOperation(metaRule.getNameOperations()).exec(matchingString);
        if (StringUtils.isNotBlank(name)) {
            if (map.containsKey(name)) {
                T sourceCodeAspect = map.get(name);
                List<SourceFile> sourceFiles = sourceCodeAspect.getSourceFiles();
                if (!sourceFiles.contains(sourceFile)) {
                    sourceFiles.add(sourceFile);
                    sourceCodeAspectFactory.updateSourceFile(sourceFile, sourceCodeAspect);
                }
            } else {
                NamedSourceCodeAspect sourceCodeAspect = sourceCodeAspectFactory.getInstance(name);
                sourceCodeAspect.getSourceFiles().add(sourceFile);

                sourceCodeAspectFactory.updateSourceFile(sourceFile, sourceCodeAspect);

                map.put(name, (T) sourceCodeAspect);
                concerns.add((T) sourceCodeAspect);
            }
        }
    }

    private void updateAlreadyProcessedFiles(SourceFile sourceFile) {
        if (uniqueClassification && !alreadyAddedFiles.contains(sourceFile)) {
            alreadyAddedFiles.add(sourceFile);
        }
    }

}

