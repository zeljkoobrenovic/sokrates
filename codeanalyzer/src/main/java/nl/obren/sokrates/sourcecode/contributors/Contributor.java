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
import java.util.List;
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
    private int commitsCount90Days = 0;
    private int commitsCount180Days = 0;
    private int commitsCount365Days = 0;
    private String firstCommitDate = "";
    private String latestCommitDate = "";
    private List<String> activeYears = new ArrayList<>();
    private List<String> commitDates = new ArrayList<>();
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
        if (commitDatesSet.add(date)) {
            commitDates.add(date);
        }
        if (StringUtils.isBlank(firstCommitDate) || date.compareTo(firstCommitDate) < 0) {
            firstCommitDate = date;
        }
        if (StringUtils.isBlank(latestCommitDate) || date.compareTo(latestCommitDate) > 0) {
            latestCommitDate = date;
        }
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
            }
            if (DateUtils.isCommittedLessThanDaysAgo(date, 90)) {
                commitsCount90Days += 1;
            }
            if (DateUtils.isCommittedLessThanDaysAgo(date, 180)) {
                commitsCount180Days += 1;
            }
            if (DateUtils.isCommittedLessThanDaysAgo(date, 365)) {
                commitsCount365Days += 1;
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
