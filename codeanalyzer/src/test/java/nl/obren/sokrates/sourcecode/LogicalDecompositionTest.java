/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static java.nio.file.FileSystems.getDefault;
import static junit.framework.TestCase.assertEquals;

public class LogicalDecompositionTest {
    private File root = new File("/root/system/");
    private String separator = "/";

    private List<SourceFile> getTestSourceFiles() {
        return Arrays.asList(
                new SourceFile(new File("/root/system/a/a1/file1.java")).relativize(root),
                new SourceFile(new File("/root/system/a/a2/file2.java")).relativize(root),
                new SourceFile(new File("/root/system/b/b1/file3.java")).relativize(root),
                new SourceFile(new File("/root/system/b/b2/file4.java")).relativize(root)
        );
    }

    @Test
    public void updateLogicalComponentsFilesBasedOnFolderDepth() throws Exception {
        List<SourceFile> allFiles = getTestSourceFiles();

        LogicalDecomposition logicalDecomposition = new LogicalDecomposition("test");

        CodeConfiguration codeConfiguration = new CodeConfiguration();
        codeConfiguration.setSrcRoot(root.getPath());

        SourceCodeFiles sourceCodeFiles = new SourceCodeFiles();
        sourceCodeFiles.setAllFiles(allFiles);

        codeConfiguration.getMain().setSourceFiles(allFiles);

        logicalDecomposition.updateLogicalComponentsFiles(sourceCodeFiles, codeConfiguration, new File(""));

        assertEquals(logicalDecomposition.getComponents().size(), 2);
        assertEquals(logicalDecomposition.getComponents().get(0).getName(), "a");
        assertEquals(logicalDecomposition.getComponents().get(0).getSourceFiles().size(), 2);
        assertEquals(logicalDecomposition.getComponents().get(0).getSourceFiles().get(0).getRelativePath(), "a"  + separator + "a1"  + separator + "file1.java");
        assertEquals(logicalDecomposition.getComponents().get(0).getSourceFiles().get(1).getRelativePath(), "a"  + separator + "a2" + separator + "file2.java");
        assertEquals(logicalDecomposition.getComponents().get(1).getName(), "b");
        assertEquals(logicalDecomposition.getComponents().get(1).getSourceFiles().size(), 2);
        assertEquals(logicalDecomposition.getComponents().get(1).getSourceFiles().get(0).getRelativePath(), "b"  + separator + "b1"  + separator + "file3.java");
        assertEquals(logicalDecomposition.getComponents().get(1).getSourceFiles().get(1).getRelativePath(), "b"  + separator + "b2"  + separator + "file4.java");
    }

    @Test
    public void updateLogicalComponentsFilesBasedOnFolderDepthWithFilter() throws Exception {
        List<SourceFile> allFiles = getTestSourceFiles();

        CodeConfiguration codeConfiguration = new CodeConfiguration();
        codeConfiguration.setSrcRoot(root.getPath());

        SourceCodeFiles sourceCodeFiles = new SourceCodeFiles();
        sourceCodeFiles.setAllFiles(allFiles);

        codeConfiguration.getMain().setSourceFiles(allFiles);

        LogicalDecomposition logicalDecomposition = new LogicalDecomposition("test");
        logicalDecomposition.getFilters().add(new SourceFileFilter(".*/a.*?/.*", ""));
        logicalDecomposition.updateLogicalComponentsFiles(sourceCodeFiles, codeConfiguration, new File(""));

        assertEquals(logicalDecomposition.getComponents().size(), 2);
        assertEquals(logicalDecomposition.getComponents().get(0).getName(), "a");
        assertEquals(logicalDecomposition.getComponents().get(0).getSourceFiles().size(), 2);
        assertEquals(logicalDecomposition.getComponents().get(0).getSourceFiles().get(0).getRelativePath(), "a" + separator + "a1"  + separator + "file1.java");
        assertEquals(logicalDecomposition.getComponents().get(0).getSourceFiles().get(1).getRelativePath(), "a"  + separator + "a2" + separator + "file2.java");
        assertEquals(logicalDecomposition.getComponents().get(1).getName(), "Unclassified");
        assertEquals(logicalDecomposition.getComponents().get(1).getSourceFiles().size(), 2);
        assertEquals(logicalDecomposition.getComponents().get(1).getSourceFiles().get(0).getRelativePath(), "b"  + separator + "b1" + separator + "file3.java");
        assertEquals(logicalDecomposition.getComponents().get(1).getSourceFiles().get(1).getRelativePath(), "b"  + separator + "b2"  + separator + "file4.java");
    }

    @Test
    public void updateLogicalComponentsFilesBasedOnFolderDepthWithFilterWithoutRemainder() throws Exception {
        List<SourceFile> allFiles = getTestSourceFiles();

        CodeConfiguration codeConfiguration = new CodeConfiguration();
        codeConfiguration.setSrcRoot(root.getPath());

        SourceCodeFiles sourceCodeFiles = new SourceCodeFiles();
        sourceCodeFiles.setAllFiles(allFiles);

        codeConfiguration.getMain().setSourceFiles(allFiles);

        LogicalDecomposition logicalDecomposition = new LogicalDecomposition("test");
        logicalDecomposition.setIncludeRemainingFiles(false);
        logicalDecomposition.getFilters().add(new SourceFileFilter(".*/a.*?/.*", ""));
        logicalDecomposition.updateLogicalComponentsFiles(sourceCodeFiles, codeConfiguration, new File(""));

        assertEquals(logicalDecomposition.getComponents().size(), 1);
        assertEquals(logicalDecomposition.getComponents().get(0).getName(), "a");
        assertEquals(logicalDecomposition.getComponents().get(0).getSourceFiles().size(), 2);
        assertEquals(logicalDecomposition.getComponents().get(0).getSourceFiles().get(0).getRelativePath(), "a" + separator + "a1" + separator + "file1.java");
        assertEquals(logicalDecomposition.getComponents().get(0).getSourceFiles().get(1).getRelativePath(), "a" + separator + "a2" + separator + "file2.java");
    }

    @Test
    public void updateLogicalComponentsFilesBasedOnExplicitDefinition() throws Exception {
        List<SourceFile> allFiles = getTestSourceFiles();

        LogicalDecomposition logicalDecomposition = new LogicalDecomposition("test");
        logicalDecomposition.setComponentsFolderDepth(0);
        NamedSourceCodeAspect a = new NamedSourceCodeAspect("A");
        a.getSourceFileFilters().add(new SourceFileFilter(".*/a/.*", ""));
        logicalDecomposition.getComponents().add(a);
        NamedSourceCodeAspect b = new NamedSourceCodeAspect("B");
        b.getSourceFileFilters().add(new SourceFileFilter(".*/b/.*", ""));
        logicalDecomposition.getComponents().add(b);

        CodeConfiguration codeConfiguration = new CodeConfiguration();
        codeConfiguration.setSrcRoot(root.getPath());

        SourceCodeFiles sourceCodeFiles = new SourceCodeFiles();
        sourceCodeFiles.setAllFiles(allFiles);

        codeConfiguration.getMain().setSourceFiles(allFiles);

        logicalDecomposition.updateLogicalComponentsFiles(sourceCodeFiles, codeConfiguration, new File(""));

        assertEquals(logicalDecomposition.getComponents().size(), 2);
        assertEquals(logicalDecomposition.getComponents().get(0).getName(), "A");
        assertEquals(logicalDecomposition.getComponents().get(0).getSourceFiles().size(), 2);
        assertEquals(logicalDecomposition.getComponents().get(0).getSourceFiles().get(0).getRelativePath(), "a" + separator + "a1" + separator + "file1.java");
        assertEquals(logicalDecomposition.getComponents().get(0).getSourceFiles().get(1).getRelativePath(), "a" + separator + "a2" + separator + "file2.java");
        assertEquals(logicalDecomposition.getComponents().get(1).getName(), "B");
        assertEquals(logicalDecomposition.getComponents().get(1).getSourceFiles().size(), 2);
        assertEquals(logicalDecomposition.getComponents().get(1).getSourceFiles().get(0).getRelativePath(), "b"+ separator + "b1" + separator + "file3.java");
        assertEquals(logicalDecomposition.getComponents().get(1).getSourceFiles().get(1).getRelativePath(), "b" + separator + "b2" + separator + "file4.java");
    }
}
