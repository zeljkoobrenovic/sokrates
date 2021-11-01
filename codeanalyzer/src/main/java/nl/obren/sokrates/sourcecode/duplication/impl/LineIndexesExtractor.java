/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class LineIndexesExtractor {
    // limit line max length to avoid memory overlflow cause by minimized libraries
    public static final int MAX_LINE_LENGTH = 500;

    private int totalLinesCount = 0;
    private Map<String, LineInfo> uniqueLinesMap = new HashMap<>();
    private Map<Integer, LineInfo> uniqueLinesIdMap = new HashMap<>();

    public int getTotalLinesCount() {
        return totalLinesCount;
    }

    public void setTotalLinesCount(int totalLinesCount) {
        this.totalLinesCount = totalLinesCount;
    }

    public List<Integer> getLineIDs(List<String> lines) {
        totalLinesCount += lines.size();
        List<Integer> lineIDs = new ArrayList<>();
        lines.forEach(line -> {
            addLineID(lineIDs, line);
        });

        return lineIDs;
    }

    private void addLineID(List<Integer> lineIDs, String line) {
        if (line.length() > MAX_LINE_LENGTH) {
            line = line.substring(0, MAX_LINE_LENGTH);
        }

        LineInfo lineInfo = uniqueLinesMap.get(line);

        if (lineInfo == null) {
            lineInfo = new LineInfo();
            uniqueLinesMap.put(line, lineInfo);
            uniqueLinesIdMap.put(lineInfo.getId(), lineInfo);
        } else {
            lineInfo.incrementCount();
        }

        lineIDs.add(lineInfo.getId());
    }

    public Map<String, LineInfo> getUniqueLinesMap() {
        return uniqueLinesMap;
    }

    public void setUniqueLinesMap(Map<String, LineInfo> uniqueLinesMap) {
        this.uniqueLinesMap = uniqueLinesMap;
    }

    public void clearUniqueLines(List<Integer> lineIndexes) {
        for (int i = 0; i < lineIndexes.size(); i++) {
            int index = lineIndexes.get(i).intValue();
            if (uniqueLinesIdMap.get(index) == null || uniqueLinesIdMap.get(index).getCount() == 1) {
                lineIndexes.set(i, FileInfoForDuplication.IGNORE_LINE_INDEX);
            }
        }
    }
}
