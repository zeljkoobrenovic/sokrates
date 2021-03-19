package nl.obren.sokrates.sourcecode.lang.thrift;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.lang.swift.SwiftExampleFragments;
import org.junit.jupiter.api.Test;

import java.io.File;

import static nl.obren.sokrates.sourcecode.lang.thrift.ThriftExamples.*;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

class ThriftAnalyzerTest {

    @Test
    void cleanForLinesOfCodeCalculations() {
        ThriftAnalyzer analyzer = new ThriftAnalyzer();
        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File(""), EXAMPLE1));
        assertEquals(EXAMPLE1_CLEANED, cleanedContent.getCleanedContent());
    }

    @Test
    void cleanForDuplicationCalculations() {
        ThriftAnalyzer analyzer = new ThriftAnalyzer();
        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File(""), EXAMPLE1));
        assertEquals(EXAMPLE1_CLEANED_DUPLICATION, cleanedContent.getCleanedContent());
    }
}