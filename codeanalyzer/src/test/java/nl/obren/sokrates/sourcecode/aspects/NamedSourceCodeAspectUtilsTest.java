/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.aspects;

import nl.obren.sokrates.sourcecode.SourceFile;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.file.FileSystems.getDefault;
import static junit.framework.TestCase.assertEquals;


public class NamedSourceCodeAspectUtilsTest {

    @Test
    public void getSourceCodeAspectBasedOnFolderDepth() {
        List<SourceFile> sourceFiles = Arrays.asList(
                new SourceFile(new File("/root/subfolder/a/folder1a/A.java"), "").relativize(new File("/root")),
                new SourceFile(new File("/root/subfolder/a/folder1b/B.java"), "").relativize(new File("/root")),
                new SourceFile(new File("/root/subfolder/b/C.java"), "").relativize(new File("/root")),
                new SourceFile(new File("/root/subfolder/b/D.java"), "").relativize(new File("/root"))
        );
        String srcRoot = "/root";
        assertEquals(SourceCodeAspectUtils.getSourceCodeAspectBasedOnFolderDepth(srcRoot, sourceFiles, 2, 0).size(), 2);
        assertEquals(SourceCodeAspectUtils.getSourceCodeAspectBasedOnFolderDepth(srcRoot, sourceFiles, 2, 0).get(0).getName(), "a");
        assertEquals(SourceCodeAspectUtils.getSourceCodeAspectBasedOnFolderDepth(srcRoot, sourceFiles, 2, 0).get(0).getSourceFileFilters().size(), 1);
        assertEquals(SourceCodeAspectUtils.getSourceCodeAspectBasedOnFolderDepth(srcRoot, sourceFiles, 2, 0).get(1).getName(), "b");
        assertEquals(SourceCodeAspectUtils.getSourceCodeAspectBasedOnFolderDepth(srcRoot, sourceFiles, 2, 0).get(1).getSourceFileFilters().size(), 1);
    }

    @Test
    public void getSourceCodeAspectBasedOnFolderDepthForRoot() {
        List<SourceFile> sourceFiles = Arrays.asList(
                new SourceFile(new File("/root/prefixABC/A.java"), "").relativize(new File("/root")),
                new SourceFile(new File("/root/prefixDEF/B.java"), "").relativize(new File("/root"))
        );
        String srcRoot = "/root";
        assertEquals(SourceCodeAspectUtils.getSourceCodeAspectBasedOnFolderDepth(srcRoot, sourceFiles, 2, 0).size(), 2);
        assertEquals(SourceCodeAspectUtils.getSourceCodeAspectBasedOnFolderDepth(srcRoot, sourceFiles, 2, 0).get(0).getName(), "prefixABC");
        assertEquals(SourceCodeAspectUtils.getSourceCodeAspectBasedOnFolderDepth(srcRoot, sourceFiles, 2, 0).get(0).getSourceFileFilters().size(), 1);
        assertEquals(SourceCodeAspectUtils.getSourceCodeAspectBasedOnFolderDepth(srcRoot, sourceFiles, 2, 0).get(1).getName(), "prefixDEF");
        assertEquals(SourceCodeAspectUtils.getSourceCodeAspectBasedOnFolderDepth(srcRoot, sourceFiles, 2, 0).get(1).getSourceFileFilters().size(), 1);
    }

    @Test
    public void getUniquePaths() {
        List<SourceFile> sourceFiles = Arrays.asList(
                new SourceFile(new File("/root/folder1/folder1a/A.java"), "").relativize(new File("/root")),
                new SourceFile(new File("/root/folder1/folder1b/B.java"), "").relativize(new File("/root")),
                new SourceFile(new File("/root/folder2/C.java"), "").relativize(new File("/root")),
                new SourceFile(new File("/root/folder2/D.java"), "").relativize(new File("/root"))
        );
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 2).size(), 3);
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 2).get(0), "folder1" + getDefault().getSeparator() + "folder1a");
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 2).get(1), "folder1" + getDefault().getSeparator() + "folder1b");
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 2).get(2), "folder2");
    }


    @Test
    public void getFolderBasedComponent() {
        SourceFile sourceFile = new SourceFile(new File("/a/b/c/d/e/J.java"));
        sourceFile.setRelativePath("b/c/d/e/J.java");
        assertEquals(SourceCodeAspectUtils.getFolderBasedComponentName(sourceFile, 1), "b");
        assertEquals(SourceCodeAspectUtils.getFolderBasedComponentName(sourceFile, 2), "b" + getDefault().getSeparator() + "c");
        assertEquals(SourceCodeAspectUtils.getFolderBasedComponentName(sourceFile, 3), "b" + getDefault().getSeparator() + "c" + getDefault().getSeparator() + "d");
        assertEquals(SourceCodeAspectUtils.getFolderBasedComponentName(sourceFile, 4), "b" + getDefault().getSeparator() + "c" + getDefault().getSeparator() + "d" + getDefault().getSeparator() + "e");
        assertEquals(SourceCodeAspectUtils.getFolderBasedComponentName(sourceFile, 5), "b" + getDefault().getSeparator() + "c" + getDefault().getSeparator() + "d" + getDefault().getSeparator() + "e");
        assertEquals(SourceCodeAspectUtils.getFolderBasedComponentName(sourceFile, 6), "b" + getDefault().getSeparator() + "c" + getDefault().getSeparator() + "d" + getDefault().getSeparator() + "e");
    }


    @Test
    public void getUniquePaths1() {
        List<SourceFile> sourceFiles = new ArrayList<>();
        SourceFile sourceFile1 = new SourceFile(new File("/root/a/b/c/d.java"), "");
        sourceFile1.setRelativePath("a/b/c/d.java");
        sourceFiles.add(sourceFile1);

        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 0).size(), 1);
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 0).get(0), "");
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 1).size(), 1);
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 1).get(0), "a");
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 2).size(), 1);
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 2).get(0), "a" + getDefault().getSeparator() + "b");
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 3).size(), 1);
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 3).get(0), "a" + getDefault().getSeparator() + "b" + getDefault().getSeparator() + "c");
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 4).size(), 1);
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 4).get(0), "a" + getDefault().getSeparator() + "b" + getDefault().getSeparator() + "c");
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 5).size(), 1);
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 5).get(0), "a" + getDefault().getSeparator() + "b" + getDefault().getSeparator() + "c");
    }

    @Test
    public void getUniquePaths2() {
        List<SourceFile> sourceFiles = new ArrayList<>();
        SourceFile sourceFile1a = new SourceFile(new File("/root/a/b/c/d1.java"), "");
        sourceFile1a.setRelativePath("a/b/c/d2.java");

        SourceFile sourceFile1b = new SourceFile(new File("/root/a/b/c/d2.java"), "");
        sourceFile1b.setRelativePath("a/b/c/d2.java");

        SourceFile sourceFile1c = new SourceFile(new File("/root/a/b/c/d3.java"), "");
        sourceFile1c.setRelativePath("a/b/c/d3.java");

        SourceFile sourceFile2a = new SourceFile(new File("/root/a/b/e/f/g1.java"), "");
        sourceFile2a.setRelativePath("a/b/e/f/g.java");

        SourceFile sourceFile2b = new SourceFile(new File("/root/a/b/e/f/g2.java"), "");
        sourceFile2b.setRelativePath("a/b/e/f/g.java");

        SourceFile sourceFile3 = new SourceFile(new File("/root/a/b/h/i/j.java"), "");
        sourceFile3.setRelativePath("a/b/h/i/j.java");

        sourceFiles.add(sourceFile1a);
        sourceFiles.add(sourceFile1b);
        sourceFiles.add(sourceFile1c);
        sourceFiles.add(sourceFile2a);
        sourceFiles.add(sourceFile2b);
        sourceFiles.add(sourceFile3);

        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 0).size(), 1);
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 0).get(0), "");
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 1).size(), 1);
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 1).get(0), "a");
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 2).size(), 1);
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 2).get(0), "a"+ getDefault().getSeparator() +"b");
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 3).size(), 3);
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 3).get(0), "a" + getDefault().getSeparator() + "b" + getDefault().getSeparator() + "c");
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 3).get(1), "a" + getDefault().getSeparator() + "b" + getDefault().getSeparator() + "e");
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 3).get(2), "a" + getDefault().getSeparator() + "b" + getDefault().getSeparator() + "h");
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 4).size(), 3);
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 4).get(0), "a" + getDefault().getSeparator() + "b" + getDefault().getSeparator() + "c");
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 4).get(1), "a" + getDefault().getSeparator() + "b" + getDefault().getSeparator() + "e" + getDefault().getSeparator() + "f");
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 4).get(2), "a" + getDefault().getSeparator() + "b"+ getDefault().getSeparator() +"h"+ getDefault().getSeparator() +"i");
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 5).size(), 3);
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 5).get(0), "a"+ getDefault().getSeparator() +"b" + getDefault().getSeparator() + "c");
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 5).get(1), "a" + getDefault().getSeparator() + "b" + getDefault().getSeparator() + "e" + getDefault().getSeparator() + "f");
        assertEquals(SourceCodeAspectUtils.getUniquePaths(sourceFiles, 5).get(2), "a" + getDefault().getSeparator() + "b" + getDefault().getSeparator() + "h" + getDefault().getSeparator() + "i");
    }

    @Test
    public void getMaxLinesOfCode() {
        NamedSourceCodeAspect aspect1 = new NamedSourceCodeAspect();
        SourceFile sourceFile1 = new SourceFile();
        sourceFile1.setLinesOfCode(100);
        aspect1.getSourceFiles().add(sourceFile1);

        NamedSourceCodeAspect aspect2 = new NamedSourceCodeAspect();
        SourceFile sourceFile2 = new SourceFile();
        sourceFile2.setLinesOfCode(400);
        aspect2.getSourceFiles().add(sourceFile2);

        NamedSourceCodeAspect aspect3 = new NamedSourceCodeAspect();
        SourceFile sourceFile3 = new SourceFile();
        sourceFile3.setLinesOfCode(500);
        aspect3.getSourceFiles().add(sourceFile3);

        NamedSourceCodeAspect aspect4 = new NamedSourceCodeAspect();
        SourceFile sourceFile4 = new SourceFile();
        sourceFile4.setLinesOfCode(200);
        aspect4.getSourceFiles().add(sourceFile4);

        Assert.assertEquals(SourceCodeAspectUtils.getMaxLinesOfCode(Arrays.asList(aspect1, aspect2, aspect3, aspect4)), 500);
    }

    @Test
    public void getMaxFileCount() {
        NamedSourceCodeAspect aspect1 = new NamedSourceCodeAspect();
        aspect1.getSourceFiles().add(new SourceFile());
        aspect1.getSourceFiles().add(new SourceFile());
        aspect1.getSourceFiles().add(new SourceFile());
        aspect1.getSourceFiles().add(new SourceFile());

        NamedSourceCodeAspect aspect2 = new NamedSourceCodeAspect();
        aspect2.getSourceFiles().add(new SourceFile());
        aspect2.getSourceFiles().add(new SourceFile());
        aspect2.getSourceFiles().add(new SourceFile());
        aspect2.getSourceFiles().add(new SourceFile());
        aspect2.getSourceFiles().add(new SourceFile());
        aspect2.getSourceFiles().add(new SourceFile());

        NamedSourceCodeAspect aspect3 = new NamedSourceCodeAspect();
        aspect3.getSourceFiles().add(new SourceFile());
        aspect3.getSourceFiles().add(new SourceFile());
        aspect3.getSourceFiles().add(new SourceFile());

        NamedSourceCodeAspect aspect4 = new NamedSourceCodeAspect();
        aspect4.getSourceFiles().add(new SourceFile());
        aspect4.getSourceFiles().add(new SourceFile());

        Assert.assertEquals(SourceCodeAspectUtils.getMaxFileCount(Arrays.asList(aspect1, aspect2, aspect3, aspect4)), 6);
    }

    @Test
    public void greatestCommonPrefix() {
        assertEquals(SourceCodeAspectUtils.greatestCommonPrefix(Arrays.asList("")), "");

        assertEquals(SourceCodeAspectUtils.greatestCommonPrefix(Arrays.asList("", "")), "");

        assertEquals(SourceCodeAspectUtils.greatestCommonPrefix(Arrays.asList("",
                "main/java/nl/obren/codeexplorer/")), "");


        assertEquals(SourceCodeAspectUtils.greatestCommonPrefix(Arrays.asList("main/java/nl/obren/codeexplorer/common",
                "main/java/nl/obren/codeexplorer/fx",
                "main/java/nl/obren/codeexplorer/io",
                "main/java/nl/obren/codeexplorer/sourcecode")), "main/java/nl/obren/codeexplorer/");

        assertEquals(SourceCodeAspectUtils.greatestCommonPrefix(Arrays.asList(
                "test/java/nl/obren/codeexplorer/codebrowser",
                "test/java/nl/obren/codeexplorer/common",
                "test/java/nl/obren/codeexplorer/io",
                "test/java/nl/obren/codeexplorer/sourcecode",
                "test/java/nl/obren/codeexplorer/temp")), "test/java/nl/obren/codeexplorer/");

        assertEquals(SourceCodeAspectUtils.greatestCommonPrefix(Arrays.asList("main/java/nl/obren/codeexplorer/common",
                "main/java/nl/obren/codeexplorer/fx",
                "main/java/nl/obren/codeexplorer/io",
                "main/java/nl/obren/codeexplorer/sourcecode",
                "test/java/nl/obren/codeexplorer/codebrowser",
                "test/java/nl/obren/codeexplorer/common",
                "test/java/nl/obren/codeexplorer/io",
                "test/java/nl/obren/codeexplorer/sourcecode",
                "test/java/nl/obren/codeexplorer/temp")), "");
    }

    @Test
    public void greatestCommonPrefixEnsureWholeFolderNames() {
        assertEquals(SourceCodeAspectUtils.greatestCommonPrefix(Arrays.asList(
                "main/java/nl/obren/codeexplorer/common",
                "main/java/nl/obren/codeexplorer/code",
                "main/java/nl/obren/codeexplorer/codesign")), "main/java/nl/obren/codeexplorer/");

        assertEquals(SourceCodeAspectUtils.greatestCommonPrefix(Arrays.asList(
                "common",
                "code",
                "codesign")), "");

    }
}
