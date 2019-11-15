package nl.obren.sokrates.sourcecode.dependencies;

public class DependencyError {
    private String message;
    private String filtering;

    public DependencyError() {
    }

    public DependencyError(String message, String filtering) {
        this.message = message;
        this.filtering = filtering;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFiltering() {
        return filtering;
    }

    public void setFiltering(String filtering) {
        this.filtering = filtering;
    }
}
