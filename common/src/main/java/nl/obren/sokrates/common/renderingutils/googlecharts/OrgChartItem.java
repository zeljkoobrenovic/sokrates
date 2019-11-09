package nl.obren.sokrates.common.renderingutils.googlecharts;

public class OrgChartItem {
    private String item = "";
    private String parent = "";
    private String tooltip = "";

    public OrgChartItem(String item, String parent) {
        this.item = item;
        this.parent = parent;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getTooltip() {
        return tooltip;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }
}
