/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.contributors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/*
 * Assumes that you have generated the text file being read using the following git command:
 * git log --pretty=format:"%ad %an <%ae>" --date=short > git-contributors-log.txt
 */
public class GitContributorsUtil {
    public static String printGitLogCommand() {
        return "git log --pretty=format:\"%ad %an <%ae>\" --date=short > git-contributors-log.txt";
    }

    public static List<Contributor> importGitContributorsExport(File file) {
        List<String> lines;
        try {
            lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        List<Contributor> list = new ArrayList<>();
        Map<String, Contributor> map = new HashMap<>();

        lines.forEach(line -> {
            if (line.length() > 11) {
                String date = line.substring(0, 10).trim();
                String name = line.substring(11).trim();
                if (map.containsKey(name)) {
                    map.get(name).addCommit(date);
                } else {
                    Contributor contributor = new Contributor(name);
                    map.put(name, contributor);
                    list.add(contributor);
                    contributor.addCommit(date);
                }
            }
        });

        Collections.sort(list, (a, b) -> b.getCommitsCount() - a.getCommitsCount());

        return list;
    }
}
