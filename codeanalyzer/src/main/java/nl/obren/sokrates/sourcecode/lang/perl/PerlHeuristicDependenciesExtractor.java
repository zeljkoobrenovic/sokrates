/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.perl;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.dependencies.HeuristicDependenciesExtractor;

import java.util.ArrayList;
import java.util.List;

public class PerlHeuristicDependenciesExtractor extends HeuristicDependenciesExtractor {

    @Override
    public List<DependencyAnchor> extractDependencyAnchors(SourceFile sourceFile) {
        List<DependencyAnchor> anchors = new ArrayList<>();

        String content = sourceFile.getContent();
        content = content.replace("\t", " ");
        content = SourceCodeCleanerUtils.normalizeLineEnds(content);

        int startIndexOfPackageName = content.indexOf("package ");
        if (startIndexOfPackageName >= 0) {
            int endIndexOfPackageName = content.indexOf(";", startIndexOfPackageName + "package ".length());
            if (endIndexOfPackageName >= 0) {
                String packageName = content.substring(startIndexOfPackageName + "package ".length(), endIndexOfPackageName).trim();
                DependencyAnchor dependencyAnchor = new DependencyAnchor(packageName);
                dependencyAnchor.setCodeFragment(content.substring(startIndexOfPackageName, endIndexOfPackageName + 1).trim());

                String[] elements = packageName.split("::");

                String useName = "";
                for (String element : elements) {
                    if (!useName.isEmpty()) {
                        useName += "::";
                    }
                    useName += element;
                    dependencyAnchor.getDependencyPatterns().add("use[ ]+" + useName + ".*");
                }

                dependencyAnchor.getSourceFiles().add(sourceFile);
                anchors.add(dependencyAnchor);
            }
        }


        return anchors;
    }
}
