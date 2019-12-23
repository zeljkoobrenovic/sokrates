/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.perl;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PerlHeuristicDependenciesExtractorTest {
    @Test
    public void extractDependencyAnchors() throws Exception {
        PerlHeuristicDependenciesExtractor parser = new PerlHeuristicDependenciesExtractor();
        ArrayList<SourceFile> sourceFiles = new ArrayList<>();

        sourceFiles.add(new SourceFile(new File("file1"), "package a.b.c; {}"));
        sourceFiles.add(new SourceFile(new File("file2"), "package a.b.c; {}"));
        sourceFiles.add(new SourceFile(new File("file3"), "package a.b.c; {}"));
        sourceFiles.add(new SourceFile(new File("file4"), "package d.e.f; {}"));
        sourceFiles.add(new SourceFile(new File("file5"), "package g.h.i; {}"));

        List<DependencyAnchor> dependencyAnchors = parser.getDependencyAnchors(sourceFiles);

        assertEquals(dependencyAnchors.size(), 3);

        assertEquals(dependencyAnchors.get(0).getAnchor(), "a.b.c");
        assertEquals(dependencyAnchors.get(0).getDependencyPatterns().get(0), "use[ ]+a.b.c.*");
        assertEquals(dependencyAnchors.get(0).getSourceFiles().size(), 3);
        assertEquals(dependencyAnchors.get(0).getSourceFiles().get(0).getFile().getPath(), "file1");
        assertEquals(dependencyAnchors.get(0).getSourceFiles().get(1).getFile().getPath(), "file2");
        assertEquals(dependencyAnchors.get(0).getSourceFiles().get(2).getFile().getPath(), "file3");

        assertEquals(dependencyAnchors.get(1).getAnchor(), "d.e.f");
        assertEquals(dependencyAnchors.get(1).getDependencyPatterns().get(0), "use[ ]+d.e.f.*");
        assertEquals(dependencyAnchors.get(1).getSourceFiles().size(), 1);
        assertEquals(dependencyAnchors.get(1).getSourceFiles().get(0).getFile().getPath(), "file4");

        assertEquals(dependencyAnchors.get(2).getAnchor(), "g.h.i");
        assertEquals(dependencyAnchors.get(2).getDependencyPatterns().get(0), "use[ ]+g.h.i.*");
        assertEquals(dependencyAnchors.get(2).getSourceFiles().size(), 1);
        assertEquals(dependencyAnchors.get(2).getSourceFiles().get(0).getFile().getPath(), "file5");

    }

}
