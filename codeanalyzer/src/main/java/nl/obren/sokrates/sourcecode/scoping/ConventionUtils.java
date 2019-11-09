package nl.obren.sokrates.sourcecode.scoping;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;

import java.util.List;

public class ConventionUtils {
    public static void addConventions(List<Convention> conventions, List<SourceFileFilter> sourceFileFilters, List<SourceFile> sourceFiles) {
        sourceFiles.forEach(sourceFile -> {
            conventions.forEach(convention -> {
                SourceFileFilter filter = convention.getFilter();
                if (isNotAdded(filter, sourceFileFilters) && filter.matches(sourceFile)) {
                    sourceFileFilters.add(filter);
                }
            });
        });
    }

    private static boolean isNotAdded(SourceFileFilter filter, List<SourceFileFilter> sourceFileFilters) {
        for (SourceFileFilter sourceFileFilter : sourceFileFilters) {
            if (sourceFileFilter.getPathPattern().equals(filter.getPathPattern())
                    && sourceFileFilter.getContentPattern().equals(filter.getContentPattern())) {
                return false;
            }
        }
        return true;
    }
}
