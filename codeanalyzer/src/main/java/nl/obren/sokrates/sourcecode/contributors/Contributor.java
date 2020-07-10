/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.contributors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Contributor {
    private String name = "";
    private int commitsCount = 0;
    private String firstCommitDate = "";
    private String latestCommitDate = "";

    public Contributor() {
    }

    public Contributor(String name) {
        this.name = name;
    }

    public Contributor(String name, int commitsCount) {
        this.name = name;
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

        commitsCount += 1;
    }

    public boolean isActive() {
        if (StringUtils.isBlank(latestCommitDate)) {
            return true;
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -180);

        String thresholdDate = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());

        return latestCommitDate.compareTo(thresholdDate) > 0;
    }

    public boolean isRookie() {
        if (StringUtils.isBlank(firstCommitDate)) {
            return false;
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -180);

        String thresholdDate = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());

        return firstCommitDate.compareTo(thresholdDate) > 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
