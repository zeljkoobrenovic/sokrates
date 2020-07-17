/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.contributors;

import nl.obren.sokrates.sourcecode.githistory.AuthorCommit;
import nl.obren.sokrates.sourcecode.githistory.FileUpdate;
import nl.obren.sokrates.sourcecode.githistory.GitHistoryUtils;

import java.io.File;
import java.util.*;

public class GitContributorsUtil {
    public static ContributorsImport importGitContributorsExport(File file) {
        ContributorsImport contributorsImport = new ContributorsImport();
        List<AuthorCommit> authorCommits = GitHistoryUtils.getAuthorCommits(file);
        contributorsImport.setContributors(getContributors(authorCommits));
        List<ContributionYear> contributorsPerYear = getContributorsPerYear(authorCommits);

        contributorsImport.setContributorsPerYear(contributorsPerYear);

        return contributorsImport;
    }

    public static List<Contributor> getContributors(List<AuthorCommit> authorCommits) {
        List<Contributor> list = new ArrayList<>();
        Map<String, Contributor> map = new HashMap<>();

        authorCommits.forEach(authorCommit -> {
            String date = authorCommit.getDate();
            Contributor contributor = new Contributor(authorCommit.getAuthorEmail());
            String id = contributor.getEmail();
            if (map.containsKey(id)) {
                map.get(id).addCommit(date);
            } else {
                map.put(id, contributor);
                list.add(contributor);
                contributor.addCommit(date);
            }
        });
        Collections.sort(list, (a, b) -> b.getCommitsCount() - a.getCommitsCount());

        return list;
    }

    public static List<ContributionYear> getContributorsPerYear(List<AuthorCommit> authorCommits) {
        List<ContributionYear> list = new ArrayList<>();
        Map<String, ContributionYear> map = new HashMap<>();
        Map<String, List<String>> peopleIds = new HashMap<>();

        authorCommits.forEach(authorCommit -> {
            String year = authorCommit.getYear();
            Contributor contributor = new Contributor(authorCommit.getAuthorEmail());
            String id = contributor.getEmail();
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
        });

        Collections.sort(list, Comparator.comparing(ContributionYear::getYear));

        return list;
    }
}
