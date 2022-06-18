package nl.obren.sokrates.sourcecode.landscape.analysis;

public class ContributorConnections {
    private String email = "";
    private int repositoriesCount;
    private int connectionsCount;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getRepositoriesCount() {
        return repositoriesCount;
    }

    public void setRepositoriesCount(int repositoriesCount) {
        this.repositoriesCount = repositoriesCount;
    }

    public int getConnectionsCount() {
        return connectionsCount;
    }

    public void setConnectionsCount(int connectionsCount) {
        this.connectionsCount = connectionsCount;
    }
}