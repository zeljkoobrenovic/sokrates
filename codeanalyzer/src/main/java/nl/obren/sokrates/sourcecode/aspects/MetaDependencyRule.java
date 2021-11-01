package nl.obren.sokrates.sourcecode.aspects;

public class MetaDependencyRule extends MetaRule {
    // If true, identified dependency will be visualized with an arrow from target to source
    private boolean reverseDirection = false;

    // An optional link color used in dependency diagrams
    private String color = "";

    public MetaDependencyRule() {
    }

    public MetaDependencyRule(String pathPattern, String contentPattern, String use) {
        super(pathPattern, contentPattern, use);
    }

    public boolean isReverseDirection() {
        return reverseDirection;
    }

    public void setReverseDirection(boolean reverseDirection) {
        this.reverseDirection = reverseDirection;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
