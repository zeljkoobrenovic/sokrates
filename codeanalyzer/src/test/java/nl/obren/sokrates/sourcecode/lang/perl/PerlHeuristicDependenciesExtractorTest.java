/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.perl;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;
import nl.obren.sokrates.sourcecode.dependencies.DependenciesUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PerlHeuristicDependenciesExtractorTest {
    @Test
    public void extractDependencyAnchors() throws Exception {
        PerlHeuristicDependenciesExtractor parser = new PerlHeuristicDependenciesExtractor();
        ArrayList<SourceFile> sourceFiles = new ArrayList<>();

        sourceFiles.add(new SourceFile(new File("file1.pm"), "package A::B::C; {}"));
        sourceFiles.add(new SourceFile(new File("file2.pm"), "package A::B::C; {}"));
        sourceFiles.add(new SourceFile(new File("file3.pm"), "package D::E 1.02;\npackage D::E::Helper;\n"));
        sourceFiles.add(new SourceFile(new File("script.pl"), "use A::B::C;\n"));

        List<DependencyAnchor> dependencyAnchors = parser.getDependencyAnchors(sourceFiles);

        assertEquals(3, dependencyAnchors.size());

        assertEquals("A::B::C", dependencyAnchors.get(0).getAnchor());
        assertEquals(2, dependencyAnchors.get(0).getSourceFiles().size());

        assertEquals("D::E", dependencyAnchors.get(1).getAnchor());
        assertEquals("package D::E 1.02;", dependencyAnchors.get(1).getCodeFragment());
        assertTrue(dependencyAnchors.get(1).matchesLine("use D::E::Helper;"));

        // a script is a source, anchored by its path, but never a target
        assertEquals("script.pl", dependencyAnchors.get(2).getAnchor());
        assertEquals(0, dependencyAnchors.get(2).getDependencyPatterns().size());
    }

    @Test
    public void matchesOnlyTheFullPackageName() {
        DependencyAnchor anchor = anchorFor("package App::Model;");

        assertTrue(anchor.matchesLine("use App::Model;"));
        assertTrue(anchor.matchesLine("  use App::Model qw(load);"));
        assertTrue(anchor.matchesLine("\trequire App::Model;"));
        assertTrue(anchor.matchesLine("eval { require App::Model; 1 } or die;"));
        assertTrue(anchor.matchesLine("eval \"use App::Model; 1\";"));
        assertTrue(anchor.matchesLine("use parent 'App::Model';"));
        assertTrue(anchor.matchesLine("use parent -norequire, \"App::Model\";"));
        assertTrue(anchor.matchesLine("use base qw(Exporter App::Model);"));
        assertTrue(anchor.matchesLine("use base qw( App::Model );"));

        assertTrue(!anchor.matchesLine("use App::Model::User;"));
        assertTrue(!anchor.matchesLine("use App;"));
        assertTrue(!anchor.matchesLine("use App::ModelX;"));
        assertTrue(!anchor.matchesLine("# use App::Model;"));
        assertTrue(!anchor.matchesLine("my $x = App::Model->new; # see App::Model"));
        assertTrue(!anchor.matchesLine("use parent 'App::Model::Base';"));
    }

    @Test
    public void ignoresPodAndDataSections() {
        String content = "package App::Doc;\n\n=head1 SYNOPSIS\n\n  use App::Other;\n\n=cut\n\nsub x { 1 }\n1;\n__END__\nuse App::Other;\n";
        String code = PerlHeuristicDependenciesExtractor.codeOnly(content);
        assertTrue(!code.contains("use App::Other"));
        assertTrue(code.contains("sub x { 1 }"));
    }

    /** The example of issue #132: 5 real dependencies, and none from a comment. */
    @Test
    public void issue132Example() {
        List<SourceFile> files = new ArrayList<>();
        files.add(file("lib/App/Util.pm", "package App::Util;\n\nsub trim {\n  my ($text) = @_;\n  $text =~ s/^\\s+|\\s+$//g;\n  return $text;\n}\n\n1;\n"));
        files.add(file("lib/App/Model/Base.pm", "package App::Model::Base;\n\nsub new {\n  my ($class, %args) = @_;\n  return bless {%args}, $class;\n}\n\n1;\n"));
        files.add(file("lib/App/Model/User.pm", "package App::Model::User;\nuse parent 'App::Model::Base';\n\nsub name {\n  my ($self) = @_;\n  require App::Util;\n  return App::Util::trim($self->{name});\n}\n\n1;\n"));
        files.add(file("lib/App/Report.pm", "package App::Report;\nuse App::Util;\n\n# Unlike App::Model::User, this module holds no state.\nsub render {\n  return join(', ', map { App::Util::trim($_) } @_);\n}\n\n1;\n"));
        files.add(file("report.pl", "use lib 'lib';\nuse App::Report;\nuse App::Model::User;\n\nprint App::Report::render(App::Model::User->new(name => ' Ann ')->name), \"\\n\";\n"));

        NamedSourceCodeAspect aspect = new NamedSourceCodeAspect("main");
        aspect.setSourceFiles(files);

        // through DependenciesUtils, so .pl and .pm files are analysed together
        List<Dependency> dependencies = DependenciesUtils.extractDependencies(aspect, false).getDependencies();

        Set<String> found = new TreeSet<>();
        dependencies.forEach(d -> found.add(d.getFrom().getAnchor() + " -> " + d.getTo().getAnchor()));

        Set<String> expected = new TreeSet<>(List.of(
                "report.pl -> App::Report",
                "report.pl -> App::Model::User",
                "App::Model::User -> App::Model::Base",
                "App::Model::User -> App::Util",
                "App::Report -> App::Util"));
        assertEquals(expected, found);
    }

    private static SourceFile file(String path, String content) {
        SourceFile sourceFile = new SourceFile(new File(path), content);
        sourceFile.setRelativePath(path);
        return sourceFile;
    }

    private static DependencyAnchor anchorFor(String content) {
        return new PerlHeuristicDependenciesExtractor().extractDependencyAnchors(new SourceFile(new File("a.pm"), content)).get(0);
    }
}
