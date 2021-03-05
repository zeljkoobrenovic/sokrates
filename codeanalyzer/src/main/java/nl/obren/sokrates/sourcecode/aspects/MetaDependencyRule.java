package nl.obren.sokrates.sourcecode.aspects;

public class MetaDependencyRule extends MetaRule {
    private boolean reverseDirection = false;
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
