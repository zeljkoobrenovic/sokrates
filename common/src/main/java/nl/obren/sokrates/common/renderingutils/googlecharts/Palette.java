package nl.obren.sokrates.common.renderingutils.googlecharts;

import java.util.Arrays;
import java.util.List;

public class Palette {
    private int index = 0;
    private List<String> colors;

    private Palette(List<String> colors) {
        this.colors = colors;
    }

    public static Palette getRiskPalette() {
        return new Palette(Arrays.asList("#F2021B", "#F9CF3F", "#F4DEB5", "#9DC034"));
    }

    public static Palette getDuplicationPalette() {
        return new Palette(Arrays.asList("#9DC034", "#F2021B"));
    }

    public static Palette getDefaultPalette() {
        return new Palette(Arrays.asList("#75ADD2", "#584982", "#FFFD98", "#FCF7FF",
                "#61A99B", "#567E99", "#41365F", "#BAB96F", "#B8B4BA",
                "#B0EFE3", "#A7CAE2", "#948BAF", "#FFFDBD", "#FDF9FF", "#84E7D4"));
    }

    public String nextColor() {
        String color = colors.get(index);
        index++;
        if (index >= colors.size()) {
            index = 0;
        }
        return color;
    }

    public List<String> getColors() {
        return colors;
    }
}
