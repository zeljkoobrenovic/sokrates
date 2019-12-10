/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.js;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.dependencies.HeuristicDependenciesExtractor;
import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.List;

public class JSHeuristicDependenciesExtractor extends HeuristicDependenciesExtractor {

    @Override
    public List<DependencyAnchor> extractDependencyAnchors(SourceFile sourceFile) {
        String fileName = FilenameUtils.getBaseName(sourceFile.getFile().getName());
        String parentName = sourceFile.getFile().getParentFile().getName();
        DependencyAnchor dependencyAnchor = new DependencyAnchor(parentName + "/" + fileName);
        dependencyAnchor.setCodeFragment(fileName);
        dependencyAnchor.getDependencyPatterns().add(".*/" + parentName + "/" + ".*");
        dependencyAnchor.getSourceFiles().add(sourceFile);

        List<DependencyAnchor> anchors = new ArrayList<>();
        anchors.add(dependencyAnchor);

        return anchors;
    }
}
