/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.filehistory;

import nl.obren.sokrates.sourcecode.githistory.MergeUpdate;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GitMergesUtil {
    public static List<MergeUpdate> getMergesFromFile(File file) {
        List<MergeUpdate> updates = new ArrayList<>();
        List<String> lines;
        try {
            lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return updates;
        }

        lines.stream().forEach(line -> {
            MergeUpdate mergeUpdate = parseLine(line);
            if (mergeUpdate != null) {
                updates.add(mergeUpdate);
            }
        });

        return updates;
    }

    public static MergeUpdate parseLine(String line) {
        int index1 = line.indexOf(" ");
        if (index1 >= 10) {
            String date = line.substring(0, 10).trim();
            String author = line.substring(index1 + 1).trim();

            return new MergeUpdate(date, author);
        }

        return null;
    }
}
