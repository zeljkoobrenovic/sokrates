/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

/**
 * A symbolic link the source code walk did not follow.
 *
 * <p>The walk stops at a link rather than resolving it, so the files behind one are not measured.
 * Recording the link lets the report say which paths were left out, instead of leaving the reader
 * with a file count smaller than the checkout and no explanation.
 *
 * <p>{@code target} is the link as written on disk - relative targets are kept relative, because
 * {@code .claude/skills -> ../.agents/skills} is the form the reader will recognise from the
 * repository. {@code insideAnalysisRoot} says where that target <em>points</em>, which is the part
 * they cannot work out from the link alone. It does not promise anything exists there: a link
 * pointing inside the root at a target that was never created still points inside.
 *
 * <p>Immutable, and carries no no-arg constructor or setters: nothing serializes this type - every
 * hop that holds it is marked {@code @JsonIgnore}.
 */
public class SymbolicLink {
    private final String path;
    private final String target;
    private final boolean insideAnalysisRoot;

    public SymbolicLink(String path, String target, boolean insideAnalysisRoot) {
        this.path = path;
        this.target = target;
        this.insideAnalysisRoot = insideAnalysisRoot;
    }

    public String getPath() {
        return path;
    }

    public String getTarget() {
        return target;
    }

    public boolean isInsideAnalysisRoot() {
        return insideAnalysisRoot;
    }
}
