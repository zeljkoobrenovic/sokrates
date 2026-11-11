/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class SourceCodeEncodingTest {
    private static final byte[] BOM_UTF_8 = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final byte[] BOM_UTF_16_LE = {(byte) 0xFF, (byte) 0xFE};
    private static final byte[] BOM_UTF_16_BE = {(byte) 0xFE, (byte) 0xFF};

    private static final String SAMPLE = "public class Foo {\n    void bar() {\n    }\n}";

    @Test
    public void decodesUtf8WithoutBom() {
        assertEquals(SAMPLE, SourceCodeEncoding.decode(SAMPLE.getBytes(UTF_8)));
    }

    @Test
    public void decodesNonAsciiUtf8WithoutBom() {
        // Guards the fallback path: a file with no BOM must still decode as UTF-8, multi-byte characters included.
        String text = "// éü中文 🚀";

        assertEquals(text, SourceCodeEncoding.decode(text.getBytes(UTF_8)));
    }

    @Test
    public void stripsUtf8Bom() {
        String decoded = SourceCodeEncoding.decode(concat(BOM_UTF_8, SAMPLE.getBytes(UTF_8)));

        assertEquals(SAMPLE, decoded);
        // An unstripped U+FEFF would break regexes anchored with '^' on the first line.
        assertTrue(decoded.startsWith("public"));
    }

    @Test
    public void decodesUtf16LittleEndianWithBom() {
        assertEquals(SAMPLE, SourceCodeEncoding.decode(concat(BOM_UTF_16_LE, SAMPLE.getBytes(UTF_16LE))));
    }

    @Test
    public void decodesUtf16BigEndianWithBom() {
        assertEquals(SAMPLE, SourceCodeEncoding.decode(concat(BOM_UTF_16_BE, SAMPLE.getBytes(UTF_16BE))));
    }

    @Test
    public void decodesEmptyInput() {
        assertEquals("", SourceCodeEncoding.decode(new byte[0]));
    }

    @Test
    public void decodesInputThatIsOnlyABom() {
        assertEquals("", SourceCodeEncoding.decode(BOM_UTF_16_LE));
    }

    @Test
    public void readsFileInEveryEncoding() throws IOException {
        assertEquals(SAMPLE, SourceCodeEncoding.read(fileWith(SAMPLE.getBytes(UTF_8))));
        assertEquals(SAMPLE, SourceCodeEncoding.read(fileWith(concat(BOM_UTF_8, SAMPLE.getBytes(UTF_8)))));
        assertEquals(SAMPLE, SourceCodeEncoding.read(fileWith(concat(BOM_UTF_16_LE, SAMPLE.getBytes(UTF_16LE)))));
        assertEquals(SAMPLE, SourceCodeEncoding.read(fileWith(concat(BOM_UTF_16_BE, SAMPLE.getBytes(UTF_16BE)))));
    }

    @Test
    public void readsLinesInEveryEncoding() throws IOException {
        List<String> expected = Arrays.asList("public class Foo {", "    void bar() {", "    }", "}");

        assertEquals(expected, SourceCodeEncoding.readLines(fileWith(SAMPLE.getBytes(UTF_8))));
        assertEquals(expected, SourceCodeEncoding.readLines(fileWith(concat(BOM_UTF_8, SAMPLE.getBytes(UTF_8)))));
        assertEquals(expected, SourceCodeEncoding.readLines(fileWith(concat(BOM_UTF_16_LE, SAMPLE.getBytes(UTF_16LE)))));
        assertEquals(expected, SourceCodeEncoding.readLines(fileWith(concat(BOM_UTF_16_BE, SAMPLE.getBytes(UTF_16BE)))));
    }

    @Test
    public void readMatchesPreviousUtf8BehaviourWhenThereIsNoBom() throws IOException {
        // The central compatibility guarantee: for files without a BOM - the overwhelming majority -
        // this must return exactly what the previous hard-coded UTF-8 read returned.
        for (String text : samplesWithVariedLineEndings()) {
            File file = fileWith(text.getBytes(UTF_8));

            assertEquals(FileUtils.readFileToString(file, UTF_8), SourceCodeEncoding.read(file));
        }
    }

    @Test
    public void readLinesMatchesPreviousUtf8BehaviourWhenThereIsNoBom() throws IOException {
        // Same guarantee for the line-based path, including CRLF, CR, blank lines and a trailing newline.
        for (String text : samplesWithVariedLineEndings()) {
            File file = fileWith(text.getBytes(UTF_8));

            assertEquals(FileUtils.readLines(file, UTF_8), SourceCodeEncoding.readLines(file));
        }
    }

    private List<String> samplesWithVariedLineEndings() {
        return Arrays.asList(
                SAMPLE,
                "a\r\nb\r\nc",
                "a\rb\rc",
                "a\n\n\nb",
                "trailing newline\n",
                "no newline at all",
                "",
                "// éü中文");
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private File fileWith(byte[] bytes) throws IOException {
        Path file = Files.createTempFile("sokrates-encoding-", ".txt");
        Files.write(file, bytes);
        File asFile = file.toFile();
        asFile.deleteOnExit();
        return asFile;
    }
}
