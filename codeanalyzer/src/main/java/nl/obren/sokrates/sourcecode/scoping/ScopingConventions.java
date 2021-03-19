/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.scoping;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;

import java.util.ArrayList;
import java.util.List;

// based on:
// - https://github.com/github/linguist/blob/master/lib/linguist/generated.rb
// - https://raw.githubusercontent.com/github/linguist/master/lib/linguist/documentation.yml
// - https://github.com/github/linguist/blob/master/lib/linguist/languages.yml
// - https://github.com/github/linguist/blob/master/lib/linguist/vendor.yml
public class ScopingConventions {
    private List<Convention> ignoredFilesConventions = new ArrayList<>();
    private List<Convention> testFilesConventions = new ArrayList<>();
    private List<Convention> generatedFilesConventions = new ArrayList<>();
    private List<Convention> buildAndDeploymentFilesConventions = new ArrayList<>();
    private List<Convention> otherFilesConventions = new ArrayList<>();

    public ScopingConventions() {
        addIgnoreConventions();
        addTestConventions();
        addGeneratedConventions();
        addBuildAndDeploymentConventions();
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
        System.out.println(s);
        ignoredFilesConventions.forEach(convention -> {
            System.out.println(s2 + convention.toString() + " (" + convention.getNote() + ")");
        });
        System.out.println();
    }

    public void addConventions(CodeConfiguration codeConfiguration, List<SourceFile> sourceFiles) {
        ConventionUtils.addConventions(ignoredFilesConventions, codeConfiguration.getIgnore(), sourceFiles);
        ConventionUtils.addConventions(testFilesConventions, codeConfiguration.getTest().getSourceFileFilters(), sourceFiles);
        ConventionUtils.addConventions(generatedFilesConventions, codeConfiguration.getGenerated().getSourceFileFilters(), sourceFiles);
        ConventionUtils.addConventions(buildAndDeploymentFilesConventions, codeConfiguration.getBuildAndDeployment().getSourceFileFilters(), sourceFiles);
        ConventionUtils.addConventions(otherFilesConventions, codeConfiguration.getOther().getSourceFileFilters(), sourceFiles);
    }

    private void addOtherConventions() {
        // static code analysis configurations
        otherFilesConventions.add(new Convention(".*/sonatype-settings[.]xml", "", "Sonatype configuration"));
        otherFilesConventions.add(new Convention(".*/config/checkstyle/.*", "", "Checkstyle configuration"));
        otherFilesConventions.add(new Convention(".*/checkstyle[.]xml", "", "Checkstyle configuration"));

        otherFilesConventions.add(new Convention(".*[.]md", "", "Markdown files"));
        otherFilesConventions.add(new Convention(".*[.]markdown", "", "Markdown files"));
        otherFilesConventions.add(new Convention(".*[.]mdown", "", "Markdown files"));
        otherFilesConventions.add(new Convention(".*[.]mdwn", "", "Markdown files"));
        otherFilesConventions.add(new Convention(".*[.]mdx", "", "Markdown files"));
        otherFilesConventions.add(new Convention(".*[.]mkd", "", "Markdown files"));
        otherFilesConventions.add(new Convention(".*[.]mkdn", "", "Markdown files"));
        otherFilesConventions.add(new Convention(".*[.]mkdown", "", "Markdown files"));
        otherFilesConventions.add(new Convention(".*[.]ronn", "", "Markdown files"));
        otherFilesConventions.add(new Convention(".*[.]workbook", "", "Markdown files"));

        otherFilesConventions.add(new Convention(".*[.]json", "", "JSON files"));
        // otherFilesConventions.add(new Convention(".*[.]yml", "", "YAML files"));
        // otherFilesConventions.add(new Convention(".*[.]yaml", "", "YAML files"));

        otherFilesConventions.add(new Convention(".*[.]svg", "", "SVG files"));

        // ignore lists
        otherFilesConventions.add(new Convention(".*/[.]atomignore", "", "Ignore list"));
        otherFilesConventions.add(new Convention(".*/[.]babelignore", "", "Ignore list"));
        otherFilesConventions.add(new Convention(".*/[.]bzrignore", "", "Ignore list"));
        otherFilesConventions.add(new Convention(".*/[.]coffeelintignore", "", "Ignore list"));
        otherFilesConventions.add(new Convention(".*/[.]cvsignore", "", "Ignore list"));
        otherFilesConventions.add(new Convention(".*/[.]dockerignore", "", "Ignore list"));
        otherFilesConventions.add(new Convention(".*/[.]eslintignore", "", "Ignore list"));
        otherFilesConventions.add(new Convention(".*/[.]gitignore", "", "Ignore list"));
        otherFilesConventions.add(new Convention(".*/[.]nodemonignore", "", "Ignore list"));
        otherFilesConventions.add(new Convention(".*/[.]npmignore", "", "Ignore list"));
        otherFilesConventions.add(new Convention(".*/[.]prettierignore", "", "Ignore list"));
        otherFilesConventions.add(new Convention(".*/[.]stylelintignore", "", "Ignore list"));
        otherFilesConventions.add(new Convention(".*/[.]vscodeignore", "", "Ignore list"));

        otherFilesConventions.add(new Convention(".*/[.]cpplint[.]py", "", "Linter"));

        otherFilesConventions.add(new Convention(".*/Jenkinsfile", "", "Jenkinsfile"));

        otherFilesConventions.add(new Convention(".*[.]storyboard", "", "Storyboard"));
        otherFilesConventions.add(new Convention(".*[.]xib", "", "XIB files"));

        otherFilesConventions.add(new Convention(".*[.]bash_[a-z]+", "", "Bash files"));

        // config
        otherFilesConventions.add(new Convention(".*[.]apacheconf", "", "Configuration"));
        otherFilesConventions.add(new Convention(".*[.]vhost", "", "Configuration"));
        otherFilesConventions.add(new Convention(".*/[.]htaccess", "", "Configuration"));
        otherFilesConventions.add(new Convention(".*[.]csf", "", "Configuration"));
        otherFilesConventions.add(new Convention(".*[.]diff", "", "Configuration"));
        otherFilesConventions.add(new Convention(".*[.]patch", "", "Configuration"));

        otherFilesConventions.add(new Convention(".*[.]editorconfig", "", "NPM Config"));

        otherFilesConventions.add(new Convention(".*[.]npmrc", "", "Editor configuration"));

        otherFilesConventions.add(new Convention(".*[.]properties", "", "Properties"));

        otherFilesConventions.add(new Convention(".*[.]dsp", "", "Microsoft Developer Studio Project"));

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
        otherFilesConventions.add(new Convention(".*/go[.]mod", "", "Text files"));
        otherFilesConventions.add(new Convention(".*/go[.]sum", "", "Text files"));
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
    }

    private void addBuildAndDeploymentConventions() {
        buildAndDeploymentFilesConventions.add(new Convention(".*/pom[.]xml", "", "Maven configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]nuspec", "", "NuSpec configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/build[.]xml", "", "Build configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/assembly[.]xml", "", "Maven assembly plugin configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/assembly/src[.]xml", "", "Maven assembly plugin configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]gradle", "", "Gradle configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.][-]gradle[.]js", "", "Gradle configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]sh", "", "Scripts"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]bat", "", "Scripts"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/AndroidManifest[.]xml", "", "Scripts"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/package[.]json", "", "npm configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/package[-]lock[.]json", "", "npm configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/glide[.]yml", "", "Glide configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/glide[.]yaml", "", "Glide configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/glide[.]lock", "", "Glide configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/docker[-]compose[.]yaml", "", "Docker configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/docker[-]compose[.]yml", "", "Docker configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]dockerfile", "", "Docker configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]mk", "", "Mk files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]cvsignore", "", "CVS configuration files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]git[a-z]+", "", "Git configuration files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*([.]|/)webpack([.]|/).*", "", "Webpack configuration files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]csproj", "", "C# project files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]gitignore", "", "Git ignore files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]gitattributes", "", "Git attributes"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]gitconfig", "", "Git config"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]gitmodules", "", "Git modules"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]manifest", "", "Manifest files"));

        buildAndDeploymentFilesConventions.add(new Convention(".*[.]mak", "", "Make files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]make", "", "Make files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]mk", "", "Make files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]mkfile", "", "Make files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]dotsettings", "", ".Net settings files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/jenkins/.*[.]groovy", "", "Jenkins files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/fastlane/.*[.]rb", "", "Fastlane files"));
    }

    private void addGeneratedConventions() {
        String defaultNote = "Generated files";
        generatedFilesConventions.add(new Convention(".*/generated/.*", "", defaultNote));
        generatedFilesConventions.add(new Convention(".*/gen-code/.*", "", defaultNote));
        generatedFilesConventions.add(new Convention(".*/_generated_/.*", "", defaultNote));
        generatedFilesConventions.add(new Convention(".*/_generated/.*", "", defaultNote));
        generatedFilesConventions.add(new Convention(".*/__generated__/.*", "", defaultNote));
        generatedFilesConventions.add(new Convention(".*/gen/.*[.]go", "", "Generated Go files"));

        generatedFilesConventions.add(new Convention(".*/npm[-]shrinkwrap[.]json", "", "A generated npm shrinkwrap file"));
        generatedFilesConventions.add(new Convention(".*/package[-]lock[.]json", "", "A generated npm package lock file"));

        generatedFilesConventions.add(new Convention(".*/.*[.]nib", "", "Xcode generated files"));
        generatedFilesConventions.add(new Convention(".*/.*[.]xcworkspacedata", "", "Xcode generated files"));
        generatedFilesConventions.add(new Convention(".*/.*[.]xcuserstate", "", "Xcode generated files"));

        generatedFilesConventions.add(new Convention(".*/Pods/.*", "", "Cocoa pods"));
        generatedFilesConventions.add(new Convention(".*/Carthage/Build/.*", "", "Carthage builds"));
        generatedFilesConventions.add(new Convention(".*/[.](css|js)[.]map", "", "JS/CSS map"));

        generatedFilesConventions.add(new Convention(".*/[.]js", "[/][/] Generated by .*", 1, "JS generated"));
        generatedFilesConventions.add(new Convention(".*/[.]xml", "[<]doc[>]", 2, "A generated documentation file for a .NET assembly"));
        generatedFilesConventions.add(new Convention(".*[.]designer[.](cs|vb)", "", "A codegen file for a .NET project"));
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

        generatedFilesConventions.add(new Convention(".*[.]cs", "[/][/][ ]*<auto-generated.*", 3, "A generated C# file"));
        generatedFilesConventions.add(new Convention(".*[.]vb", "[/][/][ ]*<auto-generated.*", 3, "A generated VisualBasic file"));

        generatedFilesConventions.add(new Convention(".*[.]js", "[/][*] parser generated by jison .*", 1, "A Jison-generated file"));
        generatedFilesConventions.add(new Convention(".*[.]js", "[/][*] generated by jison[-]lex .*", 1, "A Jison-generated file"));
        generatedFilesConventions.add(new Convention(".*[.]zep[.][a-z0-9_]+", "", ""));
        generatedFilesConventions.add(new Convention(".*[.]dart", ".*GENERATED CODE.*DO NOT MODIFY.*", 1, "A generated Dart file"));
        generatedFilesConventions.add(new Convention(".*[.]h", ".*Automatically created by Devel[:][:]PPPort.*", 9, "A generated Perl/Pollution/Portability header file"));
        generatedFilesConventions.add(new Convention(".*[.](html|html|xhtml)", ".*Generated by pkgdown[:] do not edit by hand.*", 2, "A generated HTML source file"));
        generatedFilesConventions.add(new Convention(".*[.](html|html|xhtml)", ".*Generated by Doxygen.*", 31, "A generated HTML source file"));
        generatedFilesConventions.add(new Convention(".*[.](html|html|xhtml)", "[ ]*[<]meta name[=]\"generator\" .*", 31, "A generated HTML source file"));
    }

    private void addTestConventions() {
        String defaultNote = "Test files";
        testFilesConventions.add(new Convention(".*/[Tt]est/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/[Tt]est/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/[Tt]ests/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*[.][Tt]est/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*[.][Tt]ests/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*[.][Tt]est[.].*", "", defaultNote));
        testFilesConventions.add(new Convention(".*[.][Tt]ests[.].*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/UnitTests?/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/IntegrationTests?/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/UITests?/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/src/testPlay/.*", "", defaultNote));
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
        testFilesConventions.add(new Convention(".*/tests_.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*[-]test[-].*", "", defaultNote));
        testFilesConventions.add(new Convention(".*[-]tests[-].*", "", defaultNote));
        testFilesConventions.add(new Convention(".*__test__.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*__tests__.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*[.]feature", "", defaultNote));
        testFilesConventions.add(new Convention(".*[.]lint[-]test", "", defaultNote));
        testFilesConventions.add(new Convention(".*[.]lint[-]tests", "", defaultNote));
        testFilesConventions.add(new Convention(".*[.]spec[.]ts", "", "TypeScript test files"));
        testFilesConventions.add(new Convention(".*[.]spec[.]js", "", "JavaScript test files"));
        testFilesConventions.add(new Convention(".*/karma[.]conf[.]js", "", "Karma test files"));
        testFilesConventions.add(new Convention(".*/protractor[.]conf[.]js", "", "Protractor test files"));
        testFilesConventions.add(new Convention(".*/e2e/.*", "", "Protractor test files"));

        testFilesConventions.add(new Convention(".*/RestAPIClientTests/.*", "", "API test files"));
        testFilesConventions.add(new Convention(".*/ViewTests/.*", "", "Test files"));

        testFilesConventions.add(new Convention(".*/test[-]resources/.*", "", "Test resources"));
        testFilesConventions.add(new Convention(".*/TestData/.*", "", "Test data"));
        testFilesConventions.add(new Convention(".*/__mocks?__/.*", "", "Mock resources"));
        testFilesConventions.add(new Convention(".*/mockapi/.*", "", "Mock resources"));
        testFilesConventions.add(new Convention(".*/mock[-]server/.*", "", "Mock resources"));

        testFilesConventions.add(new Convention(".*[.]snap", "", "Jest snapshots"));
        testFilesConventions.add(new Convention(".*/TestUtilities/.*", "", "Test utilities"));
        testFilesConventions.add(new Convention(".*/Mocks/.*", "", "Mocks"));
    }

    private void addIgnoreConventions() {
        ignoredFilesConventions.add(new Convention(".*/[.][a-zA-Z0-9_]+.*", "", "Hidden files and folders"));

        ignoredFilesConventions.add(new Convention(".*/node_modules/.*", "", "Node dependencies"));
        ignoredFilesConventions.add(new Convention(".*/bower_components/.*", "", "Bower components"));

        ignoredFilesConventions.add(new Convention(".*/[.]yarn/releases/.*", "", "Yarn releases"));
        ignoredFilesConventions.add(new Convention(".*/[.]yarn/plugins/.*", "", "Yarn plugins"));

        ignoredFilesConventions.add(new Convention(".*/target/.*", "", "Compiled files"));

        ignoredFilesConventions.add(new Convention(".*/bin/.*", "", "Binaries for distribution"));
        ignoredFilesConventions.add(new Convention(".*/cache/.*", "", "Caches"));
        ignoredFilesConventions.add(new Convention(".*/dependencies/.*", "", "Dependencies"));
        ignoredFilesConventions.add(new Convention(".*/Godeps/.*", "", "Golang dependencies"));
        ignoredFilesConventions.add(new Convention(".*/[Vv]endors?/.*", "", "Dependencies"));
        ignoredFilesConventions.add(new Convention(".*/extern(al)?/.*", "", "Dependencies"));
        ignoredFilesConventions.add(new Convention(".*/(3rd|[Tt]hird)[-_]?[Pp]arty/.*", "", "Dependencies"));
        ignoredFilesConventions.add(new Convention(".*/deps/.*", "", "Dependencies"));
        ignoredFilesConventions.add(new Convention(".*/dist/.*", "", "Distributions"));
        ignoredFilesConventions.add(new Convention(".*/debian/.*", "", "Distributions"));
        ignoredFilesConventions.add(new Convention(".*[.]m4", "", "stuff autogenerated by autoconf - still C deps"));

        ignoredFilesConventions.add(new Convention("(?i).*/jquery.*[.]js", "", "jQuery files"));
        ignoredFilesConventions.add(new Convention(".*/docs/.*", "", "Documentation"));
        ignoredFilesConventions.add(new Convention(".*/bootstrap[.](js|css|less|scss|styl)", "", "Bootstrap"));
        ignoredFilesConventions.add(new Convention(".*[.]bootstrap[.](js|css|less|scss|styl)", "", "Bootstrap"));
        ignoredFilesConventions.add(new Convention(".*/bootstrap-datepicker/.*", "", "Bootstrap"));
        ignoredFilesConventions.add(new Convention(".*/foundation[.](css|less|scss|styl)", "", "Foundation css"));
        ignoredFilesConventions.add(new Convention(".*/normalize[.](css|less|scss|styl)", "", "Normalize css"));
        ignoredFilesConventions.add(new Convention(".*/skeleton[.](css|less|scss|styl)", "", "Skeleton css"));
        ignoredFilesConventions.add(new Convention(".*/[Bb]ourbon[.](css|less|scss|styl)", "", "Bourbon css"));
        ignoredFilesConventions.add(new Convention(".*/animate[.](css|less|scss|styl)", "", "Animate css"));
        ignoredFilesConventions.add(new Convention(".*/materialize[.](js|css|less|scss|styl)", "", "Materialize css"));
        ignoredFilesConventions.add(new Convention(".*/select2[.](js|css|scss)", "", "Select2 css"));
        ignoredFilesConventions.add(new Convention(".*/bulma[.](css|sass|scss)", "", "Bulma css"));
        ignoredFilesConventions.add(new Convention(".*[.]min[.]js", "", "Minimized JS library"));
        ignoredFilesConventions.add(new Convention(".*[.]css[.]js", "", "Minimized CSS library"));
        ignoredFilesConventions.add(new Convention(".*tiny_mce_src[.]js", "", "Tiny MCE JS library"));
        ignoredFilesConventions.add(new Convention(".*/js/yui/.*", "", "YUI JS library"));
        ignoredFilesConventions.add(new Convention(".*/js/flotr/.*", "", "Flotr JS library"));
        ignoredFilesConventions.add(new Convention(".*[.]import[.](css|less|scss|styl)", "", "Stylesheets imported from packages"));
        ignoredFilesConventions.add(new Convention(".*font-?awesome\\.(css|less|scss|styl)", "", "Font Awesome"));
        ignoredFilesConventions.add(new Convention(".*font-?awesome/.*\\.(css|less|scss|styl)", "", "Font Awesome"));
        ignoredFilesConventions.add(new Convention(".*/fuelux[.]js", "", "Fuel UX"));
        ignoredFilesConventions.add(new Convention(".*/bootbox[.]js", "", "Bootbox"));
        ignoredFilesConventions.add(new Convention(".*/pdf[-]worker[.]js", "", "pdf-worker"));
        ignoredFilesConventions.add(new Convention(".*/slick\\.\\w+.js", "", "Slick"));
        ignoredFilesConventions.add(new Convention(".*/slick\\.\\w+.js", "", "Slick"));
        ignoredFilesConventions.add(new Convention(".*/prototype(.*)\\.js", "", "Prototype"));
        ignoredFilesConventions.add(new Convention(".*/effects\\.js", "", "Prototype"));
        ignoredFilesConventions.add(new Convention(".*/controls\\.js", "", "Prototype"));
        ignoredFilesConventions.add(new Convention(".*/dragdrop\\.js", "", "Prototype"));
        ignoredFilesConventions.add(new Convention(".*\\.d\\.ts", "", "Typescript definition files"));
        ignoredFilesConventions.add(new Convention(".*/mootools([^.]*)\\d+\\.\\d+.\\d+([^.]*)\\.js", "", "MooTools"));
        ignoredFilesConventions.add(new Convention(".*/dojo\\.js", "", "Dojo"));
        ignoredFilesConventions.add(new Convention(".*/MochiKit\\.js", "", "MochiKit"));
        ignoredFilesConventions.add(new Convention(".*/yahoo-([^.]*)\\.js", "", "YUI"));
        ignoredFilesConventions.add(new Convention(".*/yui([^.]*)\\.js", "", "YUI"));

        ignoredFilesConventions.add(new Convention(".*/Leaflet\\.Coordinates-\\d+\\.\\d+\\.\\d+\\.src\\.js", "", "Leaflet plugins"));
        ignoredFilesConventions.add(new Convention(".*/leaflet[.]draw[-]src[.]js", "", "Leaflet plugins"));
        ignoredFilesConventions.add(new Convention(".*/leaflet[.]draw[.]css", "", "Leaflet plugins"));
        ignoredFilesConventions.add(new Convention(".*/Control[.]FullScreen[.]css", "", "Leaflet plugins"));
        ignoredFilesConventions.add(new Convention(".*/Control[.]FullScreen[.]js", "", "Leaflet plugins"));
        ignoredFilesConventions.add(new Convention(".*/leaflet[.]spin[.]js", "", "Leaflet plugins"));
        ignoredFilesConventions.add(new Convention(".*/wicket[-]leaflet[.]js", "", "Leaflet plugins"));

        ignoredFilesConventions.add(new Convention(".*/_sokrates/.*", "", "Sokrates files"));
        ignoredFilesConventions.add(new Convention(".*/_sokrates_landscape/.*", "", "Sokrates landscape files"));
        ignoredFilesConventions.add(new Convention(".*/git[-][a-zA-Z0-9_]+[.]txt", "", "Git data exports for sokrates analyses"));

        ignoredFilesConventions.add(new Convention(".*/testdata/.*", "", "Test data"));
        ignoredFilesConventions.add(new Convention(".*/Godeps/_workspace/.*", "", "Go dependencies"));

        ignoredFilesConventions.add(new Convention(".*/_esy/.*", "", "esy.sh dependencies"));

        ignoredFilesConventions.add(new Convention(".*/ckeditor[.]js", "", "WYS editors"));
        ignoredFilesConventions.add(new Convention(".*/tiny_mce([^.]*)\\.j", "", "WYS editors"));
        ignoredFilesConventions.add(new Convention(".*/tiny_mce/(langs|plugins|themes|utils)/.*", "", "WYS editors"));

        ignoredFilesConventions.add(new Convention(".*/ace-builds/.*", "", "Ace Editor"));
        ignoredFilesConventions.add(new Convention(".*/MathJax/.*", "", "MathJax"));
        ignoredFilesConventions.add(new Convention(".*/fontello(.*?)\\.css", "", "Fontello CSS files"));
        ignoredFilesConventions.add(new Convention(".*/Chart\\.js", "", "Chart.js"));

        ignoredFilesConventions.add(new Convention(".*/[Cc]ode[Mm]irror/(\\d+\\.\\d+/)?(lib|mode|theme|addon|keymap|demo)/.*", "", "CodeMirror"));

        ignoredFilesConventions.add(new Convention(".*/shBrush([^.]*)\\.js", "", "SyntaxHighlighter - http://alexgorbatchev.com/"));
        ignoredFilesConventions.add(new Convention(".*/shCore\\.js", "", "SyntaxHighlighter - http://alexgorbatchev.com/"));
        ignoredFilesConventions.add(new Convention(".*/shLegacy\\.js", "", "SyntaxHighlighter - http://alexgorbatchev.com/"));

        ignoredFilesConventions.add(new Convention(".*/angular([^.]*)\\.js", "", "AngularJS"));
        ignoredFilesConventions.add(new Convention(".*/react(-[^.]*)?\\.js", "", "React"));
        ignoredFilesConventions.add(new Convention(".*/d3(\\.v\\d+)?([^.]*)\\.js", "", "D3"));
        ignoredFilesConventions.add(new Convention(".*/flow-typed/.*\\.js", "", "flow-typed"));

        ignoredFilesConventions.add(new Convention(".*/modernizr\\-\\d\\.\\d+(\\.\\d+)?\\.js", "", "Modernizr"));
        ignoredFilesConventions.add(new Convention(".*/modernizr\\.custom\\.\\d+\\.js", "", "Modernizr"));

        ignoredFilesConventions.add(new Convention(".*/knockout-(\\d+\\.){3}(debug\\.)?js", "", "Knockout"));

        ignoredFilesConventions.add(new Convention(".*/docs?/_?(build|themes?|templates?|static)/.*", "", "Sphinx"));
        ignoredFilesConventions.add(new Convention(".*/docs?/_?(build|themes?|templates?|static)/.*", "", "Sphinx"));

        ignoredFilesConventions.add(new Convention(".*/admin_media/.*", "", "django"));

        ignoredFilesConventions.add(new Convention(".*/fabfile\\.py", "", "Fabric"));

        ignoredFilesConventions.add(new Convention(".*[.]osx", "", ".osx"));

        ignoredFilesConventions.add(new Convention(".*[.]osx", "", ".osx"));

        ignoredFilesConventions.add(new Convention(".*/Carthage/.*", "", "Carthage"));
        ignoredFilesConventions.add(new Convention(".*/Sparkle/.*", "", "Sparkle"));
        ignoredFilesConventions.add(new Convention(".*/Fabric[.]framework/.*", "", "Fabric.framework"));
        ignoredFilesConventions.add(new Convention(".*/BuddyBuildSDK[.]framework/.*", "", "BuddyBuildSDK.framework"));
        ignoredFilesConventions.add(new Convention(".*/Realm[.]framework/.*", "", "Realm.framework"));
        ignoredFilesConventions.add(new Convention(".*/RealmSwift[.]framework/.*", "", "RealmSwift.framework"));

        ignoredFilesConventions.add(new Convention(".*/gradlew", "", "Gradle"));
        ignoredFilesConventions.add(new Convention(".*/gradle/wrapper/.*", "", "Gradle"));

        ignoredFilesConventions.add(new Convention(".*/mvnw", "", "Maven"));
        ignoredFilesConventions.add(new Convention(".*/mvnw\\.cmd", "", "Maven"));

        ignoredFilesConventions.add(new Convention(".*[-]vsdoc\\.js", "", "Visual Studio IntelliSense"));

        ignoredFilesConventions.add(new Convention(".*/jquery([^.]*)\\.validate(\\.unobtrusive)?\\.js", "", "jQuery validation plugin (MS bundles this with asp.net mvc)"));
        ignoredFilesConventions.add(new Convention(".*/jquery([^.]*)\\.unobtrusive\\-ajax\\.js", "", "jQuery validation plugin (MS bundles this with asp.net mvc)"));

        ignoredFilesConventions.add(new Convention(".*/[Mm]icrosoft([Mm]vc)?([Aa]jax|[Vv]alidation)(\\.debug)?\\.js", "", "Microsoft Ajax"));

        ignoredFilesConventions.add(new Convention(".*/[Pp]ackages\\/.+\\.\\d+\\/.*", "", "NuGet"));

        ignoredFilesConventions.add(new Convention(".*/extjs/.*?\\.(js|xml|txt|html|properties)", "", "ExtJS"));
        ignoredFilesConventions.add(new Convention(".*/extjs/[.]sencha/.*", "", "ExtJS"));
        ignoredFilesConventions.add(new Convention(".*/extjs/docs/", "", "ExtJS"));
        ignoredFilesConventions.add(new Convention(".*/extjs/builds/", "", "ExtJS"));
        ignoredFilesConventions.add(new Convention(".*/extjs/cmd/", "", "ExtJS"));
        ignoredFilesConventions.add(new Convention(".*/extjs/examples/", "", "ExtJS"));
        ignoredFilesConventions.add(new Convention(".*/extjs/locale/", "", "ExtJS"));
        ignoredFilesConventions.add(new Convention(".*/extjs/packages/", "", "ExtJS"));
        ignoredFilesConventions.add(new Convention(".*/extjs/plugins/", "", "ExtJS"));
        ignoredFilesConventions.add(new Convention(".*/extjs/resources/", "", "ExtJS"));
        ignoredFilesConventions.add(new Convention(".*/extjs/src/", "", "ExtJS"));
        ignoredFilesConventions.add(new Convention(".*/extjs/welcome/", "", "ExtJS"));

        ignoredFilesConventions.add(new Convention(".*/html5shiv\\.js", "", "Html5shiv"));

        ignoredFilesConventions.add(new Convention(".*/cordova([^.]*)\\.js", "", "PhoneGap/Cordova"));
        ignoredFilesConventions.add(new Convention(".*/cordova\\-\\d\\.\\d(\\.\\d)?\\.js", "", "PhoneGap/Cordova"));

        ignoredFilesConventions.add(new Convention(".*/foundation(\\..*)?\\.js", "", "Foundation js"));

        ignoredFilesConventions.add(new Convention(".*/Vagrantfile", "", "Vagrant"));

        ignoredFilesConventions.add(new Convention(".*/.[Dd][Ss]_[Ss]tore", "", ".DS_Stores"));
        ignoredFilesConventions.add(new Convention(".*/.[Dd][Ss]_[Ss]tore/.*", "", ".DS_Stores"));

        ignoredFilesConventions.add(new Convention(".*/vignettes/.*", "", "R packages"));
        ignoredFilesConventions.add(new Convention(".*/inst/extdata/.*", "", "R packages"));

        ignoredFilesConventions.add(new Convention(".*/octicons[.]css", "", "Octicons"));
        ignoredFilesConventions.add(new Convention(".*/sprockets[-]octicons[.]scss", "", "Octicons"));

        ignoredFilesConventions.add(new Convention(".*/activator", "", "Typesafe Activator"));
        ignoredFilesConventions.add(new Convention(".*/activator\\.bat", "", "Typesafe Activator"));

        ignoredFilesConventions.add(new Convention(".*/proguard[.]pro", "", "ProGuard"));
        ignoredFilesConventions.add(new Convention(".*/proguard[-]rules[.]pro", "", "ProGuard"));

        ignoredFilesConventions.add(new Convention(".*/puphpet/.*", "", "PuPHPet"));

        ignoredFilesConventions.add(new Convention(".*/[Gg]roovydoc/.*", "", "Generated documentation"));
        ignoredFilesConventions.add(new Convention(".*/[Jj]avadoc/.*", "", "Generated documentation"));
        ignoredFilesConventions.add(new Convention(".*/inst/doc/.*", "", "Generated documentation"));

        ignoredFilesConventions.add(new Convention(".*/Thumbs[.]db", "", "Thumbs.db"));
        ignoredFilesConventions.add(new Convention(".*/__MACOSX/.*", "", "__MACOSX folder"));
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
