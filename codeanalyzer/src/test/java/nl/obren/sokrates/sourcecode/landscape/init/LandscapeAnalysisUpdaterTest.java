/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.init;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

public class LandscapeAnalysisUpdaterTest {

    @Ignore
    @Test
    public void updateConfiguration() {
        LandscapeAnalysisUpdater updater = new LandscapeAnalysisUpdater();

        File landscapeConfigFile = new File("");
        File analysisRoot = new File("");

        updater.updateConfiguration(analysisRoot, null);
    }
}
