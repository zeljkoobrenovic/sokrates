/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.renderingutils.x3d;

import nl.obren.sokrates.common.utils.BasicColorInfo;

public class Unit3D {
    public String name = "";
    public Number value;
    private BasicColorInfo color;

    public Unit3D(String name, Number value, BasicColorInfo color) {
        this.name = name;
        this.value = value;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Number getValue() {
        return value;
    }

    public void setValue(Number value) {
        this.value = value;
    }

    public BasicColorInfo getColor() {
        return color;
    }

    public void setColor(BasicColorInfo color) {
        this.color = color;
    }
}
