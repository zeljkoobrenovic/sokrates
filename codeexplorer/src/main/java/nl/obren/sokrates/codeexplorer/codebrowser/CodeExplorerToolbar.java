/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.codebrowser;

import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import nl.obren.sokrates.codeexplorer.common.SvgIcons;
import nl.obren.sokrates.codeexplorer.common.UXUtils;

public class CodeExplorerToolbar extends ToolBar {
    public CodeExplorerToolbar(CodeBrowserPane codeBrowserPane) {
        getItems().add(UXUtils.getButton(SvgIcons.SVG_NEW, e -> codeBrowserPane.getCodeConfigurationView().newConfiguration(), "New"));
        getItems().add(UXUtils.getButton(SvgIcons.SVG_OPEN, e -> codeBrowserPane.getCodeConfigurationView().openConfiguration(), "Open..."));
        getItems().add(new Separator());
        getItems().add(UXUtils.getButton(SvgIcons.SVG_COMPLETE, e -> codeBrowserPane.getCodeConfigurationView().completeAndSave(), "Save & Complete"));
        getItems().add(new Separator());
        getItems().add(UXUtils.getButton(SvgIcons.SVG_REFRESH, e -> {
            codeBrowserPane.getCodeConfigurationView().completeAndSave();
            codeBrowserPane.load();
        }, "Refresh files"));
        getItems().add(new Separator());
        getItems().add(UXUtils.getButton(SvgIcons.SVG_RUN, e -> {
            codeBrowserPane.getCodeConfigurationView().generateFullReport();
        }, "Generate all reports"));
    }
}
