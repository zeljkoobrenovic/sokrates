package nl.obren.sokrates.sourcecode.dependencies;

import java.util.ArrayList;
import java.util.List;

public class DependenciesAnalysis {
    private List<Dependency> dependencies = new ArrayList<>();
    private List<DependencyError> errors = new ArrayList<>();

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    public List<DependencyError> getErrors() {
        return errors;
    }

    public void setErrors(List<DependencyError> errors) {
        this.errors = errors;
    }
}
