/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.contributors;

import nl.obren.sokrates.common.utils.ProcessingStopwatch;
import nl.obren.sokrates.sourcecode.analysis.FileHistoryAnalysisConfig;
import nl.obren.sokrates.sourcecode.githistory.AuthorCommit;
import nl.obren.sokrates.sourcecode.githistory.CommitsPerExtension;
import nl.obren.sokrates.sourcecode.githistory.GitHistoryPerExtensionUtils;
import nl.obren.sokrates.sourcecode.githistory.GitHistoryUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.*;
import java.util.function.Function;

public class GitContributorsUtil {
    private static final Log LOG = LogFactory.getLog(GitContributorsUtil.class);

    public static ContributorsImport importGitContributorsExport(File file, FileHistoryAnalysisConfig config) {
        ContributorsImport contributorsImport = new ContributorsImport();
        List<AuthorCommit> authorCommits = GitHistoryUtils.getAuthorCommits(file, config);
        int index[] = {0};
        authorCommits.forEach(commit -> {
            String date = commit.getDate();
            if (StringUtils.isBlank(contributorsImport.getFirstCommitDate()) || date.compareTo(contributorsImport.getFirstCommitDate()) <= 0) {
                contributorsImport.setFirstCommitDate(date);
            }
            if (StringUtils.isBlank(contributorsImport.getLatestCommitDate()) || date.compareTo(contributorsImport.getLatestCommitDate()) >= 0) {
                contributorsImport.setLatestCommitDate(date);
            }
        });
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

    public static List<CommitsPerExtension> getCommitsPerExtension(File file, FileHistoryAnalysisConfig config) {
        return new GitHistoryPerExtensionUtils().getCommitsPerExtensions(file, config);
    }

    public static List<Contributor> getContributors(List<AuthorCommit> authorCommits) {
        List<Contributor> list = new ArrayList<>();
        Map<String, Contributor> map = new HashMap<>();

        authorCommits.forEach(authorCommit -> {
            String date = authorCommit.getDate();
            Contributor contributor = new Contributor(authorCommit.getAuthorEmail());
            contributor.setUserName(authorCommit.getUserName());
            contributor.setBot(authorCommit.isBot());
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
            String timeSlot = idFunction.apply(authorCommit);
            String id = authorCommit.getAuthorEmail();
            List<String> ids = peopleIds.get(timeSlot);
            if (ids == null) {
                ids = new ArrayList<>();
                peopleIds.put(timeSlot, ids);
            }
            if (!ids.contains(id)) {
                ids.add(id);
            }
            ContributionTimeSlot contributionTimeSlot = map.get(timeSlot);
            if (contributionTimeSlot == null) {
                contributionTimeSlot = new ContributionTimeSlot(timeSlot);
                map.put(timeSlot, contributionTimeSlot);
                list.add(contributionTimeSlot);
            }
            contributionTimeSlot.incrementCommitsCount();
            contributionTimeSlot.setContributorsCount(ids.size());
        });

        Collections.sort(list, Comparator.comparing(ContributionTimeSlot::getTimeSlot));

        return list;
    }
}
