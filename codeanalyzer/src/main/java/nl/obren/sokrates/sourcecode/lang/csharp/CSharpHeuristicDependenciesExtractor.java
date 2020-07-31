/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.csharp;

import nl.obren.sokrates.common.utils.ExplorerStringUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.dependencies.HeuristicDependenciesExtractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CSharpHeuristicDependenciesExtractor extends HeuristicDependenciesExtractor {

    public static final String NAMESPACE_PREFIX = "namespace ";

    @Override
    public List<DependencyAnchor> extractDependencyAnchors(SourceFile sourceFile) {
        List<DependencyAnchor> anchors = new ArrayList<>();

        String content = sourceFile.getContent();
        content = content.replace("\t", " ");
        content = SourceCodeCleanerUtils.normalizeLineEnds(content);

        int startIndexOfNamespaceName = content.indexOf(NAMESPACE_PREFIX);
        if (startIndexOfNamespaceName >= 0) {
            int endIndexOfNamespaceName = ExplorerStringUtils.firstIndexOfAny(Arrays.asList("{", ";", " ", "\n"), content, startIndexOfNamespaceName + "namespace ".length());
            if (endIndexOfNamespaceName >= 0) {
                String namespaceName = content.substring(startIndexOfNamespaceName + NAMESPACE_PREFIX.length(), endIndexOfNamespaceName).trim();
                DependencyAnchor dependencyAnchor = new DependencyAnchor(namespaceName);
                dependencyAnchor.setCodeFragment(content.substring(startIndexOfNamespaceName, endIndexOfNamespaceName + 1).trim());
                dependencyAnchor.getDependencyPatterns().add("[ ]*using[ ]+" + namespaceName.replace(".", "[.]") + "([.][*]|);");
                dependencyAnchor.getSourceFiles().add(sourceFile);
                anchors.add(dependencyAnchor);
            }
        }


        return anchors;
    }
}
