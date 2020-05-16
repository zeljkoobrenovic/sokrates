/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.init;

import nl.obren.sokrates.sourcecode.landscape.init.LandscapeAnalysisUpdater;
import org.junit.Test;

import java.io.File;

public class LandscapeAnalysisUpdaterTest {

    @Test
    public void updateConfiguration() {
        LandscapeAnalysisUpdater updater = new LandscapeAnalysisUpdater();

        File landscapeConfigFile = new File("/Users/zeljkoobrenovic/Documents/workspace/sokrates-gallery/_sokrates_landscape/config.json");
        File analysisRoot = new File("/Users/zeljkoobrenovic/Documents/workspace/sokrates-gallery/ubeross");

        updater.updateConfiguration(analysisRoot, null);
    }
}
