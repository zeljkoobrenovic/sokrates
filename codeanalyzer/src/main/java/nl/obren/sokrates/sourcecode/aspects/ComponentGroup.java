package nl.obren.sokrates.sourcecode.aspects;

import java.util.ArrayList;
import java.util.List;

public class ComponentGroup {
    private String name;
    private List<String> componentNames = new ArrayList<>();

    public ComponentGroup() {
    }

    public ComponentGroup(String name, List<String> componentNames) {
        this.name = name;
        this.componentNames = componentNames;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getComponentNames() {
        return componentNames;
    }

    public void setComponentNames(List<String> componentNames) {
        this.componentNames = componentNames;
    }
}
