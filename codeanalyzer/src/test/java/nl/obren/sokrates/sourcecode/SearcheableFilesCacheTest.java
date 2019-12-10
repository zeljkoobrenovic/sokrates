/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.search.SearchExpression;
import nl.obren.sokrates.sourcecode.search.SearchRequest;
import nl.obren.sokrates.sourcecode.search.SearchResult;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.*;

public class SearcheableFilesCacheTest {
    @Test
    public void getInstance() throws Exception {
        SearcheableFilesCache instance = SearcheableFilesCache.getInstance(getTestSourceFiles());
        assertEquals(instance.getSourceFiles().size(), 2);
    }

    private List<SourceFile> getTestSourceFiles() {
        String code1 = "public class ExampleMinNumber {\n" +
                "   \n" +
                "   public static void main(String[] args) {\n" +
                "      int a = 11;\n" +
                "      int b = 6;\n" +
                "      int c = minFunction(a, b);\n" +
                "      System.out.println(\"Minimum Value = \" + c);\n" +
                "   }\n" +
                "\n" +
                "   /** returns the minimum of two numbers */\n" +
                "   public static int minFunction(int n1, int n2) {\n" +
                "      int min;\n" +
                "      if (n1 > n2)\n" +
                "         min = n2;\n" +
                "      else\n" +
                "         min = n1;\n" +
                "\n" +
                "      return min; \n" +
                "   }\n" +
                "}";

        String code2 = "public class ExampleVoid {\n" +
                "\n" +
                "   public static void main(String[] args) {\n" +
                "      methodRankPoints(255.7);\n" +
                "   }\n" +
                "\n" +
                "   public static void methodRankPoints(double points) {\n" +
                "      if (points >= 202.5) {\n" +
                "         System.out.println(\"Rank:A1\");\n" +
                "      }else if (points >= 122.4) {\n" +
                "         System.out.println(\"Rank:A2\");\n" +
                "      }else {\n" +
                "         System.out.println(\"Rank:A3\");\n" +
                "      }\n" +
                "   }\n" +
                "}";

        SourceFile sourceFile1 = new SourceFile(new File("file1.java"), code1);
        sourceFile1.setLinesOfCode(code1.split("\n").length);
        SourceFile sourceFile2 = new SourceFile(new File("file2.java"), code2);
        sourceFile2.setLinesOfCode(code2.split("\n").length);
        return Arrays.asList(sourceFile1, sourceFile2);
    }

    @Test
    public void getTotalLinesCount() throws Exception {
        SearcheableFilesCache instance = SearcheableFilesCache.getInstance(getTestSourceFiles());
        assertEquals(instance.getTotalLinesCount(), 36);
    }

    @Test
    public void addSearcheableFile() throws Exception {
        List<SourceFile> sourceFiles = getTestSourceFiles();
        SearcheableFilesCache instance = new SearcheableFilesCache();

        sourceFiles.forEach(sourceFile -> instance.addSearcheableFile(sourceFile));

        assertEquals(instance.getSourceFiles().size(), 2);
        assertEquals(instance.getTotalLinesCount(), 36);
    }

    @Test
    public void search() throws Exception {
        SearcheableFilesCache instance = SearcheableFilesCache.getInstance(getTestSourceFiles());

        final int progressUpdateCount[] = {0};
        ProgressFeedback progressFeedback = new ProgressFeedback() {
            public void progress(int currentValue, int endValue) {
                progressUpdateCount[0]++;
            }
        };

        SearchResult search = instance.search(new SearchRequest(new SearchExpression(""), new SearchExpression("")), progressFeedback);
        assertEquals(search.getFoundFiles().size(), 2);
        assertEquals(progressUpdateCount[0], 3);

        progressUpdateCount[0] = 0;
        search = instance.search(new SearchRequest(new SearchExpression(".*1.*"), new SearchExpression("")), progressFeedback);
        assertEquals(search.getFoundFiles().size(), 1);
        assertEquals(search.getFoundFiles().get(new File("file1.java")).getSourceFile().getFile().getPath(), "file1.java");

        progressUpdateCount[0] = 0;
        search = instance.search(new SearchRequest(new SearchExpression(""), new SearchExpression(".*methodRankPoints.*")), progressFeedback);
        assertEquals(search.getFoundFiles().size(), 1);
        assertEquals(search.getFoundFiles().get(new File("file2.java")).getSourceFile().getFile().getPath(), "file2.java");
        assertEquals(search.getFoundFiles().get(new File("file2.java")).getFoundInstancesCount(), 2);
        assertEquals(search.getFoundFiles().get(new File("file2.java")).getLinesWithSearchedContent().size(), 2);
        assertEquals(search.getFoundFiles().get(new File("file2.java")).getLinesWithSearchedContent().get(0).getLine(), "      methodRankPoints(255.7);");
        assertEquals(search.getFoundFiles().get(new File("file2.java")).getLinesWithSearchedContent().get(0).getLineNumber(), 4);
        assertEquals(search.getFoundFiles().get(new File("file2.java")).getLinesWithSearchedContent().get(1).getLine(), "   public static void methodRankPoints(double points) {");
        assertEquals(search.getFoundFiles().get(new File("file2.java")).getLinesWithSearchedContent().get(1).getLineNumber(), 7);
    }

    @Test
    public void isFilterSet() throws Exception {
        SearcheableFilesCache searcheableFilesCache = new SearcheableFilesCache();

        assertFalse(searcheableFilesCache.isFilterSet(new SearchExpression("/root/a/b.*"), new File("/other/a")));

        assertTrue(searcheableFilesCache.isFilterSet(new SearchExpression("/root/a/b.*"), new File("/root/a/b/c")));
        assertTrue(searcheableFilesCache.isFilterSet(new SearchExpression("/root/a/b.*"), new File("\\root\\a\\b\\c")));
        assertTrue(searcheableFilesCache.isFilterSet(new SearchExpression("/root/a/b.*"), new File("/root\\a\\b/c")));

        assertFalse(searcheableFilesCache.isFilterSet(new SearchExpression("\\\\root\\\\a\\\\b.*"), new File("/other/a")));

        assertTrue(searcheableFilesCache.isFilterSet(new SearchExpression("\\\\root\\\\a\\\\b.*"), new File("/root/a/b/c")));
        assertTrue(searcheableFilesCache.isFilterSet(new SearchExpression("\\\\root\\\\a\\\\b.*"), new File("\\root\\a\\b\\c")));
        assertTrue(searcheableFilesCache.isFilterSet(new SearchExpression("\\\\root\\\\a\\\\b.*"), new File("/root\\a\\b/c")));

    }


}
