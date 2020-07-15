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
            return new ContributorsImport();
        }

        ContributorsImport contributorsImport = new ContributorsImport();
        contributorsImport.setContributors(getContributors(lines));
        List<ContributionYear> contributorsPerYear = getContributorsPerYear(lines);

        contributorsImport.setContributorsPerYear(contributorsPerYear);

        return contributorsImport;
    }

    public static List<Contributor> getContributors(List<String> lines) {
        List<Contributor> list = new ArrayList<>();
        Map<String, Contributor> map = new HashMap<>();

        lines.forEach(line -> {
            if (line.length() > 11) {
                String date = line.substring(0, 10).trim();
                String contributorInfo = line.substring(11).trim();
                Contributor contributor = Contributor.getInstanceFromNameEmailLine(contributorInfo);
                String id = contributor.getId();
                if (map.containsKey(id)) {
                    map.get(id).addCommit(date);
                } else {
                    map.put(id, contributor);
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
        Map<String, List<String>> peopleIds = new HashMap<>();

        lines.forEach(line -> {
            if (line.length() > 11) {
                String year = line.substring(0, 4).trim();
                String contributorInfo = line.substring(11).trim();
                Contributor contributor = Contributor.getInstanceFromNameEmailLine(contributorInfo);
                String id = contributor.getId();
                List<String> ids = peopleIds.get(year);
                if (ids == null) {
                    ids = new ArrayList<>();
                    peopleIds.put(year, ids);
                }
                if (!ids.contains(id)) {
                    ids.add(id);
                }
                ContributionYear contributionYear = map.get(year);
                if (contributionYear == null) {
                    contributionYear = new ContributionYear(year);
                    map.put(year, contributionYear);
                    list.add(contributionYear);
                }
                contributionYear.incrementCommitsCount();
                contributionYear.setContributorsCount(ids.size());
            }
        });

        Collections.sort(list, Comparator.comparing(ContributionYear::getYear));

        return list;
    }
}
