/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.sourcecode.landscape.init.LandscapeAnalysisInitiator;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzer;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class LandscapeAnalyzerTest {

    @Ignore
    @Test
    public void analyze() throws JsonProcessingException {
        LandscapeAnalyzer analyzer = new LandscapeAnalyzer();

        File landscapeConfigFile = new File("");

        LandscapeAnalysisResults analyze = analyzer.analyze(landscapeConfigFile);

        // System.out.println(new JsonGenerator().generate(analyze));
    }
}
