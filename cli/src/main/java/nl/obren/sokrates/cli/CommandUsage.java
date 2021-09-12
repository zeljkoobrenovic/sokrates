package nl.obren.sokrates.cli;

import org.apache.commons.cli.Options;

public class CommandUsage {
    private String name;
    private String description;
    private Options options;

    public CommandUsage() {
    }

    public CommandUsage(String name, String description, Options options) {
        this.name = name;
        this.description = description;
        this.options = options;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }
}
