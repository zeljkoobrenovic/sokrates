package nl.obren.sokrates.sourcecode.dependencies;

import com.kitfox.svg.A;

import java.util.ArrayList;
import java.util.List;

public class ComponentDependency {
    private String fromComponent;
    private int locFrom = 0;
    private List<String> pathsFrom = new ArrayList<>();
    private String toComponent;
    private int count = 1;
    private String text = null;

    public ComponentDependency() {
    }

    public ComponentDependency(String fromComponent, String toComponent) {
        this.fromComponent = fromComponent;
        this.toComponent = toComponent;
    }

    public String getFromComponent() {
        return fromComponent;
    }

    public void setFromComponent(String fromComponent) {
        this.fromComponent = fromComponent;
    }

    public String getToComponent() {
        return toComponent;
    }

    public int getLocFrom() {
        return locFrom;
    }

    public void setLocFrom(int locFrom) {
        this.locFrom = locFrom;
    }

    public List<String> getPathsFrom() {
        return pathsFrom;
    }

    public void setPathsFrom(List<String> pathsFrom) {
        this.pathsFrom = pathsFrom;
    }

    public void setToComponent(String toComponent) {
        this.toComponent = toComponent;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object other) {
        boolean result = false;
        if (other instanceof ComponentDependency) {
            ComponentDependency that = (ComponentDependency) other;
            return this.getDependencyString().equals(that.getDependencyString());
        }
        return result;
    }

    @Override
    public int hashCode() {
        return this.getDependencyString().hashCode();
    }

    public String getDependencyString() {
        return fromComponent + " -> " + toComponent;
    }
}
