package nl.obren.sokrates.sourcecode.lang.plsql;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * A package body belongs to the package it implements. The declaration is read by string search,
 * and the name has to be taken from after the BODY keyword rather than from it: every body in the
 * codebase otherwise anchors under the same name, and a node that is not a package collects their
 * dependencies.
 *
 * <p>The file names here deliberately differ from the package names, so that the anchor under test
 * is the one taken from the declaration and not the one taken from the file name.
 */
public class PlSqlPackageBodyAnchorTest {

    private static final String SPEC =
            "CREATE OR REPLACE PACKAGE pkg_a AS\n" +
            "   PROCEDURE run;\n" +
            "END pkg_a;\n";

    private static final String BODY =
            "CREATE OR REPLACE PACKAGE BODY pkg_a AS\n" +
            "   PROCEDURE run IS\n" +
            "   BEGIN\n" +
            "      NULL;\n" +
            "   END run;\n" +
            "END pkg_a;\n";

    private List<String> anchorNames(SourceFile... files) {
        return new PlSqlHeuristicDependenciesExtractor()
                .getDependencyAnchors(Arrays.asList(files)).stream()
                .map(DependencyAnchor::getAnchor)
                .sorted()
                .collect(Collectors.toList());
    }

    private SourceFile file(String name, String content) {
        return new SourceFile(new File(name), content);
    }

    @Test
    public void aPackageBodyAnchorsUnderThePackageName() {
        assertEquals(Arrays.asList("pkg_a"), anchorNames(file("a_body.pkb", BODY)));
    }

    /**
     * The defect this pins: with the name taken from the BODY keyword, both files anchor under
     * "BODY" and merge into one node, so a codebase that keeps bodies in their own files loses
     * every package body as a distinct component.
     */
    @Test
    public void twoPackageBodiesDoNotMergeIntoOneAnchor() {
        assertEquals(Arrays.asList("pkg_a", "pkg_b"), anchorNames(
                file("a_body.pkb", BODY),
                file("b_body.pkb", BODY.replace("pkg_a", "pkg_b"))));
    }

    @Test
    public void theSpecAndTheBodyShareOneAnchor() {
        List<DependencyAnchor> anchors = new PlSqlHeuristicDependenciesExtractor()
                .getDependencyAnchors(Arrays.asList(file("a_spec.pks", SPEC), file("a_body.pkb", BODY)));

        assertEquals(1, anchors.size());
        assertEquals("pkg_a", anchors.get(0).getAnchor());
        assertEquals(2, anchors.get(0).getSourceFiles().size());
    }

    /**
     * Real PL/SQL puts the AS or IS on the next line as often as not. The name ends at the first
     * whitespace, not at the first space, or it runs on into the rest of the declaration.
     */
    @Test
    public void aNameEndingAtALineBreakIsReadWhole() {
        assertEquals(Arrays.asList("pkg_a"), anchorNames(
                file("a_body.pkb", "create or replace package body pkg_a\nas\n   null;\nend pkg_a;\n")));
    }

    @Test
    public void theBodyKeywordIsReadInEitherCase() {
        assertEquals(Arrays.asList("pkg_a"), anchorNames(file("a_body.pkb", BODY)));
        assertEquals(Arrays.asList("pkg_a"), anchorNames(file("a_body.pkb", BODY.toLowerCase())));
    }

    /**
     * A declaration head may be wrapped anywhere it has whitespace, the keyword included.
     */
    @Test
    public void aNameOnTheLineAfterTheKeywordIsRead() {
        assertEquals(Arrays.asList("pkg_a"), anchorNames(
                file("a_body.pkb", "CREATE OR REPLACE PACKAGE BODY\n   pkg_a AS\n   NULL;\nEND pkg_a;\n")));
    }

    /**
     * An unnamed anchor is worse than a wrongly named one: its dependency pattern is
     * {@code \s*[.]+\S+}, which matches any dotted token, so one such declaration invents edges
     * from most of the codebase. Runs of whitespace around the name must not produce one.
     */
    @Test
    public void aRunOfSpacesDoesNotProduceAnUnnamedAnchor() {
        assertEquals(Arrays.asList("pkg_a"), anchorNames(
                file("a_body.pkb", "CREATE OR REPLACE PACKAGE BODY  pkg_a AS\n   NULL;\nEND pkg_a;\n")));
        assertEquals(Arrays.asList("pkg_a"), anchorNames(
                file("a_spec.pks", "CREATE OR REPLACE PACKAGE  pkg_a AS\n   NULL;\nEND pkg_a;\n")));
    }

    /**
     * Only the keyword is skipped, not a name that begins with it: the token has to be BODY
     * followed by whitespace, or a package called body_log would be anchored as "_log".
     */
    @Test
    public void aPackageNamedAfterTheKeywordKeepsItsName() {
        assertEquals(Arrays.asList("body_log"), anchorNames(
                file("body_log.pks", "CREATE OR REPLACE PACKAGE body_log AS\n   PROCEDURE run;\nEND body_log;\n")));
    }
}
