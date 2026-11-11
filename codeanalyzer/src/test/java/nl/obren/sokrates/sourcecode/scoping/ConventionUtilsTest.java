/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.scoping;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConventionUtilsTest {

    private static SourceFile file(String path) {
        return new SourceFile(new File(path), "");
    }

    // A matching convention is added exactly once, even when many files match it.
    @Test
    public void deduplicatesAcrossManyMatchingFiles() {
        List<Convention> conventions = Arrays.asList(new Convention(".*[.]java", "", "Java"));
        List<SourceFileFilter> filters = new ArrayList<>();
        List<SourceFile> files = Arrays.asList(file("a/A.java"), file("a/B.java"), file("a/C.java"));

        ConventionUtils.addConventions(conventions, filters, files);

        assertEquals(1, filters.size());
        assertEquals(".*[.]java", filters.get(0).getPathPattern());
    }

    // Every matching convention is added (the per-convention loop does not stop at the first match).
    @Test
    public void addsAllMatchingConventions() {
        List<Convention> conventions = Arrays.asList(
                new Convention(".*[.]java", "", "Java"),
                new Convention(".*/a/.*", "", "In a/"),
                new Convention(".*[.]py", "", "Python")); // does not match
        List<SourceFileFilter> filters = new ArrayList<>();

        ConventionUtils.addConventions(conventions, filters, Arrays.asList(file("src/a/A.java")));

        assertEquals(2, filters.size());
        assertTrue(filters.stream().anyMatch(f -> f.getPathPattern().equals(".*[.]java")));
        assertTrue(filters.stream().anyMatch(f -> f.getPathPattern().equals(".*/a/.*")));
    }

    // A convention already present in the target filter list is not added again.
    @Test
    public void respectsPreExistingFilters() {
        List<Convention> conventions = Arrays.asList(new Convention(".*[.]java", "", "Java"));
        List<SourceFileFilter> filters = new ArrayList<>();
        filters.add(new SourceFileFilter(".*[.]java", ""));

        ConventionUtils.addConventions(conventions, filters, Arrays.asList(file("a/A.java")));

        assertEquals(1, filters.size());
    }

    // Distinct conventions that both match a file are kept separately (the dedup key distinguishes
    // them rather than collapsing them into one).
    @Test
    public void keepsDistinctMatchingConventionsSeparate() {
        List<Convention> conventions = Arrays.asList(
                new Convention(".*[.]java", "", "By extension"),
                new Convention(".*/a/.*", "", "By folder"));
        List<SourceFileFilter> filters = new ArrayList<>();

        ConventionUtils.addConventions(conventions, filters, Arrays.asList(file("src/a/A.java")));

        assertEquals(2, filters.size());
    }
}
