/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.scoping;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Comparator;
import java.util.List;

public class ConventionUtils {
    private static final Log LOG = LogFactory.getLog(ConventionUtils.class);

    public static void addConventions(List<Convention> conventions, List<SourceFileFilter> sourceFileFilters, List<SourceFile> sourceFiles) {
        int index[] = {0};
        sourceFiles.forEach(sourceFile -> {
            index[0] += 1;
            conventions.stream().sorted(Comparator.comparingInt(a -> a.getContentPattern().length())).forEach(convention -> {
                String prefix = index[0] + " / " + sourceFiles.size() + ": ";
                if (isNotAdded(convention, sourceFileFilters) && convention.matches(sourceFile)) {
                    sourceFileFilters.add(convention);
                    LOG.info(prefix + "  - path like \"" + convention.getPathPattern() + "\" / content like \"" + convention.getContentPattern() + "\"");
                    return;
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
