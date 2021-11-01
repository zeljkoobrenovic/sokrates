/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.utils;

import nl.obren.sokrates.sourcecode.metrics.NumericMetric;

public class ScopeRendererItem {
    private NumericMetric linesOfCode;
    private NumericMetric filesCount;
    private String filesFragment;

    public void setLinesOfCode(NumericMetric linesOfCode) {
        this.linesOfCode = linesOfCode;
    }

    public NumericMetric getLinesOfCode() {
        return linesOfCode;
    }

    public void setFilesCount(NumericMetric filesCount) {
        this.filesCount = filesCount;
    }

    public NumericMetric getFilesCount() {
        return filesCount;
    }

    public void setFilesFragment(String filesFragment) {
        this.filesFragment = filesFragment;
    }

    public String getFilesFragment() {
        return filesFragment;
    }
}
