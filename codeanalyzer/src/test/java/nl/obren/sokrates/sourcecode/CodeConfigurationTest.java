/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.*;

public class CodeConfigurationTest {
    @Test
    public void getDefaultConfiguration() throws Exception {
        CodeConfiguration codeConfiguration = CodeConfiguration.getDefaultConfiguration();

        assertNotNull(codeConfiguration);
        assertNotNull(codeConfiguration.getMain());
        assertNotNull(codeConfiguration.getTest());
        assertNotNull(codeConfiguration.getGenerated());
        assertNotNull(codeConfiguration.getBuildAndDeployment());
        assertNotNull(codeConfiguration.getOther());
        assertTrue(codeConfiguration.getLogicalDecompositions().size() > 0);
    }

    @Test
    public void getScopesWithExtensions() throws Exception {
        List<SourceFile> sourceFiles = Arrays.asList(new SourceFile(new File("file1.java")), new SourceFile(new File("file2.java")),
                new SourceFile(new File("file3.js")), new SourceFile(new File("file4.html")));
        CodeConfiguration codeConfiguration = CodeConfiguration.getDefaultConfiguration();

        codeConfiguration.getMain().setSourceFiles(sourceFiles);
        List<NamedSourceCodeAspect> scopesWithExtensions = codeConfiguration.getScopesWithExtensions();

        assertEquals(scopesWithExtensions.size(), 8);
        assertEquals(scopesWithExtensions.get(0).getName(), "main");
        assertEquals(scopesWithExtensions.get(1).getName(), "  *.java");
        assertEquals(scopesWithExtensions.get(2).getName(), "  *.js");
        assertEquals(scopesWithExtensions.get(3).getName(), "  *.html");
        assertEquals(scopesWithExtensions.get(4).getName(), "test");
        assertEquals(scopesWithExtensions.get(5).getName(), "generated");
        assertEquals(scopesWithExtensions.get(6).getName(), "build and deployment");
        assertEquals(scopesWithExtensions.get(7).getName(), "other");
    }

    @Test
    public void setMain() throws Exception {
        CodeConfiguration codeConfiguration = CodeConfiguration.getDefaultConfiguration();

        codeConfiguration.setMain(null);
        assertNotNull(codeConfiguration.getMain());
    }

    @Test
    public void setLogicalDecompositions() throws Exception {
        CodeConfiguration codeConfiguration = CodeConfiguration.getDefaultConfiguration();

        codeConfiguration.setLogicalDecompositions(null);
        assertNotNull(codeConfiguration.getLogicalDecompositions());
    }

}
