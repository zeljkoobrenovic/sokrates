/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.scoping.custom;

import java.util.ArrayList;
import java.util.List;

public class CustomExtensionConventions {
    private List<String> onlyInclude = new ArrayList<>();
    private List<String> alwaysExclude = new ArrayList<>();

    public List<String> getOnlyInclude() {
        return onlyInclude;
    }

    public void setOnlyInclude(List<String> onlyInclude) {
        this.onlyInclude = onlyInclude;
    }

    public List<String> getAlwaysExclude() {
        return alwaysExclude;
    }

    public void setAlwaysExclude(List<String> alwaysExclude) {
        this.alwaysExclude = alwaysExclude;
    }
}
