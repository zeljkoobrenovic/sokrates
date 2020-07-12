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
    public static final int ACTIVITY_THRESHOLD_DAYS = 180;
    public static final int ROOKY_THRESHOLD_DAYS = 365;
    private String name = "";
    private String email = "";
    private int commitsCount = 0;
    private String firstCommitDate = "";
    private String latestCommitDate = "";
    private List<String> activeYears = new ArrayList<>();

    public Contributor() {
    }

    public Contributor(String name) {
        this.name = name;
    }

    public Contributor(String name, int commitsCount) {
        this.name = name;
        this.commitsCount = commitsCount;
    }

    public static Contributor getInstanceFromNameEmailLine(String line) {
        Contributor contributor = new Contributor();

        int n1 = line.lastIndexOf("<");
        int n2 = line.lastIndexOf(">");

        if (n1 > 0 && n2 > n1) {
            contributor.setName(line.substring(0, n1).trim());
            contributor.setEmail(line.substring(n1 + 1, n2).trim());
        } else {
            contributor.setName(line);
            contributor.setEmail("");
        }

        return contributor;
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
        }

        commitsCount += 1;
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
        cal.add(Calendar.DATE, -ROOKY_THRESHOLD_DAYS);

        String thresholdDate = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());

        return firstCommitDate.compareTo(thresholdDate) > 0;
    }

    @JsonIgnore
    public String getId() {
        return StringUtils.isNotBlank(email) ? email : name;
    }

    @JsonPropertyOrder
    public String getDisplayName() {
        return name + (StringUtils.isNotBlank(email) ? " <" + email + ">" : "");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
