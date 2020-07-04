/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.init;

import nl.obren.sokrates.sourcecode.landscape.init.LandscapeAnalysisInitiator;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

public class LandscapeAnalysisInitiatorTest {

    @Ignore
    @Test
    public void initConfiguration() {
        LandscapeAnalysisInitiator initiator = new LandscapeAnalysisInitiator();

        File analysisRoot = new File("/Users/zobrenovic/Downloads/github/move/sokrates-batch/sokrates-initial-reports");

        initiator.initConfiguration(analysisRoot, null, true);
    }
}
