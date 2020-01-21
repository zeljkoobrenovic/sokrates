/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.aspects;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.operations.OperationStatement;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MetaRulesProcessorTest {

    private SourceFile newSourceFile(String relativePath, String content) {
        SourceFile sourceFile = new SourceFile();
        sourceFile.setFile(new File(relativePath));
        sourceFile.setRelativePath(relativePath);
        sourceFile.setContent(content);

        return sourceFile;
    }

    @Test
    public void extractConcerns() {
        NamedSourceCodeAspect aspect = new NamedSourceCodeAspect("main");
        aspect.getSourceFiles().add(newSourceFile("path1a/path2a/File1a.java", "abc 1\ndef 1\nefg"));
        aspect.getSourceFiles().add(newSourceFile("path1a/path2a/File2a.java", "abc 1\ndef 2\nefg"));
        aspect.getSourceFiles().add(newSourceFile("path1b/path2b/File1a.java", "abc 2\ndef 1\nefg"));
        aspect.getSourceFiles().add(newSourceFile("path1b/path2b/File1b.java", "abc 2\ndef 2\nefg"));

        MetaRulesProcessor processor = MetaRulesProcessor.getCrossCurringConcernsInstance();

        MetaRule content = new MetaRule(".*", ".*2.*", "content");
        List<MetaRule> rules = Arrays.asList(content);

        List<CrossCuttingConcern> concerns = processor.extractAspects(aspect.getSourceFiles(), rules);

        assertEquals(2, concerns.size());
        assertEquals("def 2", concerns.get(0).getName());
        assertEquals(2, concerns.get(0).getSourceFiles().size());

        assertEquals("abc 2", concerns.get(1).getName());
        assertEquals(2, concerns.get(1).getSourceFiles().size());

        processor = MetaRulesProcessor.getCrossCurringConcernsInstance();
        content.getNameOperations().add(new OperationStatement("extract", Arrays.asList("[0-9]+")));
        concerns = processor.extractAspects(aspect.getSourceFiles(), rules);

        assertEquals(1, concerns.size());
        assertEquals("2", concerns.get(0).getName());
        assertEquals(3, concerns.get(0).getSourceFiles().size());
    }

    @Test
    public void extractConcernsUnique() {
        NamedSourceCodeAspect aspect = new NamedSourceCodeAspect("main");
        aspect.getSourceFiles().add(newSourceFile("path1a/path2a/File1a.java", "abc 2\ndef 2\nefg 2"));
        aspect.getSourceFiles().add(newSourceFile("path1a/path2a/File2a.java", "abc 1\ndef 1\nefg 2"));
        aspect.getSourceFiles().add(newSourceFile("path1b/path2b/File1a.java", "abc 2\ndef 2\nefg 2"));
        aspect.getSourceFiles().add(newSourceFile("path1b/path2b/File1b.java", "abc 2\ndef 2\nefg 2"));

        MetaRulesProcessor processor = MetaRulesProcessor.getLogicalDecompositionInstance();

        MetaRule content = new MetaRule(".*", ".*2.*", "content");
        List<MetaRule> rules = Arrays.asList(content);

        List<NamedSourceCodeAspect> components = processor.extractAspects(aspect.getSourceFiles(), rules);

        assertEquals(2, components.size());
        assertEquals("abc 2", components.get(0).getName());
        assertEquals(3, components.get(0).getSourceFiles().size());

        assertEquals("efg 2", components.get(1).getName());
        assertEquals(1, components.get(1).getSourceFiles().size());

        content.getNameOperations().add(new OperationStatement("extract", Arrays.asList("[0-9]+")));
        components = processor.extractAspects(aspect.getSourceFiles(), rules);

        assertEquals(1, components.size());
        assertEquals("2", components.get(0).getName());
        assertEquals(4, components.get(0).getSourceFiles().size());
    }

    @Test
    public void extractConcernsAnyLines() {
        NamedSourceCodeAspect aspect = new NamedSourceCodeAspect("main");
        aspect.getSourceFiles().add(newSourceFile("path1a/path2a/File1a.java", "abc 1\ndef 1\nefg"));
        aspect.getSourceFiles().add(newSourceFile("path1a/path2a/File2a.java", "abc 1\ndef 2\nefg"));
        aspect.getSourceFiles().add(newSourceFile("path1b/path2b/File1a.java", "abc 2\ndef 1\nefg"));
        aspect.getSourceFiles().add(newSourceFile("path1b/path2b/File1b.java", "abc 2\ndef 2\nefg"));

        MetaRulesProcessor processor = MetaRulesProcessor.getCrossCurringConcernsInstance();

        List<MetaRule> rules = Arrays.asList(new MetaRule(".*", ".*", "content"));

        List<CrossCuttingConcern> concerns = processor.extractAspects(aspect.getSourceFiles(), rules);

        assertEquals(5, concerns.size());
        assertEquals("abc 1", concerns.get(0).getName());
        assertEquals(2, concerns.get(0).getSourceFiles().size());

        assertEquals("def 1", concerns.get(1).getName());
        assertEquals(2, concerns.get(1).getSourceFiles().size());

        assertEquals("efg", concerns.get(2).getName());
        assertEquals(4, concerns.get(2).getSourceFiles().size());

        assertEquals("def 2", concerns.get(3).getName());
        assertEquals(2, concerns.get(3).getSourceFiles().size());

        assertEquals("abc 2", concerns.get(4).getName());
        assertEquals(2, concerns.get(4).getSourceFiles().size());
    }
}
