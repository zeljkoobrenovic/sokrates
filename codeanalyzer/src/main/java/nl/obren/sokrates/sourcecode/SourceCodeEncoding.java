/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Reads source code, detecting the character encoding from a leading byte order mark (BOM) instead of
 * assuming UTF-8.
 *
 * <p>Decoding every file as UTF-8 silently corrupts UTF-16 sources: line breaks still survive, so
 * lines-of-code counts look normal while every unit-extraction regex fails to match, yielding zero units
 * and no complexity metrics. Windows tooling produces UTF-16 by default in several common cases (for
 * example SQL Server Management Studio exports .sql as UTF-16 LE with a BOM), so this is not a rare
 * situation.
 *
 * <p>Only a BOM is used to select the charset - no statistical charset guessing. A file without a BOM
 * is decoded as UTF-8 exactly as before, so existing UTF-8 and ASCII code bases are unaffected: same
 * bytes in, same strings out.
 *
 * <p>The BOM itself is never part of the returned text. Leaving a U+FEFF at the start of the content
 * would break regular expressions anchored with {@code ^} on the first line.
 */
public class SourceCodeEncoding {
    // UTF-32 is deliberately absent: it is not encountered in practice for source code, and its BOMs
    // start with the UTF-16 BOM byte sequences, so including it would require careful ordering here
    // for no practical gain.
    private static final ByteOrderMark[] SUPPORTED_BOMS = {
            ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE
    };

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private SourceCodeEncoding() {
    }

    /**
     * Decodes source code bytes, honouring a leading BOM and falling back to UTF-8 when there is none.
     *
     * @param bytes the raw bytes of a source file; an empty array yields an empty string
     * @return the decoded text, without any BOM character
     */
    public static String decode(byte[] bytes) {
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            return read(inputStream);
        } catch (IOException e) {
            // Unreachable: a ByteArrayInputStream performs no I/O. Rethrown unchecked so that callers
            // holding bytes in memory are not forced to handle an exception that cannot occur.
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads a source file, honouring a leading BOM and falling back to UTF-8 when there is none.
     *
     * @return the file's text, without any BOM character
     */
    public static String read(File file) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            return read(inputStream);
        }
    }

    /**
     * Reads a source file as lines, honouring a leading BOM and falling back to UTF-8 when there is none.
     *
     * <p>Line splitting matches {@code FileUtils.readLines}: lines are separated by any of LF, CRLF or
     * CR, the terminators are not included, and a trailing terminator does not produce a final empty
     * line.
     *
     * @return the file's lines, with any BOM character removed from the first line
     */
    public static List<String> readLines(File file) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            return readLines(inputStream);
        }
    }

    private static String read(InputStream source) throws IOException {
        try (BOMInputStream inputStream = bomInputStream(source)) {
            return IOUtils.toString(inputStream, charsetOf(inputStream));
        }
    }

    private static List<String> readLines(InputStream source) throws IOException {
        try (BOMInputStream inputStream = bomInputStream(source)) {
            return IOUtils.readLines(inputStream, charsetOf(inputStream));
        }
    }

    private static BOMInputStream bomInputStream(InputStream source) throws IOException {
        return BOMInputStream.builder()
                .setInputStream(source)
                // Exclude the BOM from the stream so it never reaches the decoded text.
                .setInclude(false)
                .setByteOrderMarks(SUPPORTED_BOMS)
                .get();
    }

    /**
     * Resolves the charset from the detected BOM. Reading the BOM buffers the first bytes of the stream
     * without consuming them, so this must be called before the stream is read.
     */
    private static Charset charsetOf(BOMInputStream inputStream) throws IOException {
        String charsetName = inputStream.getBOMCharsetName();
        return charsetName != null ? Charset.forName(charsetName) : DEFAULT_CHARSET;
    }
}
