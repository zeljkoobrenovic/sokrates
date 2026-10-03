/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.scoping;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

// based on:
// - https://github.com/github/linguist/blob/master/lib/linguist/generated.rb
// - https://raw.githubusercontent.com/github/linguist/master/lib/linguist/documentation.yml
// - https://github.com/github/linguist/blob/master/lib/linguist/languages.yml
// - https://github.com/github/linguist/blob/master/lib/linguist/vendor.yml
public class ScopingConventions {
    private static final Log LOG = LogFactory.getLog(ScopingConventions.class);

    private List<Convention> ignoredFilesConventions = new ArrayList<>();
    private List<Convention> testFilesConventions = new ArrayList<>();
    private List<Convention> generatedFilesConventions = new ArrayList<>();
    private List<Convention> buildAndDeploymentFilesConventions = new ArrayList<>();
    private List<Convention> otherFilesConventions = new ArrayList<>();

    public ScopingConventions() {
        IgnoreConventions.addTo(ignoredFilesConventions);
        addTestConventions();
        addGeneratedConventions();
        BuildAndDeploymentConventions.addTo(buildAndDeploymentFilesConventions);
        addOtherConventions();
    }

    public static void main(String args[]) {
        printText("ignore files with:", new ScopingConventions().ignoredFilesConventions, "  - ");
        printText("add to the test scope files with:", new ScopingConventions().testFilesConventions, "   - ");
        printText("add to the generated scope files with:", new ScopingConventions().generatedFilesConventions, "   - ");
        printText("add to the build-and-deploy scope files with:", new ScopingConventions().buildAndDeploymentFilesConventions, "   - ");
        printText("add to the other scope files with:", new ScopingConventions().otherFilesConventions, "   - ");
    }

    private static void printText(String s, List<Convention> ignoredFilesConventions, String s2) {
        LOG.info(s);
        ignoredFilesConventions.forEach(convention -> {
            LOG.info(s2 + convention.toString() + " (" + convention.getNote() + ")");
        });
    }

    /**
     * What Sokrates itself writes next to the code: the analysis folder, a landscape folder, the git
     * history exports and the conventions/configuration files. Never source code, so these are
     * ignored unconditionally (see {@link #ensureSokratesOutputIgnored}) — the other conventions are
     * only written into a configuration when a file matches them at init, and the analysis output
     * does not exist yet on the first run, which used to put the first run's reports into the
     * second run's main scope.
     */
    public static final List<Convention> SOKRATES_OUTPUT_CONVENTIONS = List.of(
            new Convention(".*/_sokrates/.*", "", "Sokrates files"),
            new Convention(".*/_sokrates_landscape/.*", "", "Sokrates landscape files"),
            new Convention(".*/git[-][a-zA-Z0-9_]+[.]txt", "", "Git data exports for sokrates analyses"),
            new Convention(".*/sokrates_.*?[.]json", "", "Sokrates conventions and configurations"));

    /** Adds the {@link #SOKRATES_OUTPUT_CONVENTIONS} missing from {@code ignore} (matched by path and content pattern). */
    public static void ensureSokratesOutputIgnored(List<SourceFileFilter> ignore) {
        for (Convention convention : SOKRATES_OUTPUT_CONVENTIONS) {
            boolean present = ignore.stream().anyMatch(filter -> convention.getPathPattern().equals(filter.getPathPattern())
                    && convention.getContentPattern().equals(filter.getContentPattern()));
            if (!present) {
                ignore.add(convention);
            }
        }
    }

    public void addConventions(CodeConfiguration codeConfiguration, List<SourceFile> sourceFiles) {
        LOG.info("Adding ignore conventions:");
        ConventionUtils.addConventions(ignoredFilesConventions, codeConfiguration.getIgnore(), sourceFiles);
        ensureSokratesOutputIgnored(codeConfiguration.getIgnore());
        LOG.info("Adding test files conventions:");
        ConventionUtils.addConventions(testFilesConventions, codeConfiguration.getTest().getSourceFileFilters(), sourceFiles);
        LOG.info("Adding generated files conventions:");
        ConventionUtils.addConventions(generatedFilesConventions, codeConfiguration.getGenerated().getSourceFileFilters(), sourceFiles);
        LOG.info("Adding build & deployment conventions:");
        ConventionUtils.addConventions(buildAndDeploymentFilesConventions, codeConfiguration.getBuildAndDeployment().getSourceFileFilters(), sourceFiles);
        LOG.info("Adding other files conventions:");
        ConventionUtils.addConventions(otherFilesConventions, codeConfiguration.getOther().getSourceFileFilters(), sourceFiles);
    }

    private void addOtherConventions() {
        // static code analysis configurations
        otherFilesConventions.add(new Convention(".*/vendor/.*", "", "Vendor files"));

        otherFilesConventions.add(new Convention(".*[.]md", "", "Markdown files"));
        otherFilesConventions.add(new Convention(".*[.]markdown", "", "Markdown files"));
        otherFilesConventions.add(new Convention(".*[.]mdown", "", "Markdown files"));
        otherFilesConventions.add(new Convention(".*[.]mdwn", "", "Markdown files"));
        otherFilesConventions.add(new Convention(".*[.]mdx", "", "Markdown files"));
        otherFilesConventions.add(new Convention(".*[.]mkd", "", "Markdown files"));
        otherFilesConventions.add(new Convention(".*[.]mkdn", "", "Markdown files"));
        otherFilesConventions.add(new Convention(".*[.]mkdown", "", "Markdown files"));

        otherFilesConventions.add(new Convention(".*[.]adoc", "", "AsciiDoc documentation"));

        otherFilesConventions.add(new Convention(".*[.](rst|rest|resttxt|rsttxt)", "", "reST files"));

        otherFilesConventions.add(new Convention(".*[.]ronn", "", "Markdown files"));
        otherFilesConventions.add(new Convention(".*[.]workbook", "", "Markdown files"));
        otherFilesConventions.add(new Convention(".*[.]plist", "", "Property list files"));

        otherFilesConventions.add(new Convention(".*[.]json", "", "JSON files"));

        otherFilesConventions.add(new Convention(".*[.]svg", "", "SVG files"));

        otherFilesConventions.add(new Convention(".*[.]storyboard", "", "Storyboard"));
        otherFilesConventions.add(new Convention(".*[.]xib", "", "XIB files"));

        // config
        otherFilesConventions.add(new Convention(".*[.]apacheconf", "", "Configuration"));
        otherFilesConventions.add(new Convention(".*[.]vhost", "", "Configuration"));
        otherFilesConventions.add(new Convention(".*/[.]htaccess", "", "Configuration"));
        otherFilesConventions.add(new Convention(".*[.]csf", "", "Configuration"));
        otherFilesConventions.add(new Convention(".*[.]diff", "", "Configuration"));
        otherFilesConventions.add(new Convention(".*[.]patch", "", "Configuration"));

        otherFilesConventions.add(new Convention(".*[.]properties", "", "Properties"));
        otherFilesConventions.add(new Convention(".*[.]po", "", "Properties"));

        otherFilesConventions.add(new Convention(".*[.]dsp", "", "Microsoft Developer Studio repository"));

        otherFilesConventions.add(new Convention(".*[.]txi", "", "Textinfo"));
        otherFilesConventions.add(new Convention(".*[.]texi", "", "Textinfo"));
        otherFilesConventions.add(new Convention(".*[.]texinfo", "", "Textinfo"));

        otherFilesConventions.add(new Convention(".*[.]txt", "", "Text files"));
        otherFilesConventions.add(new Convention(".*[.]fr", "", "Text files"));
        otherFilesConventions.add(new Convention(".*[.]nb", "", "Text files"));
        otherFilesConventions.add(new Convention(".*[.]ncl", "", "Text files"));
        otherFilesConventions.add(new Convention(".*[.]no", "", "Text files"));

        otherFilesConventions.add(new Convention(".*/COPYING", "", "Text files"));
        otherFilesConventions.add(new Convention(".*/COPYING[.][a-z0-9]+", "", "Text files"));
        otherFilesConventions.add(new Convention(".*/COPYRIGHT", "", "Text files"));
        otherFilesConventions.add(new Convention(".*/COPYRIGHT[.][a-z0-9]+", "", "Text files"));
        otherFilesConventions.add(new Convention(".*/FONTLOG", "", "Text files"));
        otherFilesConventions.add(new Convention(".*/INSTALL", "", "Text files"));
        otherFilesConventions.add(new Convention(".*/INSTALL[.][a-z0-9]+", "", "Text files"));
        otherFilesConventions.add(new Convention(".*/LICENSE", "", "Text files"));
        otherFilesConventions.add(new Convention(".*/LICENSE[.][a-z0-9]+", "", "Text files"));
        otherFilesConventions.add(new Convention(".*/NEWS", "", "Text files"));
        otherFilesConventions.add(new Convention(".*/README", "", "Text files"));
        otherFilesConventions.add(new Convention(".*/README[.][a-z0-9]+", "", "Text files"));

        otherFilesConventions.add(new Convention(".*/CHANGE(S|LOG)?(\\.|)", "", "Documentation"));
        otherFilesConventions.add(new Convention(".*/CONTRIBUTING(\\.|)", "", "Documentation"));
        otherFilesConventions.add(new Convention(".*/COPYING(\\.|)", "", "Documentation"));
        otherFilesConventions.add(new Convention(".*/INSTALL(\\.|)", "", "Documentation"));
        otherFilesConventions.add(new Convention(".*/LICEN[CS]E(\\.|)", "", "Documentation"));
        otherFilesConventions.add(new Convention(".*/[Ll]icen[cs]e(\\.|)", "", "Documentation"));
        otherFilesConventions.add(new Convention(".*/README(\\.|)", "", "Documentation"));
        otherFilesConventions.add(new Convention(".*/[Rr]eadme(\\.|)", "", "Documentation"));

        otherFilesConventions.add(new Convention(".*/click[.]me", "", "Text files"));
        otherFilesConventions.add(new Convention(".*/delete[.]me", "", "Text files"));
        otherFilesConventions.add(new Convention(".*/keep[.]me", "", "Text files"));
        otherFilesConventions.add(new Convention(".*/read[.]me", "", "Text files"));
        otherFilesConventions.add(new Convention(".*/test[.]me", "", "Text files"));
        // go.mod / go.sum are classified as build-and-deployment (see addBuildAndDeploymentConventions).
        otherFilesConventions.add(new Convention(".*/package[.]mask", "", "Text files"));
        otherFilesConventions.add(new Convention(".*/package[.]use[.]mask", "", "Text files"));
        otherFilesConventions.add(new Convention(".*/package[.]use[.]stable[.]mask", "", "Text files"));
        otherFilesConventions.add(new Convention(".*/readme[.]1st", "", "Text files"));
        otherFilesConventions.add(new Convention(".*/use[.]mask", "", "Text files"));
        otherFilesConventions.add(new Convention(".*/use[.]stable[.]mask", "", "Text files"));

        otherFilesConventions.add(new Convention(".*[.]indent[.]pro", "", "Text files"));

        otherFilesConventions.add(new Convention(".*[.]lock", "", "Locked files"));

        otherFilesConventions.add(new Convention(".*[.]scm", "", "SCM files"));

        otherFilesConventions.add(new Convention(".*/[Dd]ocumentation/.*", "", "Documentation"));
        otherFilesConventions.add(new Convention(".*/asciidoc/.*", "", "Documentation"));
        otherFilesConventions.add(new Convention(".*/[Mm]an/.*", "", "Documentation"));
        otherFilesConventions.add(new Convention(".*/[Ee]xamples/.*", "", "Documentation"));
        otherFilesConventions.add(new Convention(".*/[Ss]amples/.*", "", "Samples"));
        otherFilesConventions.add(new Convention(".*/[Dd]emos?/.*", "", "Documentation"));
        otherFilesConventions.add(new Convention(".*[.]3pm", "", "Manual pages"));
        otherFilesConventions.add(new Convention(".*[.]vim", "", "vim editor config"));

        otherFilesConventions.add(new Convention(".*[.]_js", "", ""));
        otherFilesConventions.add(new Convention(".*[.]sublime-project", "", ""));
        otherFilesConventions.add(new Convention(".*[.]ini", "", "INI files"));
        otherFilesConventions.add(new Convention(".*[.]libsonnet", "", "Libsonnet files"));
        otherFilesConventions.add(new Convention(".*[.]tab", "", "Table files"));
        otherFilesConventions.add(new Convention(".*[.]xmi", "", "XMI files"));

        otherFilesConventions.add(new Convention(".*changers[.]xml", "", "Changes documentation"));
        otherFilesConventions.add(new Convention(".*/resources/.*[.]xsd", "", "XSD files"));
        otherFilesConventions.add(new Convention(".*/wp[-]includes/.*", "", "WordPress includes"));
        otherFilesConventions.add(new Convention(".*/changes[.]xml", "", "Changes log"));

        otherFilesConventions.add(new Convention(".*[.]pb", "", "Protocol buffer (protobuf) files"));
        otherFilesConventions.add(new Convention(".*[.]obj", "", "Geometry definition files"));
        otherFilesConventions.add(new Convention(".*[.]mtl", "", "Material Template Library files"));
        otherFilesConventions.add(new Convention(".*[.]urdf", "", "URDF files"));

        otherFilesConventions.add(new Convention(".*/site[-]packages/.*", "", "3rd party libraries and artifacts"));
    }

    private void addGeneratedConventions() {
        String defaultNote = "Generated files";
        generatedFilesConventions.add(new Convention(".*/generated/.*", "", defaultNote));
        generatedFilesConventions.add(new Convention(".*/gen-code/.*", "", defaultNote));
        generatedFilesConventions.add(new Convention(".*/_generated_/.*", "", defaultNote));
        generatedFilesConventions.add(new Convention(".*/_generated/.*", "", defaultNote));
        generatedFilesConventions.add(new Convention(".*/__generated__/.*", "", defaultNote));
        generatedFilesConventions.add(new Convention(".*/gen/.*[.]go", "", "Generated Go files"));
        generatedFilesConventions.add(new Convention(".*[.]generated[.][a-zA-Z]+", "", "Generated files"));
        generatedFilesConventions.add(new Convention("", "//[ ]*Generated by .*", "Generated files"));
        generatedFilesConventions.add(new Convention("", "// Generated using .*", "Generated files"));
        generatedFilesConventions.add(new Convention("", "// This file was generated .*", "Generated files"));
        generatedFilesConventions.add(new Convention("", "This is auto[-]generated with .*", "Generated files"));
        generatedFilesConventions.add(new Convention("", "#[ ]*WARNING[ ]*[:][ ]*This file is generated.*", "Generated files"));
        generatedFilesConventions.add(new Convention("", "\\<\\!\\-\\-[ ]*Generated by .*", "Generated files"));

        generatedFilesConventions.add(new Convention(".*/npm[-]shrinkwrap[.]json", "", "A generated npm shrinkwrap file"));
        generatedFilesConventions.add(new Convention(".*/package[-]lock[.]json", "", "A generated npm package lock file"));

        generatedFilesConventions.add(new Convention(".*/.*[.]nib", "", "Xcode generated files"));
        generatedFilesConventions.add(new Convention(".*/.*[.]xcworkspacedata", "", "Xcode generated files"));
        generatedFilesConventions.add(new Convention(".*/.*[.]xcuserstate", "", "Xcode generated files"));

        generatedFilesConventions.add(new Convention(".*/Pods/.*", "", "Cocoa pods"));
        generatedFilesConventions.add(new Convention(".*/Carthage/Build/.*", "", "Carthage builds"));
        generatedFilesConventions.add(new Convention(".*/[.](css|js)[.]map", "", "JS/CSS map"));

        generatedFilesConventions.add(new Convention(".*[.]js", "[/][/] Generated by .*", 1, "JS generated"));
        generatedFilesConventions.add(new Convention(".*[.]xml", "[<]doc[>]", 2, "A generated documentation file for a .NET assembly"));
        generatedFilesConventions.add(new Convention(".*[.]designer[.](cs|vb)", "", "A codegen file for a .NET repository"));
        generatedFilesConventions.add(new Convention(".*[.]feature[.]cs", "", "A codegen file for Specflow feature file"));
        generatedFilesConventions.add(new Convention(".*[.]feature[.]cs", ".*Generated by PEG[.]js.*", 5, "A parser generated by PEG.js"));
        generatedFilesConventions.add(new Convention(".*[.](c|cpp)", ".*Generated by Cython.*", 1, "A compiled C/C++ file from Cython"));
        generatedFilesConventions.add(new Convention(".*[.](cpp|hpp|h|cc)", ".*[/][/] Generated by the gRPC.*", 1, "A protobuf/grpc-generated C++ file"));
        generatedFilesConventions.add(new Convention(".*[.](c|h)", ".*GIMP header image file format .*", 1, "A generated GIMP C image file"));
        generatedFilesConventions.add(new Convention(".*[.](c|h)", ".*GIMP .* C[-]Source image dump.*", 1, "A generated GIMP C image file"));
        generatedFilesConventions.add(new Convention(".*[.]dsp", ".*[#] Microsoft Developer Studio Generated Build File.*", 4, "A generated Microsoft Visual Studio 6.0 build file"));
        generatedFilesConventions.add(new Convention(".*[.](ps|eps|pfa)", "", "PostScript generated"));
        generatedFilesConventions.add(new Convention(".*[.](py|java|h|cc|cpp|m|rb|php)", ".*Generated by the protocol buffer compiler[.][ ]+DO NOT EDIT[!].*", 3, "Generated by protocol buffer compiler"));
        generatedFilesConventions.add(new Convention(".*[.](js|py|lua|cpp|h|java|cs|php)", ".*Generated by Haxe.*", 4, "A generated Haxe-generated source file"));
        generatedFilesConventions.add(new Convention(".*[.]js", ".*GENERATED CODE [-][-] DO NOT EDIT[!].*", 3, "Generated by protocol buffer compiler"));
        generatedFilesConventions.add(new Convention(".*[.]h", ".* DO NOT EDIT THIS FILE [-] it is machine generated .*", 6, "A C/C++ header generated by the Java JNI tool javah"));
        generatedFilesConventions.add(new Convention(".*[.]meta", ".*fileFormatVersion[:] .*", 6, "A metadata file from Unity3D"));
        generatedFilesConventions.add(new Convention(".*[.]rb", "# This file is automatically generated by Racc.*", 3, "A a Racc-generated file"));
        generatedFilesConventions.add(new Convention(".*[.]java", ".*The following code was generated by JFlex.*", 3, "A JFlex-generated file"));
        generatedFilesConventions.add(new Convention(".*[.]java", ".*[/][/] This is a generated file[.] Not intended for manual editing[.].*", 1, "A GrammarKit-generated file"));
        generatedFilesConventions.add(new Convention(".*[.]java", ".*This file is generated by jOOQ[.].*", 3, "A generated jOOQ file"));
        generatedFilesConventions.add(new Convention(".*[.]java", ".*// DO NOT EDIT[.] This is code generated.*", 3, "A generated file"));
        generatedFilesConventions.add(new Convention(".*[.]java", "[ ]*[*][ ]*[@]author[ ]+DAOGenerator.*", 3, "A generated Java DAO file"));
        generatedFilesConventions.add(new Convention(".*[.]java", "[/][/][ ]*This file is auto-generated.*", 3, "A generated Java file"));

        generatedFilesConventions.add(new Convention(".*[.]cs", "[/][/][ ]*<auto[-]?generated.*", 3, "A generated C# file"));
        generatedFilesConventions.add(new Convention(".*[.]vb", "[/][/][ ]*<auto[-]?generated.*", 3, "A generated VisualBasic file"));

        generatedFilesConventions.add(new Convention(".*[.](kts?|java|go|py|cs)", "[/][/][ ]*File generated from .*", 3, "A generated file"));

        generatedFilesConventions.add(new Convention(".*[.]js", "[/][*] parser generated by jison .*", 1, "A Jison-generated file"));
        generatedFilesConventions.add(new Convention(".*[.]js", "[/][*] generated by jison[-]lex .*", 1, "A Jison-generated file"));
        generatedFilesConventions.add(new Convention(".*[.]zep[.][a-z0-9_]+", "", ""));
        generatedFilesConventions.add(new Convention(".*[.]dart", ".*GENERATED CODE.*DO NOT MODIFY.*", 1, "A generated Dart file"));
        generatedFilesConventions.add(new Convention(".*[.]h", ".*Automatically created by Devel[:][:]PPPort.*", 9, "A generated Perl/Pollution/Portability header file"));
        generatedFilesConventions.add(new Convention(".*[.](html|htm|xhtml)", ".*Generated by pkgdown[:] do not edit by hand.*", 2, "A generated HTML source file"));
        generatedFilesConventions.add(new Convention(".*[.](html|htm|xhtml)", ".*Generated by Doxygen.*", 31, "A generated HTML source file"));
        generatedFilesConventions.add(new Convention(".*[.](html|htm|xhtml)", "[ ]*[<]meta name[=]\"generator\" .*", 31, "A generated HTML source file"));
        generatedFilesConventions.add(new Convention(".*[.](cxx|cpp|c|hxx|hpp|h)", ".*autogen include statement, do not remove.*", 31, "A generated C(PP) source file"));
        generatedFilesConventions.add(new Convention(".*[.](cxx|cpp|c|hxx|hpp|h)", "[ ]*[*][ ]*Generated automatically by[ ]*.*", 31, "A generated C(PP) source file"));
        generatedFilesConventions.add(new Convention(".*/src/gen/.*", "", defaultNote));
        generatedFilesConventions.add(new Convention(".*_generated[.][a-z]+", "", defaultNote));

        generatedFilesConventions.add(new Convention(".*/zz_generated[.].*[.]go", "", defaultNote));
        generatedFilesConventions.add(new Convention(".*/generated[.].*[.]go", "", defaultNote));
    }

    private void addTestConventions() {
        String defaultNote = "Test files";
        testFilesConventions.add(new Convention(".*/[Tt]est/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/[Tt]ests/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*[.][Tt]est/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*[.][Tt]ests/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*[.][Tt]est[.].*", "", defaultNote));
        testFilesConventions.add(new Convention(".*[.][Tt]ests[.].*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/UnitTests?/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*[.]UnitTests/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*UnitTests[.][a-zA-Z0-9_]+", "", defaultNote));
        testFilesConventions.add(new Convention(".*/IntegrationTests?/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/UITests?/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/src/testPlay/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/Unit Tests/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/src/ciTest/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/src/ciTests/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/src/androidTest/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/src/androidTests/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/[Ss]pecs/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*[-]tests/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/test[-]data/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*_test[.].*", "", defaultNote));
        testFilesConventions.add(new Convention(".*_tests[.].*", "", defaultNote));
        testFilesConventions.add(new Convention(".*[.]test[.].*", "", defaultNote));
        testFilesConventions.add(new Convention(".*[.]tests[.].*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/test_.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/test[.].*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/testing[.].*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/tests_.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*[-]test[-].*", "", defaultNote));
        testFilesConventions.add(new Convention(".*[-]tests[-].*", "", defaultNote));
        testFilesConventions.add(new Convention(".*__test__.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*__tests__.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*[.]feature", "", defaultNote));
        testFilesConventions.add(new Convention(".*[.]lint[-]test", "", defaultNote));
        testFilesConventions.add(new Convention(".*[.]lint[-]tests", "", defaultNote));
        testFilesConventions.add(new Convention(".*/vitest[.].*", "", "Vitest configuration files"));
        testFilesConventions.add(new Convention(".*/test[-]runner[.].*", "", "Vitest configuration files"));
        testFilesConventions.add(new Convention(".*[.]spec[.]ts", "", "TypeScript test files"));
        testFilesConventions.add(new Convention(".*[.]spec[.]tsx", "", "TSX (React) files"));
        testFilesConventions.add(new Convention(".*[.]spec[.]js", "", "JavaScript test files"));
        testFilesConventions.add(new Convention(".*/karma[.]conf[.]js", "", "Karma test files"));
        testFilesConventions.add(new Convention(".*/protractor[.]conf[.]js", "", "Protractor test files"));
        testFilesConventions.add(new Convention(".*/e2e/.*", "", "Protractor test files"));
        testFilesConventions.add(new Convention(".*/cppunittests/.*", "", "CPP unit test files"));
        testFilesConventions.add(new Convention(".*/palmtests/.*", "", "Palm test files"));
        testFilesConventions.add(new Convention(".*/jstests/.*", "", "JS test files"));

        testFilesConventions.add(new Convention(".*/RestAPIClientTests/.*", "", "API test files"));
        testFilesConventions.add(new Convention(".*/ViewTests/.*", "", "Test files"));

        testFilesConventions.add(new Convention(".*/test[-]resources/.*", "", "Test resources"));
        testFilesConventions.add(new Convention(".*/test[-]helpers/.*", "", "Test helpers"));
        testFilesConventions.add(new Convention(".*/TestData/.*", "", "Test data"));
        testFilesConventions.add(new Convention(".*/mockapi/.*", "", "Mock resources"));
        // Any folder whose name starts with "mock" holds mock resources, so the rest of the name is
        // matched with [^/]* rather than a list of allowed characters. The earlier [a-zA-Z0-9_\- ]+
        // missed a folder named plainly "mock" (the + demanded at least one further character), one
        // with a dot such as "mock.data", and one carrying any non-ASCII character. [^/] cannot cross
        // a separator, so the match stays within the single folder name.
        testFilesConventions.add(new Convention(".*/__mock[^/]*/.*", "", "Mock resources"));
        testFilesConventions.add(new Convention(".*/mock[^/]*/.*", "", "Mock resources"));
        testFilesConventions.add(new Convention(".*_mock[.][a-zA-Z0-9_\\-]+", "", "Mock resources"));

        testFilesConventions.add(new Convention(".*[.]snap", "", "Jest snapshots"));
        testFilesConventions.add(new Convention(".*/jest[.][a-zA-Z0-9\\.]+", "", "Jest files"));
        testFilesConventions.add(new Convention(".*/TestUtilities/.*", "", "Test utilities"));
        testFilesConventions.add(new Convention(".*/[Mm]ocks/.*", "", "Mocks"));
    }

    public List<Convention> getIgnoredFilesConventions() {
        return ignoredFilesConventions;
    }

    public void setIgnoredFilesConventions(List<Convention> ignoredFilesConventions) {
        this.ignoredFilesConventions = ignoredFilesConventions;
    }

    public List<Convention> getTestFilesConventions() {
        return testFilesConventions;
    }

    public void setTestFilesConventions(List<Convention> testFilesConventions) {
        this.testFilesConventions = testFilesConventions;
    }

    public List<Convention> getGeneratedFilesConventions() {
        return generatedFilesConventions;
    }

    public void setGeneratedFilesConventions(List<Convention> generatedFilesConventions) {
        this.generatedFilesConventions = generatedFilesConventions;
    }

    public List<Convention> getBuildAndDeploymentFilesConventions() {
        return buildAndDeploymentFilesConventions;
    }

    public void setBuildAndDeploymentFilesConventions(List<Convention> buildAndDeploymentFilesConventions) {
        this.buildAndDeploymentFilesConventions = buildAndDeploymentFilesConventions;
    }

    public List<Convention> getOtherFilesConventions() {
        return otherFilesConventions;
    }

    public void setOtherFilesConventions(List<Convention> otherFilesConventions) {
        this.otherFilesConventions = otherFilesConventions;
    }
}
