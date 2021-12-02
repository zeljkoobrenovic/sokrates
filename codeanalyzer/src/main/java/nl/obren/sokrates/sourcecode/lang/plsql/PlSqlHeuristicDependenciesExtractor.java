package nl.obren.sokrates.sourcecode.lang.plsql;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.dependencies.HeuristicDependenciesExtractor;

import java.util.List;

public class PlSqlHeuristicDependenciesExtractor extends HeuristicDependenciesExtractor {

    @Override
    public List<DependencyAnchor> extractDependencyAnchors(SourceFile sourceFile) {
        return null;
    }
}
