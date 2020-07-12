/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.contributors;

import org.apache.commons.io.FileUtils;

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

    public static ContributorsImport importGitContributorsExport(File file) {
        List<String> lines;
        try {
            lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return new ContributorsImport();
        }

        ContributorsImport contributorsImport = new ContributorsImport();
        contributorsImport.setContributors(getContributors(lines));
        List<ContributionYear> contributorsPerYear = getContributorsPerYear(lines);
        List<ContributionYear> rookiesPerYear = new ArrayList<>(contributorsPerYear);
        List<ContributionYear> leaversPerYear = new ArrayList<>(contributorsPerYear);

        contributorsImport.setContributorsPerYear(contributorsPerYear);

        return contributorsImport;
    }

    public static List<Contributor> getContributors(List<String> lines) {
        List<Contributor> list = new ArrayList<>();
        Map<String, Contributor> map = new HashMap<>();

        lines.forEach(line -> {
            if (line.length() > 11) {
                String date = line.substring(0, 10).trim();
                String name = line.substring(11).trim();
                if (map.containsKey(name)) {
                    map.get(name).addCommit(date);
                } else {
                    Contributor contributor = Contributor.getInstanceFromNameEmailLine(name);
                    map.put(name, contributor);
                    list.add(contributor);
                    contributor.addCommit(date);
                }
            }
        });
        Collections.sort(list, (a, b) -> b.getCommitsCount() - a.getCommitsCount());

        return list;
    }

    public static List<ContributionYear> getContributorsPerYear(List<String> lines) {
        List<ContributionYear> list = new ArrayList<>();
        Map<String, ContributionYear> map = new HashMap<>();
        Map<String, List<String>> peopleNames = new HashMap<>();

        lines.forEach(line -> {
            if (line.length() > 11) {
                String year = line.substring(0, 4).trim();
                String name = line.substring(11).trim();
                List<String> names = peopleNames.get(year);
                if (names == null) {
                    names = new ArrayList<>();
                    peopleNames.put(year, names);
                }
                if (!names.contains(name)) {
                    names.add(name);
                }
                ContributionYear contributionYear = map.get(year);
                if (contributionYear == null) {
                    contributionYear = new ContributionYear(year);
                    map.put(year, contributionYear);
                    list.add(contributionYear);
                }
                contributionYear.incrementCommitsCount();
                contributionYear.setContributorsCount(names.size());
            }
        });

        Collections.sort(list, Comparator.comparing(ContributionYear::getYear));

        return list;
    }
}
