/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import nl.obren.sokrates.sourcecode.units.UnitsExtractor;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.TestCase.assertEquals;

/**
 * End-to-end cover for the encoding fix: a source file must produce the same analysis regardless of the
 * encoding it happens to be stored in.
 *
 * <p>Uses C# because Windows tooling is where UTF-16 sources actually come from, and because it exercises
 * a language analyzer other than the Java one.
 */
public class SourceFileEncodingTest {
    private static final byte[] BOM_UTF_8 = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final byte[] BOM_UTF_16_LE = {(byte) 0xFF, (byte) 0xFE};
    private static final byte[] BOM_UTF_16_BE = {(byte) 0xFE, (byte) 0xFF};

    private static final String CSHARP_SOURCE = "using System;\n"
            + "namespace Demo {\n"
            + "    public class Calculator {\n"
            + "        public int Compute(int a, int b) {\n"
            + "            if (a > 0 && b > 0) { return a + b; }\n"
            + "            return 0;\n"
            + "        }\n"
            + "        public int Other(int x) {\n"
            + "            for (int i = 0; i < x; i++) { x += i; }\n"
            + "            return x;\n"
            + "        }\n"
            + "    }\n"
            + "}\n";

    @Test
    public void getContentHonoursTheByteOrderMark() throws IOException {
        for (byte[] bytes : theSameSourceInEveryEncoding()) {
            assertEquals(CSHARP_SOURCE, new SourceFile(sourceFileWith(bytes)).getContent());
        }
    }

    @Test
    public void getLinesHonoursTheByteOrderMark() throws IOException {
        List<String> expected = Arrays.asList(CSHARP_SOURCE.split("\n"));

        for (byte[] bytes : theSameSourceInEveryEncoding()) {
            assertEquals(expected, new SourceFile(sourceFileWith(bytes)).getLines());
        }
    }

    @Test
    public void linesOfCodeAreIdenticalAcrossEncodings() throws IOException {
        for (byte[] bytes : theSameSourceInEveryEncoding()) {
            SourceFile sourceFile = new SourceFile(sourceFileWith(bytes));
            sourceFile.setLinesOfCodeFromContent();

            assertEquals(13, sourceFile.getLinesOfCode());
        }
    }

    @Test
    public void unitsAndComplexityAreIdenticalAcrossEncodings() throws IOException {
        // Before the fix, the UTF-16 variants yielded zero units while lines of code still counted
        // normally - a silent loss of every complexity metric.
        for (byte[] bytes : theSameSourceInEveryEncoding()) {
            List<UnitInfo> units = unitsOf(sourceFileWith(bytes));

            assertEquals(Arrays.asList("public int Compute()", "public int Other()"), namesOf(units));
            assertEquals(Arrays.asList(3, 2), complexitiesOf(units));
        }
    }

    @Test
    public void aWindowsExportIsAnalysedWithoutBeingReEncodedFirst() throws IOException {
        // UTF-16 LE with a BOM *and* CRLF: the shape Windows tooling actually emits, and the one
        // combination not covered above, where the encoding varies but the line endings stay LF.
        byte[] export = concat(BOM_UTF_16_LE, CSHARP_SOURCE.replace("\n", "\r\n").getBytes(UTF_16LE));
        File file = sourceFileWith(export);

        assertEquals(Arrays.asList(CSHARP_SOURCE.split("\n")), new SourceFile(file).getLines());

        SourceFile sourceFile = new SourceFile(file);
        sourceFile.setLinesOfCodeFromContent();
        assertEquals(13, sourceFile.getLinesOfCode());

        // Units are asserted beside the lines of code in one method for the reason the class javadoc
        // gives: a wrong decode still counts lines normally and only empties the unit list.
        List<UnitInfo> units = unitsOf(file);
        assertEquals(Arrays.asList("public int Compute()", "public int Other()"), namesOf(units));
        assertEquals(Arrays.asList(3, 2), complexitiesOf(units));
    }

    private List<byte[]> theSameSourceInEveryEncoding() {
        return Arrays.asList(
                CSHARP_SOURCE.getBytes(UTF_8),
                concat(BOM_UTF_8, CSHARP_SOURCE.getBytes(UTF_8)),
                concat(BOM_UTF_16_LE, CSHARP_SOURCE.getBytes(UTF_16LE)),
                concat(BOM_UTF_16_BE, CSHARP_SOURCE.getBytes(UTF_16BE)));
    }

    private List<UnitInfo> unitsOf(File file) {
        return new UnitsExtractor().getUnits(Collections.singletonList(new SourceFile(file)), new ProgressFeedback());
    }

    private List<String> namesOf(List<UnitInfo> units) {
        return units.stream().map(UnitInfo::getShortName).collect(Collectors.toList());
    }

    private List<Integer> complexitiesOf(List<UnitInfo> units) {
        return units.stream().map(UnitInfo::getMcCabeIndex).collect(Collectors.toList());
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private File sourceFileWith(byte[] bytes) throws IOException {
        Path file = Files.createTempFile("sokrates-encoding-", ".cs");
        Files.write(file, bytes);
        File asFile = file.toFile();
        asFile.deleteOnExit();
        return asFile;
    }
}
