/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import nl.obren.sokrates.sourcecode.stats.SourceFileSizeDistribution;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SourceFileSizeDistributionTest {
    @Test
    public void calculate() throws Exception {
        SourceFileSizeDistribution sourceFileSizeDistribution = new SourceFileSizeDistribution();

        sourceFileSizeDistribution.getOverallDistribution(getSourceFile(2000));

        assertEquals(sourceFileSizeDistribution.getLowRiskValue(), 0);
        assertEquals(sourceFileSizeDistribution.getMediumRiskValue(), 0);
        assertEquals(sourceFileSizeDistribution.getHighRiskValue(), 0);
        assertEquals(sourceFileSizeDistribution.getVeryHighRiskValue(), 2000);

        sourceFileSizeDistribution.getOverallDistribution(getSourceFile(1000));

        assertEquals(sourceFileSizeDistribution.getLowRiskValue(), 0);
        assertEquals(sourceFileSizeDistribution.getMediumRiskValue(), 0);
        assertEquals(sourceFileSizeDistribution.getHighRiskValue(), 1000);
        assertEquals(sourceFileSizeDistribution.getVeryHighRiskValue(), 0);

        sourceFileSizeDistribution.getOverallDistribution(getSourceFile(500));

        assertEquals(sourceFileSizeDistribution.getLowRiskValue(), 0);
        assertEquals(sourceFileSizeDistribution.getMediumRiskValue(), 500);
        assertEquals(sourceFileSizeDistribution.getHighRiskValue(), 0);
        assertEquals(sourceFileSizeDistribution.getVeryHighRiskValue(), 0);

        sourceFileSizeDistribution.getOverallDistribution(getSourceFile(100));

        assertEquals(sourceFileSizeDistribution.getNegligibleRiskValue(), 100);
        assertEquals(sourceFileSizeDistribution.getLowRiskValue(), 0);
        assertEquals(sourceFileSizeDistribution.getMediumRiskValue(), 0);
        assertEquals(sourceFileSizeDistribution.getHighRiskValue(), 0);
        assertEquals(sourceFileSizeDistribution.getVeryHighRiskValue(), 0);

        sourceFileSizeDistribution.getOverallDistribution(getSourceFile(100, 80));

        assertEquals(sourceFileSizeDistribution.getNegligibleRiskValue(), 180);
        assertEquals(sourceFileSizeDistribution.getLowRiskValue(), 0);
        assertEquals(sourceFileSizeDistribution.getMediumRiskValue(), 0);
        assertEquals(sourceFileSizeDistribution.getHighRiskValue(), 0);
        assertEquals(sourceFileSizeDistribution.getVeryHighRiskValue(), 0);

        sourceFileSizeDistribution.getOverallDistribution(getSourceFile(100, 80, 500, 400, 1000, 800, 2000, 1400));

        assertEquals(sourceFileSizeDistribution.getNegligibleRiskValue(), 180);
        assertEquals(sourceFileSizeDistribution.getLowRiskValue(), 0);
        assertEquals(sourceFileSizeDistribution.getMediumRiskValue(), 900);
        assertEquals(sourceFileSizeDistribution.getHighRiskValue(), 1800);
        assertEquals(sourceFileSizeDistribution.getVeryHighRiskValue(), 3400);
    }

    private List<SourceFile> getSourceFile(int... linesOfCode) {
        List<SourceFile> sourceFiles = new ArrayList<>();
        for (int lines : linesOfCode) {
            SourceFile sourceFile = new SourceFile();
            sourceFile.setLinesOfCode(lines);
            sourceFiles.add(sourceFile);
        }
        return sourceFiles;
    }

}
