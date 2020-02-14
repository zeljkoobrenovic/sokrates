/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.scoping;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;

import java.util.ArrayList;
import java.util.List;

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

    public void addConventions(CodeConfiguration codeConfiguration, List<SourceFile> sourceFiles) {
        ConventionUtils.addConventions(ignoredFilesConventions, codeConfiguration.getIgnore(), sourceFiles);
        ConventionUtils.addConventions(testFilesConventions, codeConfiguration.getTest().getSourceFileFilters(), sourceFiles);
        ConventionUtils.addConventions(generatedFilesConventions, codeConfiguration.getGenerated().getSourceFileFilters(), sourceFiles);
        ConventionUtils.addConventions(buildAndDeploymentFilesConventions, codeConfiguration.getBuildAndDeployment().getSourceFileFilters(), sourceFiles);
        ConventionUtils.addConventions(otherFilesConventions, codeConfiguration.getOther().getSourceFileFilters(), sourceFiles);
    }

    private void addOtherConventions() {
        otherFilesConventions.add(new Convention(".*/sonatype-settings[.]xml", "", "Sonatype configuration"));
        otherFilesConventions.add(new Convention(".*/config/checkstyle/.*", "", "Checkstyle configuration"));
        otherFilesConventions.add(new Convention(".*/checkstyle[.]xml", "", "Checkstyle configuration"));
        otherFilesConventions.add(new Convention(".*[.]md", "", "Markdown files"));
        otherFilesConventions.add(new Convention(".*[.]txt", "", "Text files"));
        otherFilesConventions.add(new Convention(".*[.]svg", "", "SVG files"));
    }

    private void addBuildAndDeploymentConventions() {
        buildAndDeploymentFilesConventions.add(new Convention(".*/pom[.]xml", "", "Maven configuration"));
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
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]thrift", "", "Docker configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]mk", "", "Mk files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]cvsignore", "", "CVS configuration files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]git[a-z]+", "", "Git configuration files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*([.]|/)webpack([.]|/).*", "", "Webpack configuration files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]csproj", "", "C# project files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]gitignore", "", "Git ignore files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]gitattributes", "", "Git attributes"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]manifest", "", "Manifest files"));
    }

    private void addGeneratedConventions() {
        String defaultNote = "Generated files";
        generatedFilesConventions.add(new Convention(".*/generated/.*", "", defaultNote));
        generatedFilesConventions.add(new Convention(".*/gen-code/.*", "", defaultNote));
    }

    private void addTestConventions() {
        String defaultNote = "Test files";
        testFilesConventions.add(new Convention(".*/test/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*/tests/.*", "", defaultNote));
        testFilesConventions.add(new Convention(".*[-]tests/.*", "", defaultNote));
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
        testFilesConventions.add(new Convention(".*[.]spec[.]ts", "", defaultNote));
        testFilesConventions.add(new Convention(".*[.]spec[.]js", "", defaultNote));
        testFilesConventions.add(new Convention(".*/karma[.]conf[.]js", "", defaultNote));
        testFilesConventions.add(new Convention(".*/protractor[.]conf[.]js", "", defaultNote));
    }

    private void addIgnoreConventions() {
        ignoredFilesConventions.add(new Convention(".*/[.][a-zA-Z0-9_]+.*", "", "Hidden files and folders"));
        ignoredFilesConventions.add(new Convention(".*/node_modules/.*", "", "Node modules"));
        ignoredFilesConventions.add(new Convention(".*/bower_components/.*", "", "Bower components"));
        ignoredFilesConventions.add(new Convention(".*/target/.*", "", "Compiled files"));
        ignoredFilesConventions.add(new Convention(".*/dist/.*", "", "Binaries for distribution"));
        ignoredFilesConventions.add(new Convention("(?i).*bootstrap[.]css", "", "Bootstrap CSS files"));
        ignoredFilesConventions.add(new Convention("(?i).*/jquery.*[.]js", "", "jQuery files"));
        ignoredFilesConventions.add(new Convention(".*/sokrates[.]json", "", "Sokrates configuration"));
        ignoredFilesConventions.add(new Convention(".*/docs/.*", "", "Documentation"));
        ignoredFilesConventions.add(new Convention(".*/bootstrap[.]js", "", "3rd party library"));
        ignoredFilesConventions.add(new Convention(".*/.*[.]min[.]js", "", "Minimized JS library"));
    }
}
