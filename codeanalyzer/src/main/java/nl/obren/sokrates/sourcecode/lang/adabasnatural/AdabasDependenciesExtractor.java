/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.adabasnatural;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.dependencies.HeuristicDependenciesExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AdabasDependenciesExtractor extends HeuristicDependenciesExtractor {
    @Override
    public List<DependencyAnchor> extractDependencyAnchors(SourceFile sourceFile) {
        List<DependencyAnchor> anchors = new ArrayList<>();

        anchors.add(addIncludeDependencyAnchor(sourceFile, ""));
        anchors.add(addUsingDependencyAnchor(sourceFile, ""));

        return anchors;
    }

    private DependencyAnchor addIncludeDependencyAnchor(SourceFile sourceFile, String pathPrefix) {
        String fileName = sourceFile.getFile().getName();
        DependencyAnchor dependencyAnchor = new DependencyAnchor(fileName);
        dependencyAnchor.setCodeFragment(sourceFile.getRelativePath());
        if (fileName.contains(".")){
            fileName = fileName.split("\\.")[0];
        }
        dependencyAnchor.getDependencyPatterns().add(0, "INCLUDE *" + fileName + "*");
        dependencyAnchor.getDependencyPatterns().add(0, "INCLUDE *\"" + fileName + "\"*");

        dependencyAnchor.getSourceFiles().add(sourceFile);

        return dependencyAnchor;
    }

    private DependencyAnchor addUsingDependencyAnchor(SourceFile sourceFile, String pathPrefix) {
        String fileName = sourceFile.getFile().getName();
        DependencyAnchor dependencyAnchor = new DependencyAnchor(fileName);
        dependencyAnchor.setCodeFragment(sourceFile.getRelativePath());
        if (fileName.contains(".")){
            fileName = fileName.split("\\.")[0];
        }
        dependencyAnchor.getDependencyPatterns().add(0, "USING *" + fileName + "*");

        dependencyAnchor.getSourceFiles().add(sourceFile);

        return dependencyAnchor;
    }


}
