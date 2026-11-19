/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.dependencies;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.aspects.SourceCodeAspectUtils;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzer;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DependenciesUtils {
    public static DependenciesAnalysis extractDependencies(NamedSourceCodeAspect aspect, boolean skipDependencies) {
        List<Dependency> allDependencies = new ArrayList<>();
        DependenciesAnalysis dependenciesAnalysis = new DependenciesAnalysis();
        dependenciesAnalysis.setDependencies(allDependencies);
        if (!skipDependencies) {
            getFileGroups(aspect).forEach(files -> {
                LanguageAnalyzer languageAnalyzer = LanguageAnalyzerFactory.getInstance().getLanguageAnalyzer(files.get(0));
                DependenciesAnalysis localAnalysis = languageAnalyzer.extractDependencies(files, new ProgressFeedback());
                allDependencies.addAll(localAnalysis.getDependencies());
                dependenciesAnalysis.getErrors().addAll(localAnalysis.getErrors());
            });
        }

        return dependenciesAnalysis;
    }

    /**
     * The aspect's files per dependency group: per extension (largest first), except that extensions
     * whose analyzer names a common {@link LanguageAnalyzer#getDependenciesGroup group} are merged
     * into the group of the first (largest) of them.
     */
    public static List<List<SourceFile>> getFileGroups(NamedSourceCodeAspect aspect) {
        Map<String, List<SourceFile>> groups = new LinkedHashMap<>();
        SourceCodeAspectUtils.getAspectsPerExtensions(aspect).forEach(langAspect -> {
            if (langAspect.getSourceFiles().size() > 0) {
                SourceFile sourceFileSample = langAspect.getSourceFiles().get(0);
                LanguageAnalyzer languageAnalyzer = LanguageAnalyzerFactory.getInstance().getLanguageAnalyzer(sourceFileSample);
                String group = languageAnalyzer.getClass().getName() + ":" + languageAnalyzer.getDependenciesGroup(langAspect.getName());
                groups.computeIfAbsent(group, k -> new ArrayList<>()).addAll(langAspect.getSourceFiles());
            }
        });
        return new ArrayList<>(groups.values());
    }
}
