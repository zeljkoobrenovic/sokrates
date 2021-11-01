/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.utils;

import java.util.List;

public class ExplorerStringUtils {

    public static int firstIndexOfAny(List<String> lookupStrings, String content, int startIndex) {
        int index[] = {-1};
        lookupStrings.forEach(lookupString -> {
            int indexOfLookupString = content.indexOf(lookupString, startIndex);
            if (indexOfLookupString >= 0) {
                if (index[0] == -1) {
                    index[0] = indexOfLookupString;
                } else {
                    index[0] = Math.min(index[0], indexOfLookupString);
                }
            }
        });
        return index[0];
    }
}
