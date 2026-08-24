package nl.obren.sokrates.sourcecode.lang.plsql;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A caller of a package whose name is not written in ASCII must produce the same dependency as
 * an otherwise identical ASCII caller. The counts are compared against the ASCII twin rather than
 * against a literal, so the test states the property instead of the current numbers.
 */
public class PlSqlDependenciesWithNonAsciiNamesTest {

    private static final String ASCII_PACKAGE = "paket";
    private static final String ASCII_PROCEDURE = "funktion";

    private String packageSource(String packageName, String procedureName) {
        return "CREATE OR REPLACE PACKAGE " + packageName + " AS\n" +
                "   PROCEDURE " + procedureName + ";\n" +
                "END " + packageName + ";\n";
    }

    private String callerSource(String packageName, String procedureName) {
        return "DECLARE\n" +
                "   code number := 8;\n" +
                "BEGIN\n" +
                "   " + packageName + "." + procedureName + ";\n" +
                "END;";
    }

    private List<Dependency> dependencies(String packageName, String procedureName) {
        SourceFile declaration = new SourceFile(new File("package.pls"), packageSource(packageName, procedureName));
        SourceFile caller = new SourceFile(new File("caller.pls"), callerSource(packageName, procedureName));

        return new PlSqlAnalyzer()
                .extractDependencies(Arrays.asList(declaration, caller), new ProgressFeedback())
                .getDependencies();
    }

    private void assertSameDependencyAsAsciiTwin(String packageName, String procedureName) {
        List<Dependency> ascii = dependencies(ASCII_PACKAGE, ASCII_PROCEDURE);
        List<Dependency> nonAscii = dependencies(packageName, procedureName);

        assertEquals(1, ascii.size());
        assertEquals("caller -> " + ASCII_PACKAGE, ascii.get(0).getDependencyString());

        assertEquals(ascii.size(), nonAscii.size());
        assertEquals("caller -> " + packageName, nonAscii.get(0).getDependencyString());
    }

    @Test
    public void cyrillicPackageName() {
        assertSameDependencyAsAsciiTwin("пакет", "функция");
    }

    @Test
    public void greekPackageName() {
        assertSameDependencyAsAsciiTwin("πακέτο", "λειτουργία");
    }

    /**
     * A name written in the Latin alphabet is only lost when a non-ASCII letter sits on both sides
     * of the dot, because an ASCII letter anywhere next to it satisfies the old pattern by itself.
     */
    @Test
    public void germanPackageNameEndingInANonAsciiLetter() {
        assertSameDependencyAsAsciiTwin("maß", "änderung");
    }

    @Test
    public void nordicPackageNameEndingInANonAsciiLetter() {
        assertSameDependencyAsAsciiTwin("verdi_blå", "økning");
    }

    /**
     * {@code \w} admitted digits, so the widened class has to as well. A package named
     * {@code paket2} is ordinary in PL/SQL, and losing it would be a regression against ASCII
     * input rather than a gap left in it.
     */
    @Test
    public void aDigitInThePackageNameIsStillAllowed() {
        List<Dependency> dependencies = dependencies("paket2", "funktion");

        assertEquals(1, dependencies.size());
        assertEquals("caller -> paket2", dependencies.get(0).getDependencyString());
    }

    /**
     * An accented letter may be stored decomposed, as a base letter followed by a combining mark.
     * A class of letters alone breaks off at the mark, so the mark has to be admitted in the tail.
     */
    @Test
    public void decomposedAccentBehavesLikeAPrecomposedOne() {
        List<Dependency> precomposed = dependencies("paketé", "funktioné");
        List<Dependency> decomposed = dependencies("pakete\u0301", "funktione\u0301");

        assertEquals(1, precomposed.size());
        assertEquals(precomposed.size(), decomposed.size());
        assertEquals("caller -> pakete\u0301", decomposed.get(0).getDependencyString());
    }

    /**
     * The same must hold on the far side of the dot. A class of letters alone ends the member name
     * at the mark and then reads the rest of it as a second, invented reference: the decomposed
     * form of {@code foo.béz.qux} yields the packages {@code foo} and {@code z} rather than only
     * {@code foo}, and the file is given an anchor for each.
     */
    @Test
    public void aDecomposedAccentInAMemberNameDoesNotSplitTheReference() {
        assertEquals(anchorsOf("   foo.béz.qux;\n").size(),
                anchorsOf("   foo.be\u0301z.qux;\n").size());
    }

    private List<DependencyAnchor> anchorsOf(String bodyLine) {
        SourceFile caller = new SourceFile(new File("caller.pls"),
                "DECLARE\n" +
                        "   code number := 8;\n" +
                        "BEGIN\n" +
                        bodyLine +
                        "END;");

        return new PlSqlHeuristicDependenciesExtractor().extractDependencyAnchors(caller);
    }

    /**
     * A combining mark that begins a token is garbled input rather than a name, so it must not be
     * read as a qualified reference and turn the file into a dependency source.
     */
    @Test
    public void strayCombiningMarkIsNotAQualifiedReference() {
        SourceFile caller = new SourceFile(new File("caller.pls"),
                "DECLARE\n" +
                        "   code number := 8;\n" +
                        "BEGIN\n" +
                        "   \u0301.funktion;\n" +
                        "END;");

        List<DependencyAnchor> anchors = new PlSqlHeuristicDependenciesExtractor()
                .extractDependencyAnchors(caller);

        assertTrue(anchors.isEmpty());
    }
}
