/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.go;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.dependencies.HeuristicDependenciesExtractor;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GoLangHeuristicDependenciesExtractor extends HeuristicDependenciesExtractor {

    @Override
    public List<DependencyAnchor> extractDependencyAnchors(SourceFile sourceFile) {
        List<DependencyAnchor> anchors = new ArrayList<>();

        String content = getContent(sourceFile);

        int startIndexOfPackageName = content.indexOf("package ");
        if (startIndexOfPackageName >= 0) {
            int endIndexOfPackageName = content.indexOf("\n", startIndexOfPackageName + "package ".length());
            if (endIndexOfPackageName >= 0) {
                DependencyAnchor dependencyAnchor = getDependencyAnchor(content, startIndexOfPackageName,
                        endIndexOfPackageName);

                addPatternsForDependencyAnchors(sourceFile, anchors, dependencyAnchor);
            }
        }


        return anchors;
    }

    private void addPatternsForDependencyAnchors(SourceFile sourceFile, List<DependencyAnchor> anchors, DependencyAnchor
            dependencyAnchor) {
        String importPattern = getImportPattern(sourceFile);
        if (!importPattern.isEmpty()) {
            dependencyAnchor.getDependencyPatterns().add("[ ]*\".*/" +
                    importPattern + "\"[ ]*");
            dependencyAnchor.getSourceFiles().add(sourceFile);
            anchors.add(dependencyAnchor);
        }
    }

    private String getContent(SourceFile sourceFile) {
        String content = sourceFile.getContent();
        content = content.replace("\t", " ");
        content = SourceCodeCleanerUtils.normalizeLineEnds(content);
        return content;
    }

    private DependencyAnchor getDependencyAnchor(String content, int startIndexOfPackageName, int endIndexOfPackageName) {
        String packageName = content.substring(startIndexOfPackageName + "package ".length(),
                endIndexOfPackageName).trim();
        DependencyAnchor dependencyAnchor = new DependencyAnchor(packageName);
        dependencyAnchor.setCodeFragment(content.substring(startIndexOfPackageName, endIndexOfPackageName +
                1).trim());
        return dependencyAnchor;
    }

    private String getImportPattern(SourceFile sourceFile) {
        String parent = StringUtils.defaultString(new File(sourceFile.getRelativePath()).getParent());
        String importPattern = StringUtils.appendIfMissing(parent.replace("\\", "/"), "/");
        importPattern = StringUtils.removeEnd(importPattern, "/");
        return importPattern;
    }
}
