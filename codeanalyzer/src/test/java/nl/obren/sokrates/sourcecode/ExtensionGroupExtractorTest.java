/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.TestCase.*;

public class ExtensionGroupExtractorTest {
    @Test
    public void isKnownSourceCodeExtension() throws Exception {
        assertTrue(ExtensionGroupExtractor.isKnownSourceCodeExtension("java"));
        assertTrue(ExtensionGroupExtractor.isKnownSourceCodeExtension("cs"));
        assertTrue(ExtensionGroupExtractor.isKnownSourceCodeExtension("js"));
        assertTrue(ExtensionGroupExtractor.isKnownSourceCodeExtension("html"));

        assertFalse(ExtensionGroupExtractor.isKnownSourceCodeExtension("exe"));
        assertFalse(ExtensionGroupExtractor.isKnownSourceCodeExtension("zip"));
        assertFalse(ExtensionGroupExtractor.isKnownSourceCodeExtension("unknown"));
    }

    @Test
    public void isKnownBinaryExtension() throws Exception {
        assertTrue(ExtensionGroupExtractor.isKnownBinaryExtension("zip"));
        assertTrue(ExtensionGroupExtractor.isKnownBinaryExtension("jar"));
        assertTrue(ExtensionGroupExtractor.isKnownBinaryExtension("exe"));
        assertTrue(ExtensionGroupExtractor.isKnownBinaryExtension("dll"));

        assertFalse(ExtensionGroupExtractor.isKnownBinaryExtension("java"));
        assertFalse(ExtensionGroupExtractor.isKnownBinaryExtension("cs"));
        assertFalse(ExtensionGroupExtractor.isKnownBinaryExtension("unknown"));

    }

    @Test
    public void isKnownNonSourceExtensions() throws Exception {
        assertTrue(ExtensionGroupExtractor.isKnownIgnorableExtension("ds_store"));

        assertFalse(ExtensionGroupExtractor.isKnownIgnorableExtension("java"));
    }

    @Test
    public void extractExtensionsInfo() throws Exception {
        ExtensionGroupExtractor extractor = new ExtensionGroupExtractor();

        Path tempDirectoryPath = Files.createTempDirectory("test");

        createTempFile(tempDirectoryPath, "TestClass", ".java", " ");
        createTempFile(tempDirectoryPath, "TestClass", ".java", " ");
        createTempFile(tempDirectoryPath, "TestClass", ".java", " ");

        createTempFile(tempDirectoryPath, "TestJs", ".js", " ");
        createTempFile(tempDirectoryPath, "TestJs", ".js", " ");

        createTempFile(tempDirectoryPath, "TestHtml", ".html", " ");

        extractor.extractExtensionsInfo(tempDirectoryPath.toFile());

        assertEquals(extractor.getExtensionsList().size(), 3);
        assertEquals(extractor.getExtensionsList().get(0).toString(), "java: 3 files");
        assertEquals(extractor.getExtensionsList().get(1).toString(), "js: 2 files");
        assertEquals(extractor.getExtensionsList().get(2).toString(), "html: 1 files");
    }

    @Test
    public void getExtensionsList() throws Exception {
        ExtensionGroupExtractor extractor = new ExtensionGroupExtractor();

        Path tempDirectoryPath = Files.createTempDirectory("test");

        createTempFile(tempDirectoryPath, "TestJs", ".js", "js");

        createTempFile(tempDirectoryPath, "TestUnknown", ".unknown", " ");

        createTempFile(tempDirectoryPath, "TestClass", ".java", "java");
        createTempFile(tempDirectoryPath, "TestClass", ".java", "java");

        extractor.extractExtensionsInfo(tempDirectoryPath.toFile());

        assertEquals(extractor.getExtensionsList().size(), 3);
        assertEquals(extractor.getExtensionsList().get(0).toString(), "java: 2 files");
        assertEquals(extractor.getExtensionsList().get(1).toString(), "js: 1 files");
        assertEquals(extractor.getExtensionsList().get(2).toString(), "unknown: 1 files");

    }

    private void createTempFile(Path root, String prefix, String suffix, String content) throws IOException {
        Path tempFile = Files.createTempFile(root, prefix, suffix);
        FileUtils.writeStringToFile(tempFile.toFile(), content, UTF_8);
    }
}
