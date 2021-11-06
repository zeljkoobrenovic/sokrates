/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.scoping.custom;

import java.util.ArrayList;
import java.util.List;

public class CustomExtensionConventions {
    // A list of file extensions to be included in the analysis (all other extensions are ignored)
    private List<String> onlyInclude = new ArrayList<>();

    // A list of file extensions to always be excluded from analyses
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
