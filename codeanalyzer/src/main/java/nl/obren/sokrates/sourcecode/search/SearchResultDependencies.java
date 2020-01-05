/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.search;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileWithSearchData;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.dependencies.SourceFileDependency;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;

public class SearchResultDependencies {
    private SearchResult searchResult;
    private SearchResultCleaner searchResultCleaner;
    private SourceNodeExtractionType sourceNodeExtractionType = SourceNodeExtractionType.COMPONENT;
    private TargetNodeExtractionType targetNodeExtractionType = TargetNodeExtractionType.CLEANED_TEXT;
    private int minimalNumberOfInstancesFilter = 1;

    public SearchResultDependencies(SearchResult searchResult, SearchResultCleaner searchResultCleaner) {
        this.searchResult = searchResult;
        this.searchResultCleaner = searchResultCleaner;
    }

    public List<Dependency> getDependencies() {
        List<Dependency> dependencies = new ArrayList<>();

        searchResult.getFoundFiles().values().forEach(sourceFileWithSearchData -> {
            if (sourceFileWithSearchData.getLinesWithSearchedContent().size() >= minimalNumberOfInstancesFilter) {
                if (dependencies.size() <= 1000) {
                    addDependency(dependencies, sourceFileWithSearchData);
                }
            }
        });

        return dependencies;
    }

    private void addDependency(List<Dependency> dependencies, SourceFileWithSearchData sourceFileWithSearchData) {
        SourceFile sourceFile = sourceFileWithSearchData.getSourceFile();

        sourceFileWithSearchData.getLinesWithSearchedContent().forEach(line -> {
            DependencyAnchor from = new DependencyAnchor(sourceFile.getRelativePath());
            from.getSourceFiles().add(sourceFile);

            String target = getTarget(line);
            if (StringUtils.isNotBlank(target)) {
                DependencyAnchor to = new DependencyAnchor(target);

                SourceFileDependency sourceFileDependency = new SourceFileDependency(sourceFile);
                sourceFileDependency.setCodeFragment(line.getFoundText());

                Dependency dependency = new Dependency(from, to);
                dependency.getFromFiles().add(sourceFileDependency);
                dependencies.add(dependency);
            }
        });
    }

    private String getTarget(FoundLine line) {
        switch (targetNodeExtractionType) {
            case ORIGINAL_TEXT:
                return line.getFoundText();
            case CLEANED_TEXT:
            default:
                return searchResultCleaner.clean(line.getFoundText());
        }
    }

    private String getSourceNode(SourceFile sourceFile, String group) {
        switch (sourceNodeExtractionType) {
            case COMPONENT:
                return sourceFile.getLogicalComponents(group).size() == 0 ? "" : StringUtils.defaultIfBlank(sourceFile.getLogicalComponents(group).get(0).getName(), FileSystems.getDefault().getSeparator());
            case FILE_NAME:
                return StringUtils.defaultIfBlank(sourceFile.getFile().getName(), FileSystems.getDefault().getSeparator());
            case FILE_PATH:
            default:
                return StringUtils.defaultIfBlank(sourceFile.getRelativePath(), FileSystems.getDefault().getSeparator());
        }
    }

    public List<ComponentDependency> getComponentDependencies(List<Dependency> dependencies, String group) {
        List<ComponentDependency> componentDependencies = new ArrayList<>();
        dependencies.forEach(dependency -> {
            ComponentDependency componentDependency = new ComponentDependency(getSourceNode(dependency.getFromFiles().get(0).getSourceFile(), group), dependency.getTo().getAnchor());
            if (componentDependencies.contains(componentDependency)) {
                ComponentDependency existingComponentDependency = componentDependencies.get(componentDependencies.indexOf(componentDependency));
                existingComponentDependency.setCount(existingComponentDependency.getCount() + 1);
            } else {
                componentDependencies.add(componentDependency);
            }
        });
        return componentDependencies;
    }

    public SearchResult getSearchResult() {
        return searchResult;
    }

    public void setSearchResult(SearchResult searchResult) {
        this.searchResult = searchResult;
    }

    public SourceNodeExtractionType getSourceNodeExtractionType() {
        return sourceNodeExtractionType;
    }

    public void setSourceNodeExtractionType(SourceNodeExtractionType sourceNodeExtractionType) {
        this.sourceNodeExtractionType = sourceNodeExtractionType;
    }

    public int getMinimalNumberOfInstancesFilter() {
        return minimalNumberOfInstancesFilter;
    }

    public void setMinimalNumberOfInstancesFilter(int minimalNumberOfInstancesFilter) {
        this.minimalNumberOfInstancesFilter = minimalNumberOfInstancesFilter;
    }

    public enum SourceNodeExtractionType {
        FILE_NAME, FILE_PATH, COMPONENT
    }

    public enum TargetNodeExtractionType {
        ORIGINAL_TEXT, CLEANED_TEXT
    }
}
