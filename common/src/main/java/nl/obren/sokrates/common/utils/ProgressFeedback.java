/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.utils;

public class ProgressFeedback {
    public void clear() {}
    public void start() {}
    public void end() {}
    public void setText(String text) {}
    public void setDetailedText(String text) {}
    public boolean canceled() {
        return false;
    }

    public void progress(int currentValue, int endValue) {
    }
}
