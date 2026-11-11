package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * UnitsAnalyzer.updateFilesWithUnitInfo() is private and needs a full analysis context, so this test
 * pins the aggregation logic it relies on: grouping units by source File (by identity, matching the
 * original 'u.getSourceFile().getFile() == sourceFile.getFile()' comparison) and summing count /
 * McCabe / LOC per file must produce the same per-file totals as the original per-file filter pass.
 */
class UnitsAnalyzerAggregationTest {

    private UnitInfo unit(SourceFile sourceFile, int mcCabe, int loc) {
        UnitInfo unit = new UnitInfo();
        unit.setSourceFile(sourceFile);
        unit.setMcCabeIndex(mcCabe);
        unit.setLinesOfCode(loc);
        return unit;
    }

    // The original O(files*units) computation, reproduced for comparison.
    private int[] oldAggregate(SourceFile sourceFile, List<UnitInfo> allUnits) {
        int[] acc = new int[3];
        allUnits.stream().filter(u -> u.getSourceFile().getFile() == sourceFile.getFile()).forEach(u -> {
            acc[0] += 1;
            acc[1] += u.getMcCabeIndex();
            acc[2] += u.getLinesOfCode();
        });
        return acc;
    }

    // The new single-pass group-by aggregation.
    private Map<File, int[]> newAggregate(List<UnitInfo> allUnits) {
        Map<File, int[]> byFile = new IdentityHashMap<>();
        allUnits.forEach(u -> {
            int[] acc = byFile.computeIfAbsent(u.getSourceFile().getFile(), k -> new int[3]);
            acc[0] += 1;
            acc[1] += u.getMcCabeIndex();
            acc[2] += u.getLinesOfCode();
        });
        return byFile;
    }

    @Test
    public void groupedAggregationMatchesPerFileFilter() {
        SourceFile a = new SourceFile(new File("a.java"));
        SourceFile b = new SourceFile(new File("b.java"));
        SourceFile c = new SourceFile(new File("c.java")); // no units

        List<UnitInfo> allUnits = new ArrayList<>();
        allUnits.add(unit(a, 2, 10));
        allUnits.add(unit(a, 3, 15));
        allUnits.add(unit(b, 5, 40));

        List<SourceFile> files = new ArrayList<>();
        files.add(a);
        files.add(b);
        files.add(c);

        Map<File, int[]> grouped = newAggregate(allUnits);
        for (SourceFile file : files) {
            int[] expected = oldAggregate(file, allUnits);
            int[] actual = grouped.getOrDefault(file.getFile(), new int[3]);
            assertEquals(expected[0], actual[0], "count for " + file.getFile());
            assertEquals(expected[1], actual[1], "mcCabe sum for " + file.getFile());
            assertEquals(expected[2], actual[2], "loc for " + file.getFile());
        }

        // explicit values
        assertEquals(2, grouped.get(a.getFile())[0]);
        assertEquals(5, grouped.get(a.getFile())[1]);
        assertEquals(25, grouped.get(a.getFile())[2]);
        assertEquals(1, grouped.get(b.getFile())[0]);
    }

    @Test
    public void distinctFileInstancesForSamePathAreNotGroupedTogether() {
        // The original used reference equality on File, so two distinct File instances (even with the
        // same path) were treated as separate. IdentityHashMap preserves that.
        SourceFile a1 = new SourceFile(new File("same.java"));
        SourceFile a2 = new SourceFile(new File("same.java")); // equal path, different instance

        List<UnitInfo> allUnits = new ArrayList<>();
        allUnits.add(unit(a1, 1, 5));
        allUnits.add(unit(a2, 2, 7));

        Map<File, int[]> grouped = newAggregate(allUnits);
        assertEquals(2, grouped.size());
        assertEquals(1, grouped.get(a1.getFile())[0]);
        assertEquals(1, grouped.get(a2.getFile())[0]);
    }
}
