/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.contributors;

import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The lowercased relative paths of each scope's source files (main, test, build, generated, other),
 * as git history paths are matched against them: the scope tabs of the activity charts and the
 * per-extension history use the same sets, so they agree on what belongs to a scope. A path is also
 * registered without its first folder, for histories extracted one level above the source root.
 */
public class ScopePaths {
    private ScopePaths() {
    }

    /** The path set per scope (only scopes with files), or null when no scope has files. */
    public static Map<String, Set<String>> byScope(CodeConfiguration codeConfiguration) {
        Map<String, Set<String>> pathsByScope = new LinkedHashMap<>();
        add(pathsByScope, "main", codeConfiguration.getMain());
        add(pathsByScope, "test", codeConfiguration.getTest());
        add(pathsByScope, "build", codeConfiguration.getBuildAndDeployment());
        add(pathsByScope, "generated", codeConfiguration.getGenerated());
        add(pathsByScope, "other", codeConfiguration.getOther());
        return pathsByScope.isEmpty() ? null : pathsByScope;
    }

    private static void add(Map<String, Set<String>> pathsByScope, String scope, NamedSourceCodeAspect aspect) {
        if (aspect == null || aspect.getSourceFiles() == null || aspect.getSourceFiles().isEmpty()) {
            return;
        }
        Set<String> paths = new HashSet<>();
        aspect.getSourceFiles().forEach(sourceFile -> {
            if (sourceFile.getRelativePath() != null) {
                String path = sourceFile.getRelativePath().toLowerCase();
                paths.add(path);
                int slash = path.indexOf('/');
                if (slash > 0 && slash < path.length() - 1) {
                    paths.add(path.substring(slash + 1));
                }
            }
        });
        if (!paths.isEmpty()) {
            pathsByScope.put(scope, paths);
        }
    }
}
