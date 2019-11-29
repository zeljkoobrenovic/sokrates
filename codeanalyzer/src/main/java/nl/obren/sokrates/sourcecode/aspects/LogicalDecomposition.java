package nl.obren.sokrates.sourcecode.aspects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.SourceCodeFiles;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class LogicalDecomposition {
    private String name = "";
    private String scope = "main";
    private int componentsFolderDepth = 1;
    private List<SourceFileFilter> filters = new ArrayList<>();
    private boolean includeRemainingFiles = true;
    private List<NamedSourceCodeAspect> components = new ArrayList<>();
    private String renderingOrientation = "TB";
    private DependenciesFinder dependenciesFinder = new DependenciesFinder();

    public LogicalDecomposition() {
    }

    public LogicalDecomposition(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public int getComponentsFolderDepth() {
        return componentsFolderDepth;
    }

    public void setComponentsFolderDepth(int componentsFolderDepth) {
        this.componentsFolderDepth = componentsFolderDepth;
    }

    public List<NamedSourceCodeAspect> getComponents() {
        return components;
    }

    public void setComponents(List<NamedSourceCodeAspect> components) {
        this.components = components;
    }

    @JsonIgnore
    public void updateLogicalComponentsFiles(SourceCodeFiles sourceCodeFiles, CodeConfiguration codeConfiguration, File codeConfigurationFile) {
        List<SourceFile> allSourceFiles = codeConfiguration.getScope(scope).getSourceFiles();
        List<SourceFile> filteredSourceFiles = getSourceFiles(codeConfiguration);
        if (componentsFolderDepth > 0) {
            components.addAll(SourceCodeAspectUtils.getSourceCodeAspectBasedOnFolderDepth(
                    CodeConfiguration.getAbsoluteSrcRoot(codeConfiguration.getSrcRoot(), codeConfigurationFile),
                    filteredSourceFiles, componentsFolderDepth));
        }
        for (NamedSourceCodeAspect aspect : components) {
            sourceCodeFiles.getSourceFiles(aspect, filteredSourceFiles);
            aspect.getSourceFiles().forEach(sourceFile -> {
                sourceFile.getLogicalComponents().add(aspect);
            });
        }

        CodeConfigurationUtils.populateUnclassifiedAndMultipleAspectsFiles(components,
                (includeRemainingFiles ? allSourceFiles : filteredSourceFiles),
                sourceFileAspectPair -> {
                    NamedSourceCodeAspect namedSourceCodeAspect = sourceFileAspectPair.getRight();
                    namedSourceCodeAspect.setFiltering(name);
                    sourceFileAspectPair.getLeft().getLogicalComponents().add(namedSourceCodeAspect);
                    return null;
                });
        CodeConfigurationUtils.removeEmptyAspects(components);
        components.forEach(component -> component.setFiltering(name));
    }

    @JsonIgnore
    public boolean isInScope(SourceFile sourceFile) {
        final boolean[] inScope = {false};
        components.forEach(component -> component.getSourceFiles().forEach(compSourceFile -> {
            if (compSourceFile.equals(sourceFile)) {
                inScope[0] = true;
                return;
            }
        }));

        return inScope[0];
    }

    private List<SourceFile> getSourceFiles(CodeConfiguration codeConfiguration) {
        Predicate<SourceFile> sourceFileFilter = sourceFile -> {
            List<SourceFileFilter> filters = getFilters();
            if (filters == null || filters.size() == 0) {
                return true;
            }

            boolean[] inScope = {false};
            filters.forEach(filter -> {
                if (filter.matches(sourceFile)) {
                    if (filter.getInclude()) {
                        inScope[0] = true;
                    } else {
                        inScope[0] = false;
                        return;
                    }
                }
            });

            return inScope[0];
        };
        return codeConfiguration.getScope(scope).getSourceFiles().stream()
                .filter(sourceFileFilter).collect(Collectors.toList());
    }

    public boolean isIncludeRemainingFiles() {
        return includeRemainingFiles;
    }

    public void setIncludeRemainingFiles(boolean includeRemainingFiles) {
        this.includeRemainingFiles = includeRemainingFiles;
    }

    public List<SourceFileFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<SourceFileFilter> filters) {
        this.filters = filters;
    }

    public String getRenderingOrientation() {
        return renderingOrientation;
    }

    public void setRenderingOrientation(String renderingOrientation) {
        this.renderingOrientation = renderingOrientation;
    }

    public DependenciesFinder getDependenciesFinder() {
        return dependenciesFinder;
    }

    public void setDependenciesFinder(DependenciesFinder dependenciesFinder) {
        this.dependenciesFinder = dependenciesFinder;
    }

    public NamedSourceCodeAspect getComponentByName(String fromComponent) {
        Optional<NamedSourceCodeAspect> first = this.components.stream().filter(c -> c.getName().equalsIgnoreCase(fromComponent)).findFirst();
        return first.isPresent() ? first.get() : null;
    }
}
