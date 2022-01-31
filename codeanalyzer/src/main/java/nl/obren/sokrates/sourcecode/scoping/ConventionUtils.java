/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.scoping;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

public class ConventionUtils {
    private static final Log LOG = LogFactory.getLog(ConventionUtils.class);

    public static void addConventions(List<Convention> conventions, List<SourceFileFilter> sourceFileFilters, List<SourceFile> sourceFiles) {
        sourceFiles.forEach(sourceFile -> {
            conventions.forEach(convention -> {
                if (isNotAdded(convention, sourceFileFilters) && convention.matches(sourceFile)) {
                    sourceFileFilters.add(convention);
                    LOG.info("  - path like \"" + convention.getPathPattern() + "\" / content like \"" + convention.getContentPattern() + "\"");
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
