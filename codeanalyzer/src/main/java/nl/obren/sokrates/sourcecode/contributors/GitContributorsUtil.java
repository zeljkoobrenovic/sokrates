/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.contributors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/*
 * Assumes that you have generated the text file being read using the following git command:
 * git shortlog -sne > git-contributors.txt
 */
public class GitContributorsUtil {
    public static String printGitLogCommand() {
        return "git shortlog -sne > git-contributors.txt";
    }

    public static List<Contributor> importGitContributorsExport(File file) {
        List<Contributor> contributors = new ArrayList<>();

        List<String> lines;
        try {
            lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return contributors;
        }

        lines.forEach(line -> {
            line = line.replace("\t", " ");
            line = line.trim();
            int separatorIndex = line.indexOf(" ");
            if (separatorIndex > 0) {
                String name = line.substring(separatorIndex + 1).trim();
                String countString = line.substring(0, separatorIndex).trim();
                if (StringUtils.isNumeric(countString)) {
                    int count = Integer.parseInt(countString);
                    if (count > 0) {
                        contributors.add(new Contributor(name, count));
                    }
                }
            }
        });

        return contributors;
    }
}
