/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.contributors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Contributor {
    private String name = "";
    private String email = "";
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
        if (StringUtils.isBlank(firstCommitDate) || !isActive()) {
            return false;
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -365);

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
}
