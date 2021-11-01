/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import java.io.File;

public interface  ReportRenderingClient {
    void append(String text);
    File getVisualsExportFolder();
}
