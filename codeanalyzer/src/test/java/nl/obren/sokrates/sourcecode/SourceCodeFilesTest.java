/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.nio.file.FileSystems.getDefault;
import static org.junit.Assert.*;

public class SourceCodeFilesTest {
    @Test
    public void getLinesOfCode() throws Exception {
        SourceFile sourceFile1 = new SourceFile();
        SourceFile sourceFile2 = new SourceFile();
        List<SourceFile> testSourceFiles = Arrays.asList(sourceFile1, sourceFile2);

        assertEquals(SourceCodeFiles.getLinesOfCode(testSourceFiles), 0);

        sourceFile1.setLinesOfCode(100);
        assertEquals(SourceCodeFiles.getLinesOfCode(testSourceFiles), 100);

        sourceFile2.setLinesOfCode(200);
        assertEquals(SourceCodeFiles.getLinesOfCode(testSourceFiles), 300);
    }

    @Test
    public void getSourceFiles() throws Exception {
        SourceCodeFiles sourceCodeFiles = new SourceCodeFiles();
        List<SourceFile> testSourceFiles = getTestSourceFiles();
        sourceCodeFiles.setFilesInBroadScope(testSourceFiles);

        NamedSourceCodeAspect aspect = new NamedSourceCodeAspect();
        aspect.getSourceFileFilters().add(new SourceFileFilter(".*(A|B|C)[.]java", ""));

        List<SourceFile> sourceFiles = sourceCodeFiles.getSourceFiles(aspect);

        assertEquals(sourceFiles.size(), 3);
        assertEquals(sourceFiles.get(0).getFile().getPath(), getDefault().getSeparator() + "testproject" +
                getDefault().getSeparator() + "src" + getDefault().getSeparator() + "main" + getDefault().getSeparator()
                + "java" + getDefault().getSeparator() + "package" + getDefault().getSeparator() + "A.java");
        assertEquals(sourceFiles.get(1).getFile().getPath(), getDefault().getSeparator() + "testproject" +
                getDefault().getSeparator() + "src" + getDefault().getSeparator() + "main" + getDefault().getSeparator()
                + "java" + getDefault().getSeparator() + "package" + getDefault().getSeparator() + "B.java");
        assertEquals(sourceFiles.get(2).getFile().getPath(), getDefault().getSeparator() + "testproject" +
                getDefault().getSeparator() + "src" + getDefault().getSeparator() + "main" + getDefault().getSeparator()
                + "java" + getDefault().getSeparator() + "package" + getDefault().getSeparator() + "C.java");
    }

    @Test
    public void getSourceFilesWithDuplicationInFilters() throws Exception {
        SourceCodeFiles sourceCodeFiles = new SourceCodeFiles();
        List<SourceFile> testSourceFiles = getTestSourceFiles();
        sourceCodeFiles.setFilesInBroadScope(testSourceFiles);

        NamedSourceCodeAspect aspect = new NamedSourceCodeAspect();
        aspect.getSourceFileFilters().add(new SourceFileFilter(".*(A|B|C)[.]java", ""));
        aspect.getSourceFileFilters().add(new SourceFileFilter(".*A[.]java", ""));
        aspect.getSourceFileFilters().add(new SourceFileFilter(".*B[.]java", ""));

        List<SourceFile> sourceFiles = sourceCodeFiles.getSourceFiles(aspect);

        assertEquals(sourceFiles.size(), 3);
        assertEquals(sourceFiles.get(0).getFile().getPath(),  getDefault().getSeparator() + "testproject" +
                getDefault().getSeparator() + "src" + getDefault().getSeparator() + "main" + getDefault().getSeparator()
                + "java" + getDefault().getSeparator() + "package" + getDefault().getSeparator() + "A.java");
        assertEquals(sourceFiles.get(1).getFile().getPath(),  getDefault().getSeparator() + "testproject" +
                getDefault().getSeparator() + "src" + getDefault().getSeparator() + "main" + getDefault().getSeparator()
                + "java" + getDefault().getSeparator() + "package" + getDefault().getSeparator() + "B.java");
        assertEquals(sourceFiles.get(2).getFile().getPath(),  getDefault().getSeparator() + "testproject" +
                getDefault().getSeparator() + "src" + getDefault().getSeparator() + "main" + getDefault().getSeparator()
                + "java" + getDefault().getSeparator() + "package" + getDefault().getSeparator() + "C.java");
    }

    @Test
    public void getSourceFilesWithExclusion() throws Exception {
        SourceCodeFiles sourceCodeFiles = new SourceCodeFiles();
        List<SourceFile> testSourceFiles = getTestSourceFiles();
        sourceCodeFiles.setFilesInBroadScope(testSourceFiles);

        NamedSourceCodeAspect aspect = new NamedSourceCodeAspect();
        aspect.getSourceFileFilters().add(new SourceFileFilter(".*(A|B|C)[.]java", ""));
        SourceFileFilter exclusiveFilter = new SourceFileFilter(".*A[.]java", "");
        exclusiveFilter.setException(true);
        aspect.getSourceFileFilters().add(exclusiveFilter);

        List<SourceFile> sourceFiles = sourceCodeFiles.getSourceFiles(aspect);

        assertEquals(sourceFiles.size(), 2);
        assertEquals(sourceFiles.get(0).getFile().getPath(),  getDefault().getSeparator() + "testproject" +
                getDefault().getSeparator() + "src" + getDefault().getSeparator() + "main" + getDefault().getSeparator()
                + "java" + getDefault().getSeparator() + "package" + getDefault().getSeparator() + "B.java");
        assertEquals(sourceFiles.get(1).getFile().getPath(),  getDefault().getSeparator() + "testproject" +
                getDefault().getSeparator() + "src" + getDefault().getSeparator() + "main" + getDefault().getSeparator()
                + "java" + getDefault().getSeparator() + "package" + getDefault().getSeparator() + "C.java");
    }

    @Test
    public void createBroadScope() throws Exception {
        SourceCodeFiles sourceCodeFiles = new SourceCodeFiles();
        List<SourceFile> testSourceFiles = getTestSourceFiles();
        sourceCodeFiles.setAllFiles(testSourceFiles);

        Map<String, Integer> extensionsCountMap = sourceCodeFiles.getExtensionsCountMap(testSourceFiles);

        assertEquals(extensionsCountMap.size(), 4);

        assertEquals(extensionsCountMap.get("java"), new Integer(4));
        assertEquals(extensionsCountMap.get("js"), new Integer(3));
        assertEquals(extensionsCountMap.get("html"), new Integer(2));
        assertEquals(extensionsCountMap.get("css"), new Integer(1));

        sourceCodeFiles.createBroadScope(Arrays.asList("java", "js"), new ArrayList<>(), 1000);

        assertEquals(sourceCodeFiles.getFilesInBroadScope().size(), 7);

        Map<String, Integer> broadScopeExtensionsCountMap = sourceCodeFiles.getExtensionsCountMap(sourceCodeFiles.getFilesInBroadScope());

        assertEquals(broadScopeExtensionsCountMap.size(), 2);

        assertEquals(broadScopeExtensionsCountMap.get("java"), new Integer(4));
        assertEquals(broadScopeExtensionsCountMap.get("js"), new Integer(3));

        SourceFileFilter filter = new SourceFileFilter(".*[.]js", "");

        sourceCodeFiles.createBroadScope(Arrays.asList("java", "js"), Arrays.asList(filter), 1000);
        assertEquals(sourceCodeFiles.getFilesInBroadScope().size(), 4);

        Map<String, Integer> broadScopeExtensionsCountMapFiltered = sourceCodeFiles.getExtensionsCountMap(sourceCodeFiles.getFilesInBroadScope());

        assertEquals(broadScopeExtensionsCountMapFiltered.size(), 1);

        assertEquals(broadScopeExtensionsCountMapFiltered.get("java"), new Integer(4));
        assertNull(broadScopeExtensionsCountMapFiltered.get("js"));
    }

    @Test
    public void shouldExclude() throws Exception {
        SourceCodeFiles sourceCodeFiles = new SourceCodeFiles();

        SourceFile sourceFile = new SourceFile(new File("/testproject/src/main/java/package/A.java"));

        SourceFileFilter filter1 = new SourceFileFilter(".*[.]java", "");
        SourceFileFilter filter2 = new SourceFileFilter(".*[.]js", "");

        assertTrue(sourceCodeFiles.shouldExcludeFile(sourceFile, Arrays.asList(filter1), 100));
        assertFalse(sourceCodeFiles.shouldExcludeFile(sourceFile, Arrays.asList(filter2), 100));
        assertTrue(sourceCodeFiles.shouldExcludeFile(sourceFile, Arrays.asList(filter1, filter2), 100));
    }

    @Test
    public void getExtensionsCountMap() throws Exception {
        SourceCodeFiles sourceCodeFiles = new SourceCodeFiles();

        List<SourceFile> testSourceFiles = getTestSourceFiles();
        sourceCodeFiles.setAllFiles(testSourceFiles);

        Map<String, Integer> extensionsCountMap = sourceCodeFiles.getExtensionsCountMap(testSourceFiles);

        assertEquals(extensionsCountMap.size(), 4);

        assertEquals(extensionsCountMap.get("java"), new Integer(4));
        assertEquals(extensionsCountMap.get("js"), new Integer(3));
        assertEquals(extensionsCountMap.get("html"), new Integer(2));
        assertEquals(extensionsCountMap.get("css"), new Integer(1));

        assertNull(extensionsCountMap.get("cs"));
        assertNull(extensionsCountMap.get("asp"));
    }

    private List<SourceFile> getTestSourceFiles() {
        List<SourceFile> sourceFiles = new ArrayList<>();

        sourceFiles.add(new SourceFile(new File("/testproject/src/main/java/package/A.java")));
        sourceFiles.add(new SourceFile(new File("/testproject/src/main/java/package/B.java")));
        sourceFiles.add(new SourceFile(new File("/testproject/src/main/java/package/C.java")));
        sourceFiles.add(new SourceFile(new File("/testproject/src/main/java/package/D.java")));
        sourceFiles.add(new SourceFile(new File("/testproject/src/main/resources/public/e.js")));
        sourceFiles.add(new SourceFile(new File("/testproject/src/main/resources/public/f.js")));
        sourceFiles.add(new SourceFile(new File("/testproject/src/main/resources/public/g.js")));
        sourceFiles.add(new SourceFile(new File("/testproject/src/main/resources/public/h.html")));
        sourceFiles.add(new SourceFile(new File("/testproject/src/main/resources/public/i.html")));
        sourceFiles.add(new SourceFile(new File("/testproject/src/main/resources/public/j.css")));

        return sourceFiles;
    }

    @Test
    public void isNotVCSFolder() throws Exception {
        assertTrue(new SourceCodeFiles().isNotVCSFolder(new File("/src/main")));
        assertTrue(new SourceCodeFiles().isNotVCSFolder(new File("/src/main/test")));
        assertFalse(new SourceCodeFiles().isNotVCSFolder(new File("/src/main/test/.svn")));
        assertFalse(new SourceCodeFiles().isNotVCSFolder(new File("/src/main/test/.git")));
    }

}
