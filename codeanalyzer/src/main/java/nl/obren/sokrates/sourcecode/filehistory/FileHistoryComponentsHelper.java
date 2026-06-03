/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.filehistory;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class FileHistoryComponentsHelper {
    public int getNumberOfActiveDays(List<FileModificationHistory> fileModificationHistories) {
        List<String> dateStrings = getUniqueDates(fileModificationHistories);

        return dateStrings.size();
    }

    public List<String> getUniqueDates(List<FileModificationHistory> fileModificationHistories) {
        // Collect distinct day strings in a sorted set (O(1) dedup) rather than scanning a growing
        // list with contains() per date, then materialise the (already sorted) result as a list.
        TreeSet<String> dateStrings = new TreeSet<>();

        fileModificationHistories.forEach(history -> {
            history.getDates().forEach(date -> {
                if (date.length() >= 10) {
                    dateStrings.add(date.substring(0, 10));
                }
            });
        });

        return new ArrayList<>(dateStrings);
    }
}
