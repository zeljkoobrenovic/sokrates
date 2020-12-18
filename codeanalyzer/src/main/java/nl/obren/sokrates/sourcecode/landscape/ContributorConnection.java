package nl.obren.sokrates.sourcecode.landscape;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public class ContributorConnection {
    private String email;
    private int count;
    private int commits;

    public ContributorConnection() {
    }

    public ContributorConnection(String email, int count, int commits) {
        this.email = email;
        this.count = count;
        this.commits = commits;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCommits() {
        return commits;
    }

    public void setCommits(int commits) {
        this.commits = commits;
    }


}