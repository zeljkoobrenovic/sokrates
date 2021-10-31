/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.renderingutils.force3d;

import java.util.ArrayList;
import java.util.List;

public class Force3DLink {
    private String source;
    private String target;
    private int count;

    public Force3DLink() {
    }

    public Force3DLink(String source, String target, int count) {
        this.source = source;
        this.target = target;
        this.count = count;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
