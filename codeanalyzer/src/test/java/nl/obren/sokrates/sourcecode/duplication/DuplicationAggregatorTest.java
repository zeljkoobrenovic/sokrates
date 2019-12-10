/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication;

import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class DuplicationAggregatorTest {
    @Test
    public void getDuplicationPerSourceFile() throws Exception {
        List<DuplicationInstance> duplicates = new ArrayList<>();

        DuplicationInstance instance = new DuplicationInstance();
        DuplicatedFileBlock duplicatedFileBlock = new DuplicatedFileBlock();
        duplicatedFileBlock.setSourceFile(new SourceFile(new File("/root/a.b")));
        duplicatedFileBlock.setCleanedStartLine(1);
        duplicatedFileBlock.setCleanedEndLine(10);
        duplicatedFileBlock.setSourceFileCleanedLinesOfCode(100);

        instance.getDuplicatedFileBlocks().add(duplicatedFileBlock);

        duplicates.add(instance);

        List<SourceFileDuplication> duplicationPerSourceFile = DuplicationAggregator.getDuplicationPerSourceFile(duplicates);

        assertEquals(duplicationPerSourceFile.size(), 1);
        assertEquals(duplicationPerSourceFile.get(0).getCleanedLinesOfCode(), 100);
        assertEquals(duplicationPerSourceFile.get(0).getDuplicatedLinesOfCode(), 10);
    }

    @Test
    public void getDuplicationPerLogicalComponent() throws Exception {
        List<DuplicationInstance> duplicates = new ArrayList<>();

        NamedSourceCodeAspect namedSourceCodeAspect = new NamedSourceCodeAspect("A");

        DuplicationInstance instance = new DuplicationInstance();
        DuplicatedFileBlock duplicatedFileBlock1 = new DuplicatedFileBlock();
        SourceFile sourceFile1 = new SourceFile(new File("/root/a.b"));
        sourceFile1.setRelativePath("a.b");
        sourceFile1.getLogicalComponents().add(namedSourceCodeAspect);
        duplicatedFileBlock1.setSourceFile(sourceFile1);
        duplicatedFileBlock1.setCleanedStartLine(11);
        duplicatedFileBlock1.setCleanedEndLine(60);
        duplicatedFileBlock1.setSourceFileCleanedLinesOfCode(100);

        DuplicatedFileBlock duplicatedFileBlock2 = new DuplicatedFileBlock();
        SourceFile sourceFile2 = new SourceFile(new File("/root/d.e"));
        sourceFile2.getLogicalComponents().add(namedSourceCodeAspect);
        sourceFile2.setRelativePath("d.e");
        duplicatedFileBlock2.setSourceFile(sourceFile2);
        duplicatedFileBlock2.setCleanedStartLine(101);
        duplicatedFileBlock2.setCleanedEndLine(150);
        duplicatedFileBlock2.setSourceFileCleanedLinesOfCode(200);

        instance.getDuplicatedFileBlocks().add(duplicatedFileBlock1);
        instance.getDuplicatedFileBlocks().add(duplicatedFileBlock2);

        duplicates.add(instance);

        List<SourceFileDuplication> duplicationPerSourceFile = DuplicationAggregator.getDuplicationPerSourceFile(duplicates);
        assertEquals(duplicationPerSourceFile.size(), 2);
        assertEquals(duplicationPerSourceFile.get(0).getCleanedLinesOfCode(), 100);
        assertEquals(duplicationPerSourceFile.get(0).getDuplicatedLinesOfCode(), 50);
        assertEquals(duplicationPerSourceFile.get(1).getCleanedLinesOfCode(), 200);
        assertEquals(duplicationPerSourceFile.get(1).getDuplicatedLinesOfCode(), 50);

        List<AspectDuplication> duplicationPerSourceComponent = DuplicationAggregator.getDuplicationPerLogicalComponent(
                Arrays.asList(new LogicalDecomposition("")), Arrays.asList(sourceFile1,
                sourceFile2),
                duplicationPerSourceFile);

        assertEquals(duplicationPerSourceComponent.size(), 1);
        assertEquals(duplicationPerSourceComponent.get(0).getAspect().getName(), "A");
        assertEquals(duplicationPerSourceComponent.get(0).getCleanedLinesOfCode(), 2);
        assertEquals(duplicationPerSourceComponent.get(0).getDuplicatedLinesOfCode(), 100);
    }

}
