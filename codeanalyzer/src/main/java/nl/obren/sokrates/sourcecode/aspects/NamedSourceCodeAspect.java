package nl.obren.sokrates.sourcecode.aspects;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
    public String getFileSystemFriendlyName() {
        StringBuilder stringBuilder = new StringBuilder();

        name.chars().forEach(i -> {
            char c = (char) i;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c >= '0' && c <= '9')
                stringBuilder.append(c);
            else
                stringBuilder.append('_');
        });
        return stringBuilder.toString();
    }
}
