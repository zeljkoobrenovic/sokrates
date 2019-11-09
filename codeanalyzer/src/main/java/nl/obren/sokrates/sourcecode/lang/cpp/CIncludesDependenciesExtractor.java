package nl.obren.sokrates.sourcecode.lang.cpp;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.dependencies.HeuristicDependenciesExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CIncludesDependenciesExtractor extends HeuristicDependenciesExtractor {
    @Override
    public List<DependencyAnchor> extractDependencyAnchors(SourceFile sourceFile) {
        List<DependencyAnchor> anchors = new ArrayList<>();

        anchors.add(addIncludeDependencyAnchor(sourceFile, ""));

        File parent = sourceFile.getFile().getParentFile();

        String pathPrefix = "";
        while (parent != null) {
            pathPrefix = parent.getName() + "(/|\\\\)" + pathPrefix;
            anchors.add(addIncludeDependencyAnchor(sourceFile, pathPrefix));
            parent = parent.getParentFile();
        }

        return anchors;
    }

    private DependencyAnchor addIncludeDependencyAnchor(SourceFile sourceFile, String pathPrefix) {
        String fileName = sourceFile.getFile().getName();
        DependencyAnchor dependencyAnchor = new DependencyAnchor(fileName);
        dependencyAnchor.setCodeFragment(sourceFile.getRelativePath());

        dependencyAnchor.getDependencyPatterns().add(0, "[#]include.*\"" + pathPrefix + fileName + "\".*");
        dependencyAnchor.getDependencyPatterns().add(0, "[#]include.*<" + pathPrefix + fileName + ">.*");

        dependencyAnchor.getSourceFiles().add(sourceFile);

        return dependencyAnchor;
    }

}
