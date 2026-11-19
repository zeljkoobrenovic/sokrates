/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.perl;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.dependencies.HeuristicDependenciesExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Perl dependencies: a file depends on a package it loads with {@code use}, {@code require},
 * {@code use parent} or {@code use base}. Every Perl file is a source (scripts and tests without a
 * {@code package} too, anchored by their path); a file declaring packages is a target for each of them.
 * Only the full package name matches: {@code use App::Model::User} does not reference {@code App} or
 * {@code App::Model}.
 */
public class PerlHeuristicDependenciesExtractor extends HeuristicDependenciesExtractor {
    private static final Pattern PACKAGE = Pattern.compile(
            "(?m)^[ \\t]*package[ \\t]+([A-Za-z_][\\w:]*\\w)[ \\t]*(?:v?[\\d._]+[ \\t]*)?[;{]");
    // POD runs from a line starting with "=<word>" to "=cut" (or the end of the file).
    private static final Pattern POD = Pattern.compile("(?ms)^=[A-Za-z]\\w*.*?(?:^=cut\\b[^\\n]*$|\\z)");
    private static final Pattern END_OF_CODE = Pattern.compile("(?m)^__(?:END|DATA)__\\b");

    @Override
    public List<DependencyAnchor> extractDependencyAnchors(SourceFile sourceFile) {
        List<DependencyAnchor> anchors = new ArrayList<>();
        String content = codeOnly(sourceFile.getContent());

        List<String> packageNames = new ArrayList<>();
        String firstDeclaration = null;
        Matcher matcher = PACKAGE.matcher(content);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (name.equals("main") || packageNames.contains(name)) {
                continue;
            }
            if (firstDeclaration == null) {
                firstDeclaration = matcher.group().trim();
            }
            packageNames.add(name);
        }

        PerlDependencyAnchor anchor;
        if (packageNames.isEmpty()) {
            String path = sourceFile.getRelativePath() != null ? sourceFile.getRelativePath() : sourceFile.getFile().getPath();
            anchor = new PerlDependencyAnchor(path);
            anchor.setCodeFragment(path);
        } else {
            anchor = new PerlDependencyAnchor(packageNames.get(0));
            anchor.setCodeFragment(firstDeclaration);
            packageNames.forEach(name -> {
                anchor.getPackageNames().add(name);
                anchor.getDependencyPatterns().addAll(dependencyPatterns(name));
            });
        }
        anchor.getSourceFiles().add(sourceFile);
        anchors.add(anchor);

        return anchors;
    }

    /** The line patterns (matched against a whole line) that load the package {@code name}. */
    public static List<String> dependencyPatterns(String name) {
        String quoted = Pattern.quote(name);
        List<String> patterns = new ArrayList<>();
        // use Name; use Name qw(...); require Name; eval { require Name }; - the name followed by a
        // character that cannot continue it, so App::Model does not match "use App::Model::User".
        patterns.add("\\s*(?:eval\\s*[{\"']\\s*)?(?:use|require)\\s+" + quoted + "(?:[^\\w:].*)?");
        // use parent 'Name'; use parent -norequire, "Name"; use base qw(Other Name);
        patterns.add("\\s*use\\s+(?:parent|base)\\b.*[\\s'\"(]" + quoted + "(?:[^\\w:].*)?");
        return patterns;
    }

    /** The content without POD sections and without what follows {@code __END__} / {@code __DATA__}. */
    public static String codeOnly(String content) {
        content = SourceCodeCleanerUtils.normalizeLineEnds(content);
        Matcher end = END_OF_CODE.matcher(content);
        if (end.find()) {
            content = content.substring(0, end.start());
        }
        // keep the line breaks of POD blocks, so line positions don't matter but lines stay lines
        Matcher pod = POD.matcher(content);
        StringBuilder result = new StringBuilder();
        while (pod.find()) {
            pod.appendReplacement(result, Matcher.quoteReplacement(pod.group().replaceAll("[^\\n]", "")));
        }
        pod.appendTail(result);
        return result.toString();
    }

    @Override
    protected String getContentForDependencies(SourceFile sourceFile) {
        return codeOnly(sourceFile.getContent());
    }

    @Override
    protected boolean mayReference(String content, DependencyAnchor targetAnchor) {
        if (targetAnchor instanceof PerlDependencyAnchor) {
            // a script (no packages) is never a target
            return ((PerlDependencyAnchor) targetAnchor).getPackageNames().stream().anyMatch(content::contains);
        }
        return super.mayReference(content, targetAnchor);
    }

    static class PerlDependencyAnchor extends DependencyAnchor {
        private final List<String> packageNames = new ArrayList<>();

        PerlDependencyAnchor(String anchor) {
            super(anchor);
        }

        List<String> getPackageNames() {
            return packageNames;
        }
    }
}
