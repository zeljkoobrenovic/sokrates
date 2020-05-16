/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.init;

import nl.obren.sokrates.sourcecode.landscape.init.LandscapeAnalysisInitiator;
import org.junit.Test;

import java.io.File;

public class LandscapeAnalysisInitiatorTest {

    @Test
    public void initConfiguration() {
        LandscapeAnalysisInitiator initiator = new LandscapeAnalysisInitiator();

        File landscapeConfigFile = new File("/Users/zeljkoobrenovic/Documents/workspace/sokrates-gallery/_sokrates_landscape/config.json");
        File analysisRoot = new File("/Users/zeljkoobrenovic/Documents/workspace/sokrates-gallery/ubeross");

        initiator.initConfiguration(analysisRoot, null, true);
    }
}
