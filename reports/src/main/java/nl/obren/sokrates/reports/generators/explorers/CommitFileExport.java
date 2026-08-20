package nl.obren.sokrates.reports.generators.explorers;

/**
 * One file referenced by the commits explorer (commits-explorer.html). Files currently in the
 * codebase carry their scope ("main"/"test"/"generated"/"build"/"other") and lines of code; paths
 * that appear in the git history but match no current file (deleted, renamed away, or excluded
 * from every scope) get the special scope {@link #SCOPE_DELETED} and a size PROXY as lines of
 * code (the largest single-commit lines-added/deleted seen for the path). Commits reference
 * files by their index in the exported list, so each path is embedded only once.
 */
public class CommitFileExport {
    public static final String SCOPE_DELETED = "deleted";

    private String path = "";
    private String scope = "";
    private int linesOfCode = 0;

    public CommitFileExport() {
    }

    public CommitFileExport(String path, String scope, int linesOfCode) {
        this.path = path;
        this.scope = scope;
        this.linesOfCode = linesOfCode;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public int getLinesOfCode() {
        return linesOfCode;
    }

    public void setLinesOfCode(int linesOfCode) {
        this.linesOfCode = linesOfCode;
    }
}
