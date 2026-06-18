/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.contributors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Contributor {
    public static final int RECENTLY_ACTIVITY_THRESHOLD_DAYS = 30;
    public static final int ACTIVITY_THRESHOLD_DAYS = 180;
    public static final int ROOKIE_THRESHOLD_DAYS = 365;
    private String email = "";
    private String userName = "";
    private int commitsCount = 0;
    private int commitsCount30Days = 0;
    private int fileUpdatesCount30Days = 0;
    // Lines added/deleted across all of this contributor's commits, summed from the per-file churn
    // columns of git-history.txt. 0 for history files without churn data (older exports). The 30-day
    // counters mirror the commitsCount30Days window. Serialized so they survive into the landscape.
    private int linesAdded = 0;
    private int linesDeleted = 0;
    private int linesAdded30Days = 0;
    private int linesDeleted30Days = 0;
    // 90-day and 365-day churn windows, mirroring commitsCount90Days/365Days, so the matrix can show
    // churn under each commits-count window (3m / 1y) as well as 30d and all-time.
    private int linesAdded90Days = 0;
    private int linesDeleted90Days = 0;
    private int linesAdded180Days = 0;
    private int linesDeleted180Days = 0;
    private int linesAdded365Days = 0;
    private int linesDeleted365Days = 0;
    private int commitsCount90Days = 0;
    private int commitsCount180Days = 0;
    private int commitsCount365Days = 0;
    private String firstCommitDate = "";
    private String latestCommitDate = "";
    private List<String> activeYears = new ArrayList<>();
    private List<String> commitDates = new ArrayList<>();
    // Number of commits per date (date -> commit count). commitDates above keeps only the DISTINCT
    // dates (one entry per active day); this retains the per-day commit volume so the activity
    // visuals can be sized by commits rather than by commit days. Serialized so it survives into the
    // landscape; absent in older analysisResults.json (callers fall back to commitDates).
    private Map<String, Integer> commitsPerDate = new LinkedHashMap<>();
    // Lines added/deleted per date (date -> total), mirroring commitsPerDate, so the contributors
    // matrix can total line churn per month. Serialized; absent/empty for histories without churn.
    private Map<String, Integer> linesAddedPerDate = new LinkedHashMap<>();
    private Map<String, Integer> linesDeletedPerDate = new LinkedHashMap<>();
    // O(1) companion sets for the (serialized) lists above, so addCommit stays O(1) per commit
    // instead of scanning the growing lists. Kept in sync with commitDates/activeYears.
    @JsonIgnore
    private Set<String> commitDatesSet = new HashSet<>();
    @JsonIgnore
    private Set<String> activeYearsSet = new TreeSet<>();

    private boolean bot = false;

    public Contributor() {
    }

    public Contributor(String email) {
        this.email = email;
    }

    @JsonIgnore
    public void addCommit(String date, int fileUpdatesCount) {
        addCommit(date, fileUpdatesCount, 0, 0);
    }

    @JsonIgnore
    public void addCommit(String date, int fileUpdatesCount, int commitLinesAdded, int commitLinesDeleted) {
        if (commitDatesSet.add(date)) {
            commitDates.add(date);
        }
        commitsPerDate.merge(date, 1, Integer::sum);
        if (commitLinesAdded != 0) {
            linesAddedPerDate.merge(date, commitLinesAdded, Integer::sum);
        }
        if (commitLinesDeleted != 0) {
            linesDeletedPerDate.merge(date, commitLinesDeleted, Integer::sum);
        }
        if (StringUtils.isBlank(firstCommitDate) || date.compareTo(firstCommitDate) < 0) {
            firstCommitDate = date;
        }
        if (StringUtils.isBlank(latestCommitDate) || date.compareTo(latestCommitDate) > 0) {
            latestCommitDate = date;
        }
        linesAdded += commitLinesAdded;
        linesDeleted += commitLinesDeleted;
        if (date.length() > 4) {
            String year = date.substring(0, 4);
            if (activeYearsSet.add(year)) {
                // activeYearsSet is a TreeSet, so rebuild the sorted list only when a new year
                // appears instead of re-sorting on every commit.
                activeYears.clear();
                activeYears.addAll(activeYearsSet);
            }

            if (DateUtils.isCommittedLessThanDaysAgo(date, RECENTLY_ACTIVITY_THRESHOLD_DAYS)) {
                commitsCount30Days += 1;
                fileUpdatesCount30Days += fileUpdatesCount;
                linesAdded30Days += commitLinesAdded;
                linesDeleted30Days += commitLinesDeleted;
            }
            if (DateUtils.isCommittedLessThanDaysAgo(date, 90)) {
                commitsCount90Days += 1;
                linesAdded90Days += commitLinesAdded;
                linesDeleted90Days += commitLinesDeleted;
            }
            if (DateUtils.isCommittedLessThanDaysAgo(date, 180)) {
                commitsCount180Days += 1;
                linesAdded180Days += commitLinesAdded;
                linesDeleted180Days += commitLinesDeleted;
            }
            if (DateUtils.isCommittedLessThanDaysAgo(date, 365)) {
                commitsCount365Days += 1;
                linesAdded365Days += commitLinesAdded;
                linesDeleted365Days += commitLinesDeleted;
            }
        }

        commitsCount += 1;
    }

    // Merges another contributor's distinct commit dates into this one in O(n) using the companion
    // set, instead of an O(n) contains() scan per date.
    @JsonIgnore
    public void addCommitDates(List<String> dates) {
        dates.forEach(date -> {
            if (commitDatesSet.add(date)) {
                commitDates.add(date);
            }
        });
    }

    // Merges another contributor's per-date commit counts into this one, summing counts for shared
    // dates. Keeps commitsPerDate consistent across the landscape merges that combine a contributor's
    // activity from several repositories.
    @JsonIgnore
    public void addCommitsPerDate(Map<String, Integer> other) {
        if (other != null) {
            other.forEach((date, count) -> commitsPerDate.merge(date, count, Integer::sum));
        }
    }

    // Merges another contributor's per-date line churn into this one, summing per date. Keeps the
    // per-date churn maps consistent across the landscape merges (mirrors addCommitsPerDate).
    @JsonIgnore
    public void addLinesPerDate(Map<String, Integer> otherAdded, Map<String, Integer> otherDeleted) {
        if (otherAdded != null) {
            otherAdded.forEach((date, count) -> linesAddedPerDate.merge(date, count, Integer::sum));
        }
        if (otherDeleted != null) {
            otherDeleted.forEach((date, count) -> linesDeletedPerDate.merge(date, count, Integer::sum));
        }
    }

    // Merges another contributor's active years, keeping them distinct and sorted.
    @JsonIgnore
    public void addActiveYears(List<String> years) {
        if (activeYearsSet.addAll(years)) {
            activeYears.clear();
            activeYears.addAll(activeYearsSet);
        }
    }

    public boolean isActive() {
        return isActive(ACTIVITY_THRESHOLD_DAYS);
    }

    public boolean isActive(int rangeInDays) {
        return DateUtils.isDateWithinRange(latestCommitDate, rangeInDays);
    }

    public boolean isRookie() {
        return isRookie(ACTIVITY_THRESHOLD_DAYS);
    }

    @JsonIgnore
    public boolean isRookieAtDate(String date) {
        String elements[] = date.split("-");
        if (elements.length >= 3) {
            Calendar cal = DateUtils.getCalendar(date);
            cal.add(Calendar.YEAR, -1);

            String rookieStartDate = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
            return firstCommitDate.compareTo(rookieStartDate) >= 0;
        }
        return false;
    }

    @JsonIgnore
    public boolean isRookie(int activityThreshold) {
        if (StringUtils.isBlank(firstCommitDate) || !isActive(activityThreshold)) {
            return false;
        }

        Calendar cal = DateUtils.getCalendar();
        cal.add(Calendar.DATE, -ROOKIE_THRESHOLD_DAYS);

        String thresholdDate = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());

        return firstCommitDate.compareTo(thresholdDate) > 0;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getCommitsCount() {
        return commitsCount;
    }

    public void setCommitsCount(int commitsCount) {
        this.commitsCount = commitsCount;
    }

    public String getFirstCommitDate() {
        return firstCommitDate;
    }

    public void setFirstCommitDate(String firstCommitDate) {
        this.firstCommitDate = firstCommitDate;
    }

    public String getLatestCommitDate() {
        return latestCommitDate;
    }

    public void setLatestCommitDate(String latestCommitDate) {
        this.latestCommitDate = latestCommitDate;
    }

    public List<String> getActiveYears() {
        return activeYears;
    }

    public void setActiveYears(List<String> activeYears) {
        this.activeYears = activeYears;
        this.activeYearsSet = new TreeSet<>(activeYears);
    }

    public int getCommitsCount30Days() {
        return commitsCount30Days;
    }

    public void setCommitsCount30Days(int commitsCount30Days) {
        this.commitsCount30Days = commitsCount30Days;
    }

    public int getFileUpdatesCount30Days() {
        return fileUpdatesCount30Days;
    }

    public void setFileUpdatesCount30Days(int fileUpdatesCount30Days) {
        this.fileUpdatesCount30Days = fileUpdatesCount30Days;
    }

    public int getLinesAdded() {
        return linesAdded;
    }

    public void setLinesAdded(int linesAdded) {
        this.linesAdded = linesAdded;
    }

    public int getLinesDeleted() {
        return linesDeleted;
    }

    public void setLinesDeleted(int linesDeleted) {
        this.linesDeleted = linesDeleted;
    }

    public int getLinesAdded30Days() {
        return linesAdded30Days;
    }

    public void setLinesAdded30Days(int linesAdded30Days) {
        this.linesAdded30Days = linesAdded30Days;
    }

    public int getLinesDeleted30Days() {
        return linesDeleted30Days;
    }

    public void setLinesDeleted30Days(int linesDeleted30Days) {
        this.linesDeleted30Days = linesDeleted30Days;
    }

    public int getLinesAdded90Days() {
        return linesAdded90Days;
    }

    public void setLinesAdded90Days(int linesAdded90Days) {
        this.linesAdded90Days = linesAdded90Days;
    }

    public int getLinesDeleted90Days() {
        return linesDeleted90Days;
    }

    public void setLinesDeleted90Days(int linesDeleted90Days) {
        this.linesDeleted90Days = linesDeleted90Days;
    }

    public int getLinesAdded180Days() {
        return linesAdded180Days;
    }

    public void setLinesAdded180Days(int linesAdded180Days) {
        this.linesAdded180Days = linesAdded180Days;
    }

    public int getLinesDeleted180Days() {
        return linesDeleted180Days;
    }

    public void setLinesDeleted180Days(int linesDeleted180Days) {
        this.linesDeleted180Days = linesDeleted180Days;
    }

    public int getLinesAdded365Days() {
        return linesAdded365Days;
    }

    public void setLinesAdded365Days(int linesAdded365Days) {
        this.linesAdded365Days = linesAdded365Days;
    }

    public int getLinesDeleted365Days() {
        return linesDeleted365Days;
    }

    public void setLinesDeleted365Days(int linesDeleted365Days) {
        this.linesDeleted365Days = linesDeleted365Days;
    }

    // Merges another contributor's churn totals into this one, used when the landscape combines a
    // contributor's activity across repositories (mirrors addCommitDates/addCommitsPerDate).
    @JsonIgnore
    public void addChurn(int added, int deleted, int added30Days, int deleted30Days,
                         int added90Days, int deleted90Days, int added180Days, int deleted180Days,
                         int added365Days, int deleted365Days) {
        this.linesAdded += added;
        this.linesDeleted += deleted;
        this.linesAdded30Days += added30Days;
        this.linesDeleted30Days += deleted30Days;
        this.linesAdded90Days += added90Days;
        this.linesDeleted90Days += deleted90Days;
        this.linesAdded180Days += added180Days;
        this.linesDeleted180Days += deleted180Days;
        this.linesAdded365Days += added365Days;
        this.linesDeleted365Days += deleted365Days;
    }

    public int getCommitsCount90Days() {
        return commitsCount90Days;
    }

    public void setCommitsCount90Days(int commitsCount90Days) {
        this.commitsCount90Days = commitsCount90Days;
    }

    public int getCommitsCount180Days() {
        return commitsCount180Days;
    }

    public void setCommitsCount180Days(int commitsCount180Days) {
        this.commitsCount180Days = commitsCount180Days;
    }

    public int getCommitsCount365Days() {
        return commitsCount365Days;
    }

    public void setCommitsCount365Days(int commitsCount365Days) {
        this.commitsCount365Days = commitsCount365Days;
    }

    public List<String> getCommitDates() {
        return commitDates;
    }

    public void setCommitDates(List<String> commitDates) {
        this.commitDates = commitDates;
        this.commitDatesSet = new HashSet<>(commitDates);
    }

    public Map<String, Integer> getCommitsPerDate() {
        return commitsPerDate;
    }

    public void setCommitsPerDate(Map<String, Integer> commitsPerDate) {
        this.commitsPerDate = commitsPerDate != null ? commitsPerDate : new LinkedHashMap<>();
    }

    public Map<String, Integer> getLinesAddedPerDate() {
        return linesAddedPerDate;
    }

    public void setLinesAddedPerDate(Map<String, Integer> linesAddedPerDate) {
        this.linesAddedPerDate = linesAddedPerDate != null ? linesAddedPerDate : new LinkedHashMap<>();
    }

    public Map<String, Integer> getLinesDeletedPerDate() {
        return linesDeletedPerDate;
    }

    public void setLinesDeletedPerDate(Map<String, Integer> linesDeletedPerDate) {
        this.linesDeletedPerDate = linesDeletedPerDate != null ? linesDeletedPerDate : new LinkedHashMap<>();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Contributor)) {
            return false;
        }

        Contributor contributor = (Contributor) obj;

        return contributor.getEmail().equalsIgnoreCase(this.getEmail());
    }

    public boolean isBot() {
        return bot;
    }

    public void setBot(boolean bot) {
        this.bot = bot;
    }
}
