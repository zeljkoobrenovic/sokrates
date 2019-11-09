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

        DependencyUtils.findErrors(anchors, dependenciesAnalysis.getErrors());

        int i = 0;
        for (DependencyAnchor sourceAnchor : anchors) {
            if (progressFeedback.canceled()) {
                break;
            }
            progressFeedback.setText(sourceAnchor.getAnchor());
            for (DependencyAnchor targetAnchor : anchors) {
                if (sourceAnchor != targetAnchor) {
                    sourceAnchor.getSourceFiles().forEach(sourceFile -> {
                        extractDependenciesToTargetAnchor(dependencies, sourceFile, sourceAnchor, targetAnchor);
                    });
                }
            }
            progressFeedback.progress(i++, anchors.size());
            if (progressFeedback instanceof DependencyProgressFeedback) {
                ((DependencyProgressFeedback) progressFeedback).setCurrentDependencies(dependencies);
            }
        }

        return dependenciesAnalysis;
    }

    private void extractDependenciesToTargetAnchor(List<Dependency> dependencies, SourceFile sourceFile, DependencyAnchor sourceAnchor, DependencyAnchor targetAnchor) {
        if (DependencyUtils.sourceAndTargetInSameComponent(sourceFile, targetAnchor)) {
            return;
        }
        String content = sourceFile.getContent();
        if (content.contains(targetAnchor.getAnchor())) {
            String dependencyCodeFragment = targetAnchor.getDependencyCodeFragment(content);
            if (dependencyCodeFragment != null) {
                SourceFileDependency sourceFileDependency = new SourceFileDependency(sourceFile);
                sourceFileDependency.setCodeFragment(dependencyCodeFragment);
                DependencyUtils.addDependency(dependencies, sourceFileDependency, sourceAnchor, targetAnchor);
            }
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
