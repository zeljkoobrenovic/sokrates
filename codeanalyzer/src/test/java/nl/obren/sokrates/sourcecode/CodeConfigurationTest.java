/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
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

    /**
     * Every scope name resolves to its own aspect.
     *
     * <p>Asserted by object identity against {@code getMain()} as well as by name, because the failure
     * this guards returned a real aspect - main - rather than null. A name-only assertion would have
     * passed for {@code getScope("main")} while the other two were quietly answering with it.
     */
    @Test
    public void getScopeReturnsTheAspectEachNameRefersTo() {
        CodeConfiguration codeConfiguration = CodeConfiguration.getDefaultConfiguration();

        assertSame(codeConfiguration.getMain(), codeConfiguration.getScope("main"));
        assertSame(codeConfiguration.getTest(), codeConfiguration.getScope("test"));
        assertSame(codeConfiguration.getGenerated(), codeConfiguration.getScope("generated"));
        assertSame(codeConfiguration.getBuildAndDeployment(), codeConfiguration.getScope("buildAndDeployment"));
        assertSame(codeConfiguration.getOther(), codeConfiguration.getScope("other"));
    }

    /**
     * The name is matched case-insensitively, which is why the labels it is matched against have to be
     * lower case. {@code "buildAndDeployment"} is the spelling the configuration documentation uses,
     * so it is the one that has to work.
     */
    @Test
    public void getScopeIgnoresTheCaseOfTheName() {
        CodeConfiguration codeConfiguration = CodeConfiguration.getDefaultConfiguration();

        assertSame(codeConfiguration.getBuildAndDeployment(), codeConfiguration.getScope("buildAndDeployment"));
        assertSame(codeConfiguration.getBuildAndDeployment(), codeConfiguration.getScope("BUILDANDDEPLOYMENT"));
        assertSame(codeConfiguration.getGenerated(), codeConfiguration.getScope("GeNeRaTeD"));
    }

    /**
     * The end-to-end consequence, and the reason this is not a latent defect: a logical decomposition
     * reads its files through {@code getScope(scope)}, so a decomposition scoped to {@code generated}
     * used to decompose main instead — including the main files and excluding the generated ones,
     * exactly inverted.
     */
    @Test
    public void aLogicalDecompositionScopedToGeneratedDecomposesGeneratedFiles() {
        SourceFile mainFile = new SourceFile(new File("src/main/java/App.java"));
        SourceFile generatedFile = new SourceFile(new File("target/generated/Stub.java"));

        CodeConfiguration codeConfiguration = CodeConfiguration.getDefaultConfiguration();
        codeConfiguration.getMain().setSourceFiles(new ArrayList<>(Arrays.asList(mainFile)));
        codeConfiguration.getGenerated().setSourceFiles(new ArrayList<>(Arrays.asList(generatedFile)));

        LogicalDecomposition decomposition = new LogicalDecomposition();
        decomposition.setName("by scope");
        decomposition.setScope("generated");
        decomposition.setComponentsFolderDepth(0);
        decomposition.setComponents(new ArrayList<>());

        decomposition.updateLogicalComponentsFiles(new SourceCodeFiles(), codeConfiguration, new File("."));

        assertTrue(decomposition.isInScope(generatedFile));
        assertFalse(decomposition.isInScope(mainFile));
    }

}
