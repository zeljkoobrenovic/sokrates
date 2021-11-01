/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.dependencies;

import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;

import java.util.ArrayList;
import java.util.List;

public class Dependency {
    private List<SourceFileDependency> fromFiles = new ArrayList<>();
    private DependencyAnchor from;
    private DependencyAnchor to;
    private String fromComponentName = null;
    private String toComponentName = null;

    public Dependency() {
    }

    public Dependency(DependencyAnchor from, DependencyAnchor to) {
        this.from = from;
        this.to = to;
    }

    public void setFromFiles(List<SourceFileDependency> fromFiles) {
        this.fromFiles = fromFiles;
    }

    public String getFromComponentName() {
        return fromComponentName;
    }

    public void setFromComponentName(String fromComponentName) {
        this.fromComponentName = fromComponentName;
    }

    public String getToComponentName() {
        return toComponentName;
    }

    public void setToComponentName(String toComponentName) {
        this.toComponentName = toComponentName;
    }

    public DependencyAnchor getFrom() {
        return from;
    }

    public List<NamedSourceCodeAspect> getFromComponents(String group) {
        List<NamedSourceCodeAspect> fromComponents = new ArrayList<>();

        fromFiles.forEach(sourceFile -> {
            sourceFile.getSourceFile().getLogicalComponents(group).forEach(sourceCodeAspect -> {
                if (!fromComponents.contains(sourceCodeAspect)) {
                    fromComponents.add(sourceCodeAspect);
                }
            });
        });

        return fromComponents;
    }

    public List<NamedSourceCodeAspect> getToComponents(String group) {
        List<NamedSourceCodeAspect> toComponents = new ArrayList<>();

        to.getSourceFiles().forEach(sourceFile -> {
            sourceFile.getLogicalComponents(group).forEach(sourceCodeAspect -> {
                if (!toComponents.contains(sourceCodeAspect)) {
                    toComponents.add(sourceCodeAspect);
                }
            });
        });

        return toComponents;
    }

    public void setFrom(DependencyAnchor from) {
        this.from = from;
    }

    public DependencyAnchor getTo() {
        return to;
    }

    public void setTo(DependencyAnchor to) {
        this.to = to;
    }

    @Override
    public boolean equals(Object other) {
        boolean result = false;
        if (other instanceof Dependency) {
            Dependency that = (Dependency) other;
            return this.getDependencyString().equals(that.getDependencyString());
        }
        return result;
    }

    @Override
    public int hashCode() {
        return this.getDependencyString().hashCode();
    }

    public String getDependencyString() {
        return from.getAnchor() + " -> " + to.getAnchor();
    }

    public List<SourceFileDependency> getFromFiles() {
        return fromFiles;
    }

    public String getComponentDependency(String group) {
        List<NamedSourceCodeAspect> fromComponents = getFromComponents(group);
        List<NamedSourceCodeAspect> toComponents = getToComponents(group);
        return (fromComponents.size() > 0 ? fromComponents.get(0).getName() : from.getAnchor())
                + " -> "
                + (toComponents.size() > 0 ? toComponents.get(0).getName() : to.getAnchor());
    }
}
