/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.contributors;

import nl.obren.sokrates.sourcecode.githistory.AuthorCommit;
import nl.obren.sokrates.sourcecode.githistory.CommitsPerExtension;
import nl.obren.sokrates.sourcecode.githistory.GitHistoryPerExtensionUtils;
import nl.obren.sokrates.sourcecode.githistory.GitHistoryUtils;

import java.io.File;
import java.util.*;
import java.util.function.Function;

public class GitContributorsUtil {
    public static ContributorsImport importGitContributorsExport(File file) {
        ContributorsImport contributorsImport = new ContributorsImport();
        List<AuthorCommit> authorCommits = GitHistoryUtils.getAuthorCommits(file);
        contributorsImport.setContributors(getContributors(authorCommits));

        List<ContributionTimeSlot> contributorsPerYear = getContributorsPerTimeSlot(authorCommits, (commit) -> commit.getYear());
        List<ContributionTimeSlot> contributorsPerMonth = getContributorsPerTimeSlot(authorCommits, (commit) -> commit.getMonth());
        List<ContributionTimeSlot> contributorsPerWeek = getContributorsPerTimeSlot(authorCommits, (commit) -> commit.getWeekOfYear());
        List<ContributionTimeSlot> contributorsPerDay = getContributorsPerTimeSlot(authorCommits, (commit) -> commit.getDate());

        contributorsImport.setContributorsPerYear(contributorsPerYear);
        contributorsImport.setContributorsPerMonth(contributorsPerMonth);
        contributorsImport.setContributorsPerWeek(contributorsPerWeek);
        contributorsImport.setContributorsPerDay(contributorsPerDay);

        return contributorsImport;
    }

    public static List<CommitsPerExtension> getCommitsPerExtension(File file) {
        return new GitHistoryPerExtensionUtils().getCommitsPerExtensions(file);
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

    public static List<ContributionTimeSlot> getContributorsPerTimeSlot(List<AuthorCommit> authorCommits, Function<AuthorCommit, String> idFunction) {
        List<ContributionTimeSlot> list = new ArrayList<>();
        Map<String, ContributionTimeSlot> map = new HashMap<>();
        Map<String, List<String>> peopleIds = new HashMap<>();

        authorCommits.forEach(authorCommit -> {
            String year = idFunction.apply(authorCommit);
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
            ContributionTimeSlot contributionTimeSlot = map.get(year);
            if (contributionTimeSlot == null) {
                contributionTimeSlot = new ContributionTimeSlot(year);
                map.put(year, contributionTimeSlot);
                list.add(contributionTimeSlot);
            }
            contributionTimeSlot.incrementCommitsCount();
            contributionTimeSlot.setContributorsCount(ids.size());
        });

        Collections.sort(list, Comparator.comparing(ContributionTimeSlot::getTimeSlot));

        return list;
    }
}
