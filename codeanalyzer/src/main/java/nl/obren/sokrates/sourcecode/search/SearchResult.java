package nl.obren.sokrates.sourcecode.search;

import nl.obren.sokrates.sourcecode.SourceFileWithSearchData;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchResult {
    private int totalNumberOfFiles;
    private int totalLinesCount;
    private Map<File, SourceFileWithSearchData> foundFiles = new HashMap();
    private int foundLinesCount;
    private int maxLines;
    private int maxNumberOfFoundInstances;
    private SearchRequest searchRequest;

    public SearchResult(SearchRequest searchRequest) {
        this.searchRequest = searchRequest;
    }

    public Map<File, SourceFileWithSearchData> getFoundFiles() {
        return foundFiles;
    }

    public void setFoundFiles(Map<File, SourceFileWithSearchData> foundFiles) {
        this.foundFiles = foundFiles;
    }

    public Integer getMaxLines() {
        return maxLines;
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }

    public Integer getMaxNumberOfFoundInstances() {
        return maxNumberOfFoundInstances;
    }

    public void setMaxNumberOfFoundInstances(int maxNumberOfFoundInstances) {
        this.maxNumberOfFoundInstances = maxNumberOfFoundInstances;
    }

    public SearchRequest getSearchRequest() {
        return searchRequest;
    }

    public void setSearchRequest(SearchRequest searchRequest) {
        this.searchRequest = searchRequest;
    }

    public int getTotalNumberOfFiles() {
        return totalNumberOfFiles;
    }

    public void setTotalNumberOfFiles(int totalNumberOfFiles) {
        this.totalNumberOfFiles = totalNumberOfFiles;
    }

    public int getTotalLinesCount() {
        return totalLinesCount;
    }

    public void setTotalLinesCount(int totalLinesCount) {
        this.totalLinesCount = totalLinesCount;
    }

    public int getFoundLinesCount() {
        return foundLinesCount;
    }

    public void setFoundLinesCount(int totalFoundLinesCount) {
        this.foundLinesCount = totalFoundLinesCount;
    }

    public List<FoundText> getFoundTextList() {
        Map<String, FoundText> map = new HashMap();

        for (SourceFileWithSearchData sourceFileWithSearchData : foundFiles.values()) {
            for (FoundLine foundLine : sourceFileWithSearchData.getLinesWithSearchedContent()) {
                addFoundTextToMap(map, foundLine);
            }
        }

        return new ArrayList<>(map.values());
    }

    public int getMaxCountPerText() {
        int max = 0;

        for (FoundText foundText : getFoundTextList()) {
            max = Math.max(max, foundText.getCount());
        }

        return max;
    }

    public int getTotalNumberOfMatchingLines() {
        int count = 0;

        for (FoundText foundText : getFoundTextList()) {
            count += foundText.getCount();
        }

        return count;
    }

    protected void addFoundTextToMap(Map<String, FoundText> map, FoundLine foundLine) {
        String key = foundLine.getFoundText();
        FoundText foundText = map.get(key);
        if (foundText != null) {
            foundText.setCount(foundText.getCount() + 1);
        } else {
            map.put(key, new FoundText(key, 1));
        }
    }


    public int getTotalCountPerText() {
        int total = 0;

        for (FoundText foundText : getFoundTextList()) {
            total += foundText.getCount();
        }

        return total;
    }
}
