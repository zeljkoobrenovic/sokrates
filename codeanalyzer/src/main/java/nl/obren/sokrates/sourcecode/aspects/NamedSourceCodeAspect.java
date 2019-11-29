package nl.obren.sokrates.sourcecode.aspects;

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
}
