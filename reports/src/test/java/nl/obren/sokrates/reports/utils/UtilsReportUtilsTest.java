package nl.obren.sokrates.reports.utils;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilsReportUtilsTest {

    private UnitInfo unit(String shortName, String path) {
        SourceFile sourceFile = new SourceFile(new File(path));
        sourceFile.setRelativePath(path);
        UnitInfo unit = new UnitInfo();
        unit.setShortName(shortName);
        unit.setSourceFile(sourceFile);
        unit.setLinesOfCode(12);
        unit.setMcCabeIndex(3);
        unit.setNumberOfParameters(1);
        return unit;
    }

    @Test
    void escapesUnitNameAndPathAndEncodesViewerLink() {
        List<UnitInfo> units = List.of(unit("gen<T>", "src/pwn\" onmouseover=\"XSS/<img src=x onerror=XSS>.ts"));

        String table = UtilsReportUtils.getUnitsTable(units, "longest_unit", true, true);

        assertFalse(table.contains("<img src=x"), table);
        assertFalse(table.contains("gen<T>"), table);
        assertTrue(table.contains("href='../src/viewer.html#bundle=fragments/longest_unit.json&i=1'>gen&lt;T&gt;</a>"), table);
        assertTrue(table.contains("href='../src/viewer.html#aspect=main&file=src/pwn%22%20onmouseover%3D%22XSS/%3Cimg%20src%3Dx%20onerror%3DXSS%3E.ts'>"
                + "src/pwn&quot; onmouseover=&quot;XSS/&lt;img src=x onerror=XSS&gt;.ts</a>"), table);
    }

    @Test
    void escapesWithoutLinksToo() {
        String table = UtilsReportUtils.getUnitsTable(List.of(unit("a<b", "src/<x>.ts")), "longest_unit", false, false);

        assertFalse(table.contains("a<b"), table);
        assertFalse(table.contains("<x>"), table);
        assertTrue(table.contains("<b>a&lt;b</b>"), table);
        assertTrue(table.contains("in src/&lt;x&gt;.ts"), table);
    }

    @Test
    void ordinaryUnitsRenderUnchanged() {
        String table = UtilsReportUtils.getUnitsTable(List.of(unit("compute", "src/main/Foo.java")), "longest_unit", true, true);
        assertTrue(table.contains(">compute</a>"), table);
        assertTrue(table.contains("href='../src/viewer.html#aspect=main&file=src/main/Foo.java'>src/main/Foo.java</a>"), table);
    }
}
