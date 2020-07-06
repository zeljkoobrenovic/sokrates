/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.renderingutils.charts;

import java.util.Arrays;
import java.util.List;

// https://colorbrewer2.org/#type=sequential&scheme=BuGn&n=5

public class Palette {
    private int index = 0;
    private List<String> colors;

    private Palette(List<String> colors) {
        this.colors = colors;
    }

    public static Palette getRiskPalette() {
        return new Palette(Arrays.asList("#d7191c", "#fdae61", "#ffffbf", "#a6d96a", "#1a9641"));
    }

    public static Palette getHeatPalette() {
        return new Palette(Arrays.asList("#bd0026", "#f03b20", "#fd8d3c", "#fed98e", "#ffffd4"));
    }

    public static Palette getAgePalette() {
        return new Palette(Arrays.asList("#252525", "#636363", "#969696", "#cccccc", "#f7f7f7"));
    }

    public static Palette getFreshnessPalette() {
        return new Palette(Arrays.asList( "#252525", "#006d2c", "#31a354", "#74c476", "#bae4b3"));
    }

    public static Palette getDuplicationPalette() {
        return new Palette(Arrays.asList("#9DC034", "#F2021B"));
    }

    public static Palette getDefaultPalette() {
        return new Palette(Arrays.asList("#75ADD2", "#584982", "#FFFD98", "#FCF7FF",
                "#61A99B", "#567E99", "#41365F", "#BAB96F", "#B8B4BA",
                "#B0EFE3", "#A7CAE2", "#948BAF", "#FFFDBD", "#FDF9FF", "#84E7D4"));
        //return new Palette(Arrays.asList("grey", "lightgrey"));
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
