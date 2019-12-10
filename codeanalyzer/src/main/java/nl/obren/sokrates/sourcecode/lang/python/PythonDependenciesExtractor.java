/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.python;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.dependencies.HeuristicDependenciesExtractor;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PythonDependenciesExtractor extends HeuristicDependenciesExtractor {
    @Override
    public List<DependencyAnchor> extractDependencyAnchors(SourceFile sourceFile) {
        List<DependencyAnchor> anchors = new ArrayList<>();

        File parent = sourceFile.getFile().getParentFile();

        String pathPrefix = "";

        DependencyAnchor dependencyAnchor = getImportDependencyAnchor(sourceFile, pathPrefix);
        anchors.add(dependencyAnchor);

        while (parent != null) {
            pathPrefix = parent.getName() + "[.]" + pathPrefix;
            anchors.add(getImportDependencyAnchor(sourceFile, pathPrefix));
            parent = parent.getParentFile();
        }


        return anchors;
    }

    private DependencyAnchor getImportDependencyAnchor(SourceFile sourceFile, String pathPrefix) {
        String moduleName = FilenameUtils.getBaseName(sourceFile.getFile().getName());
        DependencyAnchor dependencyAnchor = new DependencyAnchor(moduleName);
        dependencyAnchor.setCodeFragment(sourceFile.getRelativePath());
        dependencyAnchor.getDependencyPatterns().add("import.*" + pathPrefix + moduleName + ".*");
        dependencyAnchor.getDependencyPatterns().add("from.*" + pathPrefix + moduleName + ".*import.*");
        dependencyAnchor.getSourceFiles().add(sourceFile);
        return dependencyAnchor;
    }



}
