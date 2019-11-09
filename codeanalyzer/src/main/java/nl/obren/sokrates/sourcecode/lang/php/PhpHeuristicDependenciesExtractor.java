package nl.obren.sokrates.sourcecode.lang.php;

import nl.obren.sokrates.common.utils.ExplorerStringUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.dependencies.HeuristicDependenciesExtractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PhpHeuristicDependenciesExtractor extends HeuristicDependenciesExtractor {
    public static final String PHP_IDENTIFIER_REGEX = "[a-zA-Z_\\x7f-\\xff][a-zA-Z0-9_\\x7f-\\xff]*";

    @Override
    public List<DependencyAnchor> extractDependencyAnchors(SourceFile sourceFile) {
        List<DependencyAnchor> anchors = new ArrayList<>();

        String content = sourceFile.getContent();
        content = content.replace("\t", " ");
        content = SourceCodeCleanerUtils.normalizeLineEnds(content);

        int startIndexOfNamespaceName = content.indexOf("namespace ");
        if (startIndexOfNamespaceName >= 0) {
            int endIndexOfNamespaceName = ExplorerStringUtils.firstIndexOfAny(Arrays.asList(";", " "), content, startIndexOfNamespaceName + "namespace ".length());
            if (endIndexOfNamespaceName >= 0) {
                String namespaceName = content.substring(startIndexOfNamespaceName + "namespace ".length(), endIndexOfNamespaceName).trim();
                DependencyAnchor dependencyAnchor = new DependencyAnchor(namespaceName);
                dependencyAnchor.setCodeFragment(content.substring(startIndexOfNamespaceName, endIndexOfNamespaceName + 1).trim());
                dependencyAnchor.getDependencyPatterns().add("use.*" + namespaceName.replace("\\", "\\\\") + "[ ]*;");
                dependencyAnchor.getDependencyPatterns().add("use.*" + namespaceName.replace("\\", "\\\\") + "\\\\" + PHP_IDENTIFIER_REGEX + "( |,|;|[ ]+as).*");
                dependencyAnchor.getSourceFiles().add(sourceFile);
                anchors.add(dependencyAnchor);
            }
        }


        return anchors;
    }

}
