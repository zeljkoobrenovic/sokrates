/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.filehistory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileHistoryComponentsHelper {
    public int getNumberOfActiveDays(List<FileModificationHistory> fileModificationHistories) {
        List<String> dateStrings = getUniqueDates(fileModificationHistories);

        return dateStrings.size();
    }

    public List<String> getUniqueDates(List<FileModificationHistory> fileModificationHistories) {
        List<String> dateStrings = new ArrayList<>();

        fileModificationHistories.forEach(history -> {
            history.getDates().forEach(date -> {
                if (date.length() >= 10) {
                    String dateOnly = date.substring(0, 10);

                    if (!dateStrings.contains(dateOnly)) {
                        dateStrings.add(dateOnly);
                    }
                }
            });
        });

        Collections.sort(dateStrings);

        return dateStrings;
    }
}
