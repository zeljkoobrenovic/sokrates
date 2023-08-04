/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.search.FoundLine;
import nl.obren.sokrates.sourcecode.search.SearchExpression;
import nl.obren.sokrates.sourcecode.search.SearchRequest;
import nl.obren.sokrates.sourcecode.search.SearchResult;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearcheableFilesCache {
    private final Map<File, SourceFileWithSearchData> files = new HashMap<>();
    private SearchResult searchResult;

    public static SearcheableFilesCache getInstance(List<SourceFile> sourceFiles) {
        SearcheableFilesCache searcheableFilesCache = new SearcheableFilesCache();
        sourceFiles.forEach(searcheableFilesCache::addSearcheableFile);
        return searcheableFilesCache;
    }

    public List<SourceFile> getSourceFiles() {
        List<SourceFile> sourceFiles = new ArrayList<>();

        files.forEach((file, sourceFileWithSearchData) -> sourceFiles.add(sourceFileWithSearchData.getSourceFile()));

        return sourceFiles;
    }

    public int getTotalLinesCount() {
        final int[] count = {0};
        files.values().forEach(value -> count[0] += value.getLineCount());
        return count[0];
    }

    public void addSearcheableFile(SourceFile sourceFile) {
        SourceFileWithSearchData sourceFileWithSearchData = new SourceFileWithSearchData(sourceFile);
        files.put(sourceFile.getFile(), sourceFileWithSearchData);
    }

    public SearchResult search(SearchRequest searchRequest, ProgressFeedback progressFeedback) {
        progressFeedback.start();
        updateProgress(0, progressFeedback);
        initSearch(searchRequest);
        int currentIndex = 0;
        for (File file : new ArrayList<>(files.keySet())) {
            if (progressFeedback.canceled()) {
                break;
            }
            initFileSearchProcessing(file);
            if (isFilterSet(searchRequest.getPathSearchExpression(), file)) {
                addSearcheableFile(searchRequest.getContentSearchExpression(), file);
            }
            updateProgress(++currentIndex, progressFeedback);
        }
        progressFeedback.end();
        return searchResult;
    }

    private void updateProgress(int currentIndex, ProgressFeedback progressFeedback) {
        if (progressFeedback != null) {
            progressFeedback.progress(currentIndex, files.size());
        }
    }

    private void initFileSearchProcessing(File file) {
        clearCachedFileSearchData(file);
    }

    private void initSearch(SearchRequest searchRequest) {
        searchResult = new SearchResult(searchRequest);
        searchResult.setTotalLinesCount(getTotalLinesCount());
        searchResult.setTotalNumberOfFiles(files.size());
    }

    private void clearCachedFileSearchData(File file) {
        if (files != null && files.get(file) != null) {
            files.get(file).clearSearchData();
        }
    }

    protected void addSearcheableFile(SearchExpression contentFilter, File file) {
        SourceFileWithSearchData sourceFileWithSearchData = files.get(file);
        if (sourceFileWithSearchData != null && instancesFound(contentFilter, sourceFileWithSearchData)) {
            searchResult.getFoundFiles().put(file, sourceFileWithSearchData);
            searchResult.setMaxLines(Math.max(sourceFileWithSearchData.getLineCount(), searchResult.getMaxLines()));
            searchResult.setFoundLinesCount(searchResult.getFoundLinesCount() + sourceFileWithSearchData.getLineCount());
        }
    }

    private boolean instancesFound(SearchExpression contentFilter, SourceFileWithSearchData sourceFileWithSearchData) {
        return StringUtils.isBlank(contentFilter.getExpression()) || getContentInstancesCount(contentFilter, sourceFileWithSearchData) > 0;
    }

    protected boolean isFilterSet(SearchExpression nameFilter, File file) {
        return StringUtils.isBlank(nameFilter.getExpression()) || nameFilter.getMatchedRegex(file.getPath()) != null
                || nameFilter.getMatchedRegex(file.getPath().replace("\\", "/")) != null
                || nameFilter.getMatchedRegex(file.getPath().replace("/", "\\")) != null;
    }

    protected int getContentInstancesCount(SearchExpression contentFilter, SourceFileWithSearchData sourceFileWithSearchData) {
        int foundInstancesCount = 0;
        foundInstancesCount = getFoundInstancesCount(contentFilter, sourceFileWithSearchData);
        sourceFileWithSearchData.setFoundInstancesCount(foundInstancesCount);
        searchResult.setMaxNumberOfFoundInstances(Math.max(sourceFileWithSearchData.getLineCount(), searchResult.getMaxNumberOfFoundInstances()));
        return foundInstancesCount;
    }

    protected int getFoundInstancesCount(SearchExpression contentFilter, SourceFileWithSearchData sourceFileWithSearchData) {
        int foundInstancesCount = 0;
        if (sourceFileWithSearchData != null && isNotEmpty(contentFilter.getExpression())) {
            int lineCount = 0;
            List<String> lines = sourceFileWithSearchData.getLines();
            for (String line : lines) {
                lineCount++;
                String foundText = contentFilter.getMatchedRegex(line);
                if (foundText != null) {
                    foundInstancesCount++;
                    sourceFileWithSearchData.getLinesWithSearchedContent().add(new FoundLine(lineCount, line, foundText));
                }
            }
        }
        return foundInstancesCount;
    }

    private boolean isNotEmpty(String expression) {
        return StringUtils.isNotBlank(expression);
    }

    public Map<File, SourceFileWithSearchData> search() {
        return files;
    }
}
