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
import nl.obren.sokrates.sourcecode.threshold.Thresholds;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.*;
import java.util.function.Function;

public class GitContributorsUtil {
    private static final Log LOG = LogFactory.getLog(GitContributorsUtil.class);

    public static ContributorsImport importGitContributorsExport(File file, FileHistoryAnalysisConfig config) {
        return importGitContributorsExport(file, config, null);
    }

    /**
     * @param pathsByScope for each scope (main, test, build, generated, other), the lowercased
     *                     relative paths of its files; when non-null the import also computes the
     *                     per-scope time-slot lists (contributorsPer*ByScope) that back the scope tabs
     *                     in the activity diagrams. When null those maps stay empty.
     */
    public static ContributorsImport importGitContributorsExport(File file, FileHistoryAnalysisConfig config, Map<String, Set<String>> pathsByScope) {
        ContributorsImport contributorsImport = new ContributorsImport();
        List<AuthorCommit> authorCommits = GitHistoryUtils.getAuthorCommits(file, config);
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

        populateTimeSlots(contributorsImport, authorCommits, null);

        if (pathsByScope != null) {
            pathsByScope.forEach((scope, paths) -> {
                List<AuthorCommit> scopeCommits = GitHistoryUtils.getAuthorCommits(file, config,
                        fileUpdate -> fileUpdate.getPath() != null && paths.contains(fileUpdate.getPath().toLowerCase()));
                populateTimeSlots(contributorsImport, scopeCommits, scope);
            });
        }

        return contributorsImport;
    }

    // Builds the per-year/month/week/day time-slot lists from the given commits. When scope is null
    // they become the all-scope lists; otherwise they are stored under that scope key in the
    // per-scope maps.
    private static void populateTimeSlots(ContributorsImport contributorsImport, List<AuthorCommit> authorCommits, String scope) {
        List<ContributionTimeSlot> perYear = getContributorsPerTimeSlot(authorCommits, (commit) -> commit.getYear());
        List<ContributionTimeSlot> perMonth = getContributorsPerTimeSlot(authorCommits, (commit) -> commit.getMonth());
        List<ContributionTimeSlot> perWeek = getContributorsPerTimeSlot(authorCommits, (commit) -> commit.getWeekOfYear());
        List<ContributionTimeSlot> perDay = getContributorsPerTimeSlot(authorCommits, (commit) -> commit.getDate());

        if (scope == null) {
            contributorsImport.setContributorsPerYear(perYear);
            contributorsImport.setContributorsPerMonth(perMonth);
            contributorsImport.setContributorsPerWeek(perWeek);
            contributorsImport.setContributorsPerDay(perDay);
        } else {
            contributorsImport.getContributorsPerYearByScope().put(scope, perYear);
            contributorsImport.getContributorsPerMonthByScope().put(scope, perMonth);
            contributorsImport.getContributorsPerWeekByScope().put(scope, perWeek);
            contributorsImport.getContributorsPerDayByScope().put(scope, perDay);
        }
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
            int fileUpdatesCount = authorCommit.getFileUpdatesCount();
            int linesAdded = authorCommit.getLinesAdded();
            int linesDeleted = authorCommit.getLinesDeleted();
            if (map.containsKey(id)) {
                map.get(id).addCommit(date, fileUpdatesCount, linesAdded, linesDeleted);
            } else {
                map.put(id, contributor);
                list.add(contributor);
                contributor.addCommit(date, fileUpdatesCount, linesAdded, linesDeleted);
            }
        });
        Collections.sort(list, (a, b) -> b.getCommitsCount() - a.getCommitsCount());

        return list;
    }

    public static List<ContributionTimeSlot> getContributorsPerTimeSlot(List<AuthorCommit> authorCommits, Function<AuthorCommit, String> idFunction) {
        List<ContributionTimeSlot> list = new ArrayList<>();
        Map<String, ContributionTimeSlot> map = new HashMap<>();
        // Distinct contributor ids per time slot, kept in a set so membership is O(1) per commit.
        Map<String, Set<String>> peopleIds = new HashMap<>();

        authorCommits.forEach(authorCommit -> {
            String timeSlot = idFunction.apply(authorCommit);
            String id = authorCommit.getAuthorEmail();
            Set<String> ids = peopleIds.computeIfAbsent(timeSlot, k -> new HashSet<>());
            ids.add(id);
            ContributionTimeSlot contributionTimeSlot = map.get(timeSlot);
            if (contributionTimeSlot == null) {
                contributionTimeSlot = new ContributionTimeSlot(timeSlot, Thresholds.defaultCommitFilesCountThresholds());
                map.put(timeSlot, contributionTimeSlot);
                list.add(contributionTimeSlot);
            }
            contributionTimeSlot.incrementCommitsCount();
            contributionTimeSlot.setContributorsCount(ids.size());
            contributionTimeSlot.incrementFileUpdatesCount(authorCommit.getFileUpdatesCount());
            contributionTimeSlot.addChurn(authorCommit.getLinesAdded(), authorCommit.getLinesDeleted());
        });

        Collections.sort(list, Comparator.comparing(ContributionTimeSlot::getTimeSlot));

        return list;
    }
}
