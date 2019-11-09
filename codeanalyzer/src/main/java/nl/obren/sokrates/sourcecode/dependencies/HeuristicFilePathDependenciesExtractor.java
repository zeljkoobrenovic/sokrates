package nl.obren.sokrates.sourcecode.dependencies;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class HeuristicFilePathDependenciesExtractor {
    private static final Log LOG = LogFactory.getLog(HeuristicFilePathDependenciesExtractor.class);

    public DependenciesAnalysis extractDependencies(List<SourceFile> files, ProgressFeedback progressFeedback) {
        DependenciesAnalysis dependenciesAnalysis = new DependenciesAnalysis();
        List<Dependency> dependencies = new ArrayList<>();
        dependenciesAnalysis.setDependencies(dependencies);
        List<DependencyAnchor> anchors = getDependencyAnchors(files);

        DependencyUtils.findErrors(anchors, dependenciesAnalysis.getErrors());

        int i = 0;
        for (DependencyAnchor sourceAnchor : anchors) {
            if (progressFeedback.canceled()) {
                break;
            }
            progressFeedback.setText(sourceAnchor.getAnchor());
            for (DependencyAnchor targetAnchor : anchors) {
                if (sourceAnchor != targetAnchor) {
                    sourceAnchor.getSourceFiles().forEach(sourceFile -> {
                        extractDependenciesToTargetAnchor(dependencies, sourceFile, sourceAnchor, targetAnchor);
                    });
                }
            }
            progressFeedback.progress(i++, anchors.size());
        }

        return dependenciesAnalysis;
    }

    protected abstract void extractDependenciesToTargetAnchor(List<Dependency> dependencies, SourceFile sourceFile,
                                                   DependencyAnchor sourceAnchor, DependencyAnchor targetAnchor);

    public List<DependencyAnchor> getDependencyAnchors(List<SourceFile> files) {
        List<DependencyAnchor> anchors = new ArrayList<>();

        for (SourceFile sourceFile : files) {
            try {
                DependencyAnchor dependencyAnchor = new DependencyAnchor();
                dependencyAnchor.setAnchor(sourceFile.getFile().getCanonicalFile().getPath());
                dependencyAnchor.getSourceFiles().add(sourceFile);
                anchors.add(dependencyAnchor);
            } catch (IOException e) {
                LOG.error(e);
            }
        }

        return anchors;
    }


}
