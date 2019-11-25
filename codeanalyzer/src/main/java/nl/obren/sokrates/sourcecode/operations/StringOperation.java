package nl.obren.sokrates.sourcecode.operations;

import java.util.ArrayList;
import java.util.List;

public abstract class StringOperation {
    private String name = "";
    private List<String> params = new ArrayList<>();

    public StringOperation(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getParams() {
        return params;
    }

    public void setParams(List<String> params) {
        this.params = params;
    }

    public abstract String exec(String input);
}
