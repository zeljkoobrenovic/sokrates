/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.duplication.DuplicatedFileBlock;
import nl.obren.sokrates.sourcecode.duplication.DuplicationInstance;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicationReportGeneratorTest {

    private DuplicationInstance duplicate(int blockSize, String... paths) {
        DuplicationInstance instance = new DuplicationInstance();
        instance.setBlockSize(blockSize);
        for (String path : paths) {
            DuplicatedFileBlock block = new DuplicatedFileBlock();
            SourceFile sourceFile = new SourceFile(new File(path), "");
            sourceFile.setRelativePath(path);
            block.setSourceFile(sourceFile);
            block.setStartLine(1);
            block.setEndLine(blockSize);
            block.setCleanedStartLine(1);
            block.setCleanedEndLine(blockSize);
            block.setSourceFileCleanedLinesOfCode(100);
            instance.getDuplicatedFileBlocks().add(block);
        }
        return instance;
    }

    private CodeAnalysisResults results() {
        CodeAnalysisResults results = new CodeAnalysisResults();
        results.setCodeConfiguration(new CodeConfiguration());
        return results;
    }

    @Test
    void filesCellEscapesNamesAndEncodesViewerLinks() {
        DuplicationInstance instance = duplicate(8, "src/<img src=x onerror=XSS>.ts", "src/odd #&% dir/plus+file.ts");

        String linked = DuplicationReportGenerator.getFilesDisplayHtml(instance, true);
        assertFalse(linked.contains("<img src=x"), linked);
        assertTrue(linked.contains("<a href='../src/viewer.html#aspect=main&file=src/%3Cimg%20src%3Dx%20onerror%3DXSS%3E.ts' target='_blank'>&lt;img src=x onerror=XSS&gt;.ts</a>"), linked);
        assertTrue(linked.contains("</br><a href='../src/viewer.html#aspect=main&file=src/odd%20%23%26%25%20dir/plus%2Bfile.ts' target='_blank'>plus+file.ts</a>"), linked);

        String plain = DuplicationReportGenerator.getFilesDisplayHtml(instance, false);
        assertFalse(plain.contains("<img src=x"), plain);
        assertTrue(plain.contains("<span title='&lt;img src=x onerror=XSS&gt;.ts'>&lt;img src=x onerror=XSS&gt;.ts</span>"), plain);
    }

    @Test
    void filesCellCapsTheNumberOfSamples() {
        String[] paths = new String[DuplicationInstance.MAX_NUMBER_OF_SAMPLES_FOR_DISPLAY + 2];
        for (int i = 0; i < paths.length; i++) {
            paths[i] = "src/f" + i + ".java";
        }
        String html = DuplicationReportGenerator.getFilesDisplayHtml(duplicate(5, paths), false);
        assertTrue(html.contains("f" + (DuplicationInstance.MAX_NUMBER_OF_SAMPLES_FOR_DISPLAY - 1) + ".java"), html);
        assertFalse(html.contains("f" + DuplicationInstance.MAX_NUMBER_OF_SAMPLES_FOR_DISPLAY + ".java"), html);
        assertTrue(html.endsWith("</br>..."), html);
    }

    @Test
    void rendersMostFrequentDuplicatesSectionWhenPresent() {
        CodeAnalysisResults results = results();
        results.getDuplicationAnalysisResults().getMostFrequentDuplicates().add(duplicate(8, "a/x.java", "b/y.java", "c/z.java"));
        results.getDuplicationAnalysisResults().getMostFrequentDuplicates().add(duplicate(6, "a/p.java", "b/q.java", "c/r.java"));

        RichTextReport report = new RichTextReport("Duplication", "Duplication.html");
        new DuplicationReportGenerator(results, new File("target")).addMostFrequentDuplicatesList(report);

        String html = renderedHtml(report);
        assertTrue(html.contains("Most Frequent Duplicates"), "section header should be rendered");
        assertTrue(html.contains("The list of 2 most frequently found duplicates."), "section description should reflect the count");
    }

    @Test
    void rendersNothingWhenNoMostFrequentDuplicates() {
        CodeAnalysisResults results = results();

        RichTextReport report = new RichTextReport("Duplication", "Duplication.html");
        new DuplicationReportGenerator(results, new File("target")).addMostFrequentDuplicatesList(report);

        String html = renderedHtml(report);
        assertFalse(html.contains("Most Frequent Duplicates"), "no section should be rendered for an empty list");
    }

    private String renderedHtml(RichTextReport report) {
        StringBuilder html = new StringBuilder();
        report.getRichTextFragments().forEach(fragment -> html.append(fragment.getFragment()));
        return html.toString();
    }
}
