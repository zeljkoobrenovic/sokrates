package nl.obren.sokrates.sourcecode.aspects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.common.utils.SystemUtils;
import org.apache.commons.lang3.StringUtils;

public class NamedSourceCodeAspect extends SourceCodeAspect {
    private String name = "";

    public NamedSourceCodeAspect() {
    }

    public NamedSourceCodeAspect(String name) {
        this();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public String getFileSystemFriendlyName(String prefix) {
        return SystemUtils.getFileSystemFriendlyName(prefix + name);
    }
}
