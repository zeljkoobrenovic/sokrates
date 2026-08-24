/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * A block shared among more than two files.
 *
 * <p>That is the branch where the engine enumerates the file pairs of one duplicated block. The
 * existing DuplicationEngineTest only covers two files, where there is exactly one pair and nothing
 * to enumerate; with three files the pairing is what decides which duplication instances come out.
 */
public class DuplicationEngineSharedBlockTest {

    @Test
    public void aBlockSharedAmongThreeFilesYieldsOneInstanceOverAllThree() {
        String shared = "alpha\nbeta\ngamma\ndelta\nepsilon\nzeta\n";
        List<SourceFile> sourceFiles = Arrays.asList(
                new SourceFile(new File("c.unknown"), shared + "tail c\n"),
                new SourceFile(new File("a.unknown"), shared + "tail a\n"),
                new SourceFile(new File("b.unknown"), shared + "tail b\n"));

        List<DuplicationInstance> duplicates =
                new DuplicationEngine().findDuplicates(sourceFiles, 6, new ProgressFeedback());

        assertEquals("{[a.unknown, b.unknown, c.unknown]=1}", describe(duplicates));
    }

    @Test
    public void everyPairIsCompared() {
        // The case above cannot detect a lost pair: all three files hold the same block, so the
        // instances merge and one surviving pair drags the rest in. Here each pair also shares a block
        // that the third file does not have, and d/e share one with nobody else - so dropping any pair,
        // or giving two pairs the same identity, loses instances nothing can recover.
        String all = "alpha\nbeta\ngamma\ndelta\nepsilon\nzeta\n";
        String ab = "ab1\nab2\nab3\nab4\nab5\nab6\n";
        String ac = "ac1\nac2\nac3\nac4\nac5\nac6\n";
        String bc = "bc1\nbc2\nbc3\nbc4\nbc5\nbc6\n";
        String de = "de1\nde2\nde3\nde4\nde5\nde6\n";
        List<SourceFile> sourceFiles = Arrays.asList(
                new SourceFile(new File("c.unknown"), all + ac + bc + "tail c\n"),
                new SourceFile(new File("a.unknown"), all + ab + ac + "tail a\n"),
                new SourceFile(new File("b.unknown"), all + ab + bc + "tail b\n"),
                new SourceFile(new File("d.unknown"), de + "tail d\n"),
                new SourceFile(new File("e.unknown"), de + "tail e\n"));

        List<DuplicationInstance> duplicates =
                new DuplicationEngine().findDuplicates(sourceFiles, 6, new ProgressFeedback());

        assertEquals("{[a.unknown, b.unknown, c.unknown]=1, [a.unknown, b.unknown]=6, [a.unknown, c.unknown]=1, [b.unknown, c.unknown]=1, [d.unknown, e.unknown]=1}", describe(duplicates));
    }

    /** How many duplication instances each set of files takes part in - order-independent. */
    private String describe(List<DuplicationInstance> duplicates) {
        Map<String, Integer> countsByFileSet = new TreeMap<>();
        for (DuplicationInstance duplicate : duplicates) {
            List<String> names = duplicate.getDuplicatedFileBlocks().stream()
                    .map(block -> block.getSourceFile().getFile().getName())
                    .sorted()
                    .collect(Collectors.toList());
            countsByFileSet.merge(names.toString(), 1, Integer::sum);
        }
        return countsByFileSet.toString();
    }
}
