package nl.obren.sokrates.sourcecode.search;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileWithSearchData;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class SearchResultDependenciesTest {
    private SearchResult searchResult;

    @Before
    public void createSearchResults() {
        SourceFile sourceFile1 = new SourceFile(new File("/root/path/name1.ext"));
        sourceFile1.relativize(new File("/root"));
        sourceFile1.getLogicalComponents().add(new NamedSourceCodeAspect("ComponentA"));

        SourceFile sourceFile2 = new SourceFile(new File("/root/path/name2.ext"));
        sourceFile2.relativize(new File("/root"));
        sourceFile2.getLogicalComponents().add(new NamedSourceCodeAspect("ComponentB"));

        SourceFileWithSearchData sourceFileWithSearchData1 = new SourceFileWithSearchData(sourceFile1);
        sourceFileWithSearchData1.getLinesWithSearchedContent().add(new FoundLine(1, "line start middle end content", "start midNOISEdle end"));

        SourceFileWithSearchData sourceFileWithSearchData2 = new SourceFileWithSearchData(sourceFile2);
        sourceFileWithSearchData2.getLinesWithSearchedContent().add(new FoundLine(10, "line START MIDNOISEDLE END", "START MIDNOISEDLE END"));

        searchResult = new SearchResult(new SearchRequest());
        searchResult.getFoundFiles().put(sourceFile1.getFile(), sourceFileWithSearchData1);
        searchResult.getFoundFiles().put(sourceFile2.getFile(), sourceFileWithSearchData2);
    }

    @Test
    public void getDependenciesWithoutCleaning() throws Exception {
        SearchResultCleaner searchResultCleaner = new SearchResultCleaner();

        SearchResultDependencies searchDependencies = new SearchResultDependencies(searchResult, searchResultCleaner);
        List<Dependency> dependencyList = searchDependencies.getDependencies();
        List<ComponentDependency> componentDependencies = searchDependencies.getComponentDependencies(dependencyList, "");

        assertEquals(dependencyList.size(), 2);
        assertEquals(componentDependencies.size(), 2);
        assertEquals(dependencyList.get(0).getDependencyString(), "path/name2.ext -> START MIDNOISEDLE END");
        assertEquals(dependencyList.get(1).getDependencyString(), "path/name1.ext -> start midNOISEdle end");
        assertEquals(componentDependencies.get(0).getDependencyString(), "ComponentB -> START MIDNOISEDLE END");
        assertEquals(componentDependencies.get(1).getDependencyString(), "ComponentA -> start midNOISEdle end");
    }

    @Test
    public void getDependenciesWithCleaning() throws Exception {
        SearchResultCleaner searchResultCleaner = new SearchResultCleaner();

        SearchResultDependencies searchDependencies = new SearchResultDependencies(searchResult, searchResultCleaner);

        searchResultCleaner.setEndCleaningPattern("(end|END)");
        searchResultCleaner.setStartCleaningPattern("(start|START)");
        searchResultCleaner.setReplacePairs(Arrays.asList(new ReplacePair("NOISE", "")));
        searchDependencies.setSourceNodeExtractionType(SearchResultDependencies.SourceNodeExtractionType.FILE_NAME);

        List<Dependency> dependencyList = searchDependencies.getDependencies();
        List<ComponentDependency> componentDependencies = searchDependencies.getComponentDependencies(dependencyList, "");
        assertEquals(dependencyList.size(), 2);
        assertEquals(componentDependencies.size(), 2);
        assertEquals(dependencyList.get(0).getDependencyString(), "path/name2.ext -> MIDDLE");
        assertEquals(dependencyList.get(1).getDependencyString(), "path/name1.ext -> middle");
        assertEquals(componentDependencies.get(0).getDependencyString(), "name2.ext -> MIDDLE");
        assertEquals(componentDependencies.get(1).getDependencyString(), "name1.ext -> middle");
    }

}
