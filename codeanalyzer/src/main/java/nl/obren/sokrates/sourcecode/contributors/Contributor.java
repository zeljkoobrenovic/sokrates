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
import java.util.Collections;
import java.util.List;

public class Contributor {
    public static final int RECENTLY_ACTIVITY_THRESHOLD_DAYS = 31;
    public static final int ACTIVITY_THRESHOLD_DAYS = 180;
    public static final int ROOKIE_THRESHOLD_DAYS = 365;
    private String email = "";
    private int commitsCount = 0;
    private int commitsCount30Days = 0;
    private int commitsCount90Days = 0;
    private int commitsCount180Days = 0;
    private int commitsCount365Days = 0;
    private String firstCommitDate = "";
    private String latestCommitDate = "";
    private List<String> activeYears = new ArrayList<>();
    private List<String> commitDates = new ArrayList<>();

    public Contributor() {
    }

    public Contributor(String email) {
        this.email = email;
    }

    @JsonIgnore
    public void addCommit(String date) {
        if (!commitDates.contains(date)) {
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
            if (!activeYears.contains(year)) {
                activeYears.add(year);
            }
            Collections.sort(activeYears);

            if (DateUtils.isCommittedLessThanDaysAgo(date, RECENTLY_ACTIVITY_THRESHOLD_DAYS)) {
                commitsCount30Days += 1;
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
    }

    public int getCommitsCount30Days() {
        return commitsCount30Days;
    }

    public void setCommitsCount30Days(int commitsCount30Days) {
        this.commitsCount30Days = commitsCount30Days;
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
    }
}
