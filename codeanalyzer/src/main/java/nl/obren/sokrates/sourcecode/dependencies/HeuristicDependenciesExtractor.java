/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.dependencies;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class HeuristicDependenciesExtractor {

    public DependenciesAnalysis extractDependencies(List<SourceFile> files, ProgressFeedback progressFeedback) {
        DependenciesAnalysis dependenciesAnalysis = new DependenciesAnalysis();
        List<Dependency> dependencies = new ArrayList<>();
        dependenciesAnalysis.setDependencies(dependencies);
        List<DependencyAnchor> anchors = getDependencyAnchors(files);
        // Shared index so addDependency merges duplicates in O(1) instead of scanning the list.
        Map<String, Dependency> dependencyIndex = new HashMap<>();

        // For each source file, scan its content once to collect the anchor strings it actually
        // mentions (substring match). Only those candidate target anchors are then resolved against
        // the file, instead of testing every target anchor against every file (which was
        // O(anchors^2 * files), each step re-scanning the whole file content).
        int i = 0;
        for (DependencyAnchor sourceAnchor : anchors) {
            if (progressFeedback.canceled()) {
                break;
            }
            progressFeedback.setText(sourceAnchor.getAnchor());
            for (SourceFile sourceFile : sourceAnchor.getSourceFiles()) {
                String content = sourceFile.getContent();
                for (DependencyAnchor targetAnchor : anchors) {
                    if (sourceAnchor != targetAnchor && content.contains(targetAnchor.getAnchor())) {
                        extractDependenciesToTargetAnchor(dependencies, dependencyIndex, content, sourceFile, sourceAnchor, targetAnchor);
                    }
                }
            }
            progressFeedback.progress(i++, anchors.size());
            if (progressFeedback instanceof DependencyProgressFeedback) {
                ((DependencyProgressFeedback) progressFeedback).setCurrentDependencies(dependencies);
            }
        }

        return dependenciesAnalysis;
    }

    private void extractDependenciesToTargetAnchor(List<Dependency> dependencies, Map<String, Dependency> dependencyIndex,
                                                   String content, SourceFile sourceFile, DependencyAnchor sourceAnchor, DependencyAnchor targetAnchor) {
        String dependencyCodeFragment = targetAnchor.getDependencyCodeFragment(content);
        if (dependencyCodeFragment != null) {
            SourceFileDependency sourceFileDependency = new SourceFileDependency(sourceFile);
            sourceFileDependency.setCodeFragment(dependencyCodeFragment);
            DependencyUtils.addDependency(dependencies, dependencyIndex, sourceFileDependency, sourceAnchor, targetAnchor);
        }
    }

    public List<DependencyAnchor> getDependencyAnchors(List<SourceFile> files) {
        Map<String, DependencyAnchor> anchorMap = new HashMap<>();
        List<DependencyAnchor> anchors = new ArrayList<>();

        for (SourceFile sourceFile : files) {
            List<DependencyAnchor> sourceFileAnchors = extractDependencyAnchors(sourceFile);
            sourceFileAnchors.forEach(dependencyAnchor -> {
                if (anchorMap.containsKey(dependencyAnchor.getAnchor())) {
                    anchorMap.get(dependencyAnchor.getAnchor()).getSourceFiles().addAll(dependencyAnchor.getSourceFiles());
                } else {
                    anchorMap.put(dependencyAnchor.getAnchor(), dependencyAnchor);
                    anchors.add(dependencyAnchor);
                }
            });
        }

        return anchors;
    }


    public abstract List<DependencyAnchor> extractDependencyAnchors(SourceFile sourceFile);
}
