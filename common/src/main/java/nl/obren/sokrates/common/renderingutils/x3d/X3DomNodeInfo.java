package nl.obren.sokrates.common.renderingutils.x3d;

import nl.obren.sokrates.common.utils.BasicColorInfo;

import java.io.File;

public class X3DomNodeInfo {
    private String name;
    private int size;
    private BasicColorInfo color = new BasicColorInfo(0.0, 0.0, 1.0);
    private File file = new File("");
    private String component;
    private String info;

    X3DomNodeInfo(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getColor() {
        return color.getRed() / 1.0 + " " + color.getGreen() / 1.0 + " " + color.getBlue() / 1.0;
    }

    public void setColor(BasicColorInfo color) {
        this.color = color;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
