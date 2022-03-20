package nl.obren.sokrates.common.renderingutils;

public class RacingChartItem {
    private String name = "";
    private double year;
    private double value;

    public RacingChartItem() {
    }

    public RacingChartItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getYear() {
        return year;
    }

    public void setYear(double year) {
        this.year = year;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
