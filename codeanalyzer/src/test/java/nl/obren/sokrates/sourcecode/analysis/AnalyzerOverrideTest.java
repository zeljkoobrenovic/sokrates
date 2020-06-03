/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class AnalyzerOverrideTest {
    @Test
    public void testIsOverridden() throws Exception {
        AnalyzerOverride override = new AnalyzerOverride();
        override.setAnalyzer("newAnalyzer");

        SourceFileFilter includeFilter = new SourceFileFilter("/a/b/c/.*", "");
        SourceFileFilter excludeFilter = new SourceFileFilter("/a/b/c/d/.*", "");
        excludeFilter.setException(true);

        override.getFilters().add(includeFilter);
        override.getFilters().add(excludeFilter);

        assertTrue(override.isOverridden(new SourceFile(new File("/a/b/c/file.ext"))));
        assertTrue(override.isOverridden(new SourceFile(new File("/a/b/c/e/file.ext"))));

        assertFalse(override.isOverridden(new SourceFile(new File("/a/b/c/d/file.ext"))));
        assertFalse(override.isOverridden(new SourceFile(new File("/something/else/file.ext"))));
    }
}
