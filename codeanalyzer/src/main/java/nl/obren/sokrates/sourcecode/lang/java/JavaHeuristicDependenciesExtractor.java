/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.java;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.dependencies.HeuristicDependenciesExtractor;

import java.util.ArrayList;
import java.util.List;

public class JavaHeuristicDependenciesExtractor extends HeuristicDependenciesExtractor {
    public static final String PACKAGE_PREFIX = "package ";

    @Override
    public List<DependencyAnchor> extractDependencyAnchors(SourceFile sourceFile) {
        List<DependencyAnchor> anchors = new ArrayList<>();

        String content = sourceFile.getContent();
        content = content.replace("\t", " ");
        content = SourceCodeCleanerUtils.normalizeLineEnds(content);

        int startIndexOfPackageName = content.indexOf(PACKAGE_PREFIX);
        if (startIndexOfPackageName >= 0) {
            int endIndexOfPackageName = content.indexOf(";", startIndexOfPackageName + PACKAGE_PREFIX.length());
            if (endIndexOfPackageName >= 0) {
                String packageName = content.substring(startIndexOfPackageName + PACKAGE_PREFIX.length(), endIndexOfPackageName).trim();
                DependencyAnchor dependencyAnchor = new DependencyAnchor(packageName);
                dependencyAnchor.setCodeFragment(content.substring(startIndexOfPackageName, endIndexOfPackageName + 1).trim());
                dependencyAnchor.getDependencyPatterns().add("import.* " + packageName.replace(".", "[.]") + "([.][A-Z].*|[.][*]|);");
                dependencyAnchor.getSourceFiles().add(sourceFile);
                anchors.add(dependencyAnchor);
            }
        }

        return anchors;
    }
}
