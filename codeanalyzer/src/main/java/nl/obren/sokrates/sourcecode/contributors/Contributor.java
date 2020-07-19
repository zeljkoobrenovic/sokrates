/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.contributors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class Contributor {
    public static final int RECENTLY_ACTIVITY_THRESHOLD_DAYS = 30;
    public static final int ACTIVITY_THRESHOLD_DAYS = 180;
    public static final int ROOKIE_THRESHOLD_DAYS = 365;
    private String email = "";
    private int commitsCount = 0;
    private int commitsCount30Days = 0;
    private int commitsCount90Days = 0;
    private String firstCommitDate = "";
    private String latestCommitDate = "";
    private List<String> activeYears = new ArrayList<>();

    public Contributor() {
    }

    public Contributor(String email) {
        this.email = email;
    }

    public Contributor(String email, int commitsCount) {
        this.email = email;
        this.commitsCount = commitsCount;
    }

    @JsonIgnore
    public void addCommit(String date) {
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

            if (isCommitedLessThanDaysAgo(date, RECENTLY_ACTIVITY_THRESHOLD_DAYS)) {
                commitsCount30Days += 1;
            }
            if (isCommitedLessThanDaysAgo(date, 90)) {
                commitsCount90Days += 1;
            }
        }

        commitsCount += 1;
    }

    private boolean isCommitedLessThanDaysAgo(String date, int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -daysAgo);

        String thresholdDate = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());

        return date.compareTo(thresholdDate) > 0;
    }

    public boolean isActive() {
        return isActive(ACTIVITY_THRESHOLD_DAYS);
    }

    @JsonIgnore
    public boolean isActive(int threshold) {
        if (StringUtils.isBlank(latestCommitDate)) {
            return true;
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -threshold);

        String thresholdDate = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());

        return latestCommitDate.compareTo(thresholdDate) > 0;
    }

    public boolean isRookie() {
        return isRookie(ACTIVITY_THRESHOLD_DAYS);
    }

    @JsonIgnore
    public boolean isRookie(int activityThreshold) {
        if (StringUtils.isBlank(firstCommitDate) || !isActive(activityThreshold)) {
            return false;
        }

        Calendar cal = Calendar.getInstance();
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
}
