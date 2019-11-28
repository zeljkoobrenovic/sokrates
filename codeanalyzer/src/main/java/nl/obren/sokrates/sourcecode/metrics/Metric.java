package nl.obren.sokrates.sourcecode.metrics;

public class Metric {

    public enum Scope { SYSTEM, LOGICAL_DECOMPOSITION, LOGICAL_COMPONENT, CROSS_CUTTING_CATEGORY, CROSS_CUTTING_CONCERN, EXTENSION }

    private String id;
    private Scope scope = Scope.SYSTEM;
    private String scopeQualifier;
    private Number value;
    private String description;

    public String getId() {
        return id;
    }

    public Metric id(String name) {
        this.id = name;
        return this;
    }

    public Scope getScope() {
        return scope;
    }

    public Metric scope(Scope scope) {
        this.scope = scope;
        return this;
    }

    public Metric scope(Scope scope, String scopeQualifier) {
        this.scope = scope;
        this.scopeQualifier = scopeQualifier;
        return this;
    }

    public String getScopeQualifier() {
        return scopeQualifier;
    }

    public Metric scopeQualifier(String scopeQualifier) {
        this.scopeQualifier = scopeQualifier;
        return this;
    }

    public Number getValue() {
        return value;
    }

    public Metric value(Number value) {
        this.value = value;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Metric description(String description) {
        this.description = description;
        return this;
    }
}
