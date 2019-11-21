package nl.obren.sokrates.sourcecode.lang.js;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class NodeJavaScriptHeuristicDependenciesExtractor extends HeuristicFilePathDependenciesExtractor {
    private static final Log LOG = LogFactory.getLog(NodeJavaScriptHeuristicDependenciesExtractor.class);

    private String moduleName;
    private File referencedFile;

    public void extractDependenciesToTargetAnchor(List<Dependency> dependencies, SourceFile sourceFile,
                                                  DependencyAnchor sourceAnchor, DependencyAnchor targetAnchor) {
        if (DependencyUtils.sourceAndTargetInSameComponent(sourceFile, targetAnchor)) {
            return;
        }
        String content = getContent(sourceFile);

        int startIndexOfModuleName = content.indexOf("require(");
        while (startIndexOfModuleName >= 0) {
            int endIndexOfModuleName = content.indexOf(")", startIndexOfModuleName + "require(".length());
            if (endIndexOfModuleName >= 0) {
                setModuleName(content, startIndexOfModuleName, endIndexOfModuleName);
                referencedFile = new File(sourceFile.getFile().getParentFile(), moduleName);
                addDependencyIfTargetReferenced(dependencies, sourceFile, sourceAnchor, targetAnchor);
                startIndexOfModuleName = content.indexOf("require(", endIndexOfModuleName + 1);
            } else {
                break;
            }
        }
    }

    private void setModuleName(String content, int startIndexOfModuleName, int endIndexOfModuleName) {
        moduleName = content.substring(startIndexOfModuleName + "require(".length(),
                endIndexOfModuleName).trim();
        moduleName = moduleName.replace("\"", "");
        moduleName = moduleName.replace("'", "");
        moduleName = StringUtils.appendIfMissing(moduleName, ".js");
    }

    private String getContent(SourceFile sourceFile) {
        String content = sourceFile.getContent();
        content = content.replace("\t", " ");
        content = SourceCodeCleanerUtils.normalizeLineEnds(content);
        return content;
    }

    private void addDependencyIfTargetReferenced(List<Dependency> dependencies, SourceFile sourceFile,
                                                 DependencyAnchor sourceAnchor, DependencyAnchor targetAnchor) {
        try {
            if (targetAnchor.getAnchor().equalsIgnoreCase(referencedFile.getCanonicalFile().getPath())) {
                Dependency dependency = new Dependency();
                dependency.setFrom(sourceAnchor);
                SourceFileDependency sourceFileDependency = new SourceFileDependency();
                sourceFileDependency.setSourceFile(sourceFile);
                sourceFileDependency.setCodeFragment("require('" + moduleName + "')");
                dependency.getFromFiles().add(sourceFileDependency);
                dependency.setTo(targetAnchor);
                dependencies.add(dependency);
            }
        } catch (IOException e) {
            LOG.error(e);
        }
    }

}
