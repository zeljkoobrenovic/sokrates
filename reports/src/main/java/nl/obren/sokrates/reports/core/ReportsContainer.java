/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import java.util.ArrayList;
import java.util.List;

public class ReportsContainer {
    private List<RichTextReport> richTextReports = new ArrayList<>();

    public ReportsContainer() {
    }

    public ReportsContainer(List<RichTextReport> richTextReports) {
        this.richTextReports = richTextReports;
    }

    public List<RichTextReport> getRichTextReports() {
        return richTextReports;
    }

    public void setRichTextReports(List<RichTextReport> richTextReports) {
        this.richTextReports = richTextReports;
    }
}
