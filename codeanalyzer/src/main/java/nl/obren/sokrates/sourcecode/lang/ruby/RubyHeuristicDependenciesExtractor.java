/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.ruby;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.dependencies.HeuristicDependenciesExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RubyHeuristicDependenciesExtractor extends HeuristicDependenciesExtractor {

    public static final String MODULE_PREFIX = "module ";

    @Override
    public List<DependencyAnchor> extractDependencyAnchors(SourceFile sourceFile) {
        List<DependencyAnchor> anchors = new ArrayList<>();

        String content = sourceFile.getContent();
        content = content.replace("\t", " ");
        content = SourceCodeCleanerUtils.normalizeLineEnds(content);

        extractModuleDependencyAnchors(sourceFile, anchors, content);

        anchors.add(addRequireDependencyAnchor(sourceFile, ""));

        File parent = sourceFile.getFile().getParentFile();
        String pathPrefix = "";
        while (parent != null) {
            pathPrefix = parent.getName() + "(/|\\\\)" + pathPrefix;
            anchors.add(addRequireDependencyAnchor(sourceFile, pathPrefix));
            parent = parent.getParentFile();
        }

        return anchors;
    }

    private void extractModuleDependencyAnchors(SourceFile sourceFile, List<DependencyAnchor> anchors, String content) {
        int startIndexOfPackageName = content.indexOf(MODULE_PREFIX);
        if (startIndexOfPackageName >= 0) {
            int endIndexOfPackageName = content.indexOf(";", startIndexOfPackageName + MODULE_PREFIX.length());
            if (endIndexOfPackageName >= 0) {
                String packageName = content.substring(startIndexOfPackageName + MODULE_PREFIX.length(), endIndexOfPackageName).trim();
                DependencyAnchor dependencyAnchor = new DependencyAnchor(packageName);
                dependencyAnchor.setCodeFragment(content.substring(startIndexOfPackageName, endIndexOfPackageName + 1).trim());
                dependencyAnchor.getDependencyPatterns().add("include( |\t)+" + packageName + "( |\t)*\n");
                dependencyAnchor.getSourceFiles().add(sourceFile);
                anchors.add(dependencyAnchor);
            }
        }
    }

    private DependencyAnchor addRequireDependencyAnchor(SourceFile sourceFile, String pathPrefix) {
        String fileName = sourceFile.getFile().getName();
        DependencyAnchor dependencyAnchor = new DependencyAnchor(fileName);
        dependencyAnchor.setCodeFragment(sourceFile.getRelativePath());

        dependencyAnchor.getDependencyPatterns().add(0, "[ ]*require (\"|')" + pathPrefix + fileName + "(\"|')");
        dependencyAnchor.getDependencyPatterns().add(0, "[ ]*require_relative (\"|')" + pathPrefix + fileName + "(\"|')");

        dependencyAnchor.getSourceFiles().add(sourceFile);

        return dependencyAnchor;
    }

}
