package nl.obren.sokrates.sourcecode.units;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class UnitUtilsTest {
    @Test
    public void getLinesOfCode() throws Exception {
        UnitInfo unit1 = new UnitInfo();
        unit1.setLinesOfCode(10);
        UnitInfo unit2 = new UnitInfo();
        unit2.setLinesOfCode(20);

        assertEquals(UnitUtils.getLinesOfCode(Arrays.asList(unit1, unit2)), 30);
    }

    @Test
    public void getUnitSizeRiskDistributionInstance() throws Exception {
        RiskDistributionStats instance = UnitUtils.getUnitSizeRiskDistributionInstance();
        assertTrue(instance.getMediumRiskThreshold() > 0);
        assertTrue(instance.getHighRiskThreshold() > instance.getMediumRiskThreshold());
        assertTrue(instance.getVeryHighRiskThreshold() > instance.getHighRiskThreshold());
    }

    @Test
    public void getCyclomaticComplexityRiskDistributionInstance() throws Exception {
        RiskDistributionStats instance = UnitUtils.getCyclomaticComplexityRiskDistributionInstance();
        assertTrue(instance.getMediumRiskThreshold() > 0);
        assertTrue(instance.getHighRiskThreshold() > instance.getMediumRiskThreshold());
        assertTrue(instance.getVeryHighRiskThreshold() > instance.getHighRiskThreshold());
    }

    @Test
    public void getUnitSizeDistributionPerExtension() throws Exception {
        List<UnitInfo> units = new ArrayList<>();
        UnitInfo unit1 = new UnitInfo();
        unit1.setLinesOfCode(10);
        unit1.setSourceFile(new SourceFile(new File("A.java"), " "));
        units.add(unit1);

        UnitInfo unit2 = new UnitInfo();
        unit2.setLinesOfCode(20);
        unit2.setSourceFile(new SourceFile(new File("B.java"), " "));
        units.add(unit2);

        UnitInfo unit3 = new UnitInfo();
        unit3.setLinesOfCode(110);
        unit3.setSourceFile(new SourceFile(new File("B.java"), " "));
        units.add(unit3);

        List<RiskDistributionStats> instance = UnitUtils.getUnitSizeDistributionPerExtension(units);
        assertEquals(instance.size(), 1);
        assertEquals(instance.get(0).getKey(), "java");
        assertEquals(instance.get(0).getLowRiskValue(), 30);
        assertEquals(instance.get(0).getMediumRiskValue(), 0);
        assertEquals(instance.get(0).getHighRiskValue(), 0);
        assertEquals(instance.get(0).getVeryHighRiskValue(), 110);
    }

    @Test
    public void getCyclomaticComplexityDistributionPerExtension() throws Exception {
        List<UnitInfo> units = new ArrayList<>();
        UnitInfo unit1 = new UnitInfo();
        unit1.setLinesOfCode(10);
        unit1.setMcCabeIndex(1);
        unit1.setSourceFile(new SourceFile(new File("A.java"), " "));
        units.add(unit1);

        UnitInfo unit2 = new UnitInfo();
        unit2.setLinesOfCode(30);
        unit2.setSourceFile(new SourceFile(new File("B.java"), " "));
        unit2.setMcCabeIndex(10);
        units.add(unit2);

        UnitInfo unit3 = new UnitInfo();
        unit3.setLinesOfCode(50);
        unit3.setMcCabeIndex(30);
        unit3.setSourceFile(new SourceFile(new File("B.java"), " "));
        units.add(unit3);

        List<RiskDistributionStats> instance = UnitUtils.getCyclomaticComplexityDistributionPerExtension(units);
        assertEquals(instance.size(), 1);
        assertEquals(instance.get(0).getKey(), "java");
        assertEquals(instance.get(0).getLowRiskValue(), 10);
        assertEquals(instance.get(0).getMediumRiskValue(), 30);
        assertEquals(instance.get(0).getHighRiskValue(), 0);
        assertEquals(instance.get(0).getVeryHighRiskValue(), 50);
    }

    @Test
    public void getUnitSizeDistributionPerComponent() throws Exception {
        List<UnitInfo> units = new ArrayList<>();
        UnitInfo unit1 = new UnitInfo();
        unit1.setLinesOfCode(10);
        SourceFile sourceFile1 = new SourceFile(new File("A.java"), " ");
        sourceFile1.getLogicalComponents().add(new NamedSourceCodeAspect("A"));
        unit1.setSourceFile(sourceFile1);
        units.add(unit1);

        UnitInfo unit2 = new UnitInfo();
        unit2.setLinesOfCode(20);
        SourceFile sourceFile2 = new SourceFile(new File("B.java"), " ");
        sourceFile2.getLogicalComponents().add(new NamedSourceCodeAspect("A"));
        unit2.setSourceFile(sourceFile2);
        units.add(unit2);

        UnitInfo unit3 = new UnitInfo();
        unit3.setLinesOfCode(110);
        SourceFile sourceFile3 = new SourceFile(new File("B.java"), " ");
        sourceFile3.getLogicalComponents().add(new NamedSourceCodeAspect("B"));
        unit3.setSourceFile(sourceFile3);
        units.add(unit3);

        LogicalDecomposition logicalDecomposition = new LogicalDecomposition("") {
            @Override
            public boolean isInScope(SourceFile sourceFile) {
                return true;
            }
        };

        List<RiskDistributionStats> instance = UnitUtils.getUnitSizeDistributionPerComponent(Arrays.asList(logicalDecomposition), units).get(0);
        assertEquals(instance.size(), 2);
        assertEquals(instance.get(0).getKey(), "A");
        assertEquals(instance.get(0).getLowRiskValue(), 30);
        assertEquals(instance.get(0).getMediumRiskValue(), 0);
        assertEquals(instance.get(0).getHighRiskValue(), 0);
        assertEquals(instance.get(0).getVeryHighRiskValue(), 0);
        assertEquals(instance.get(1).getKey(), "B");
        assertEquals(instance.get(1).getLowRiskValue(), 0);
        assertEquals(instance.get(1).getMediumRiskValue(), 0);
        assertEquals(instance.get(1).getHighRiskValue(), 0);
        assertEquals(instance.get(1).getVeryHighRiskValue(), 110);
    }

    @Test
    public void getCyclomaticComplexityDistributionPerComponent() throws Exception {
        List<UnitInfo> units = new ArrayList<>();
        UnitInfo unit1 = new UnitInfo();
        unit1.setLinesOfCode(20);
        unit1.setMcCabeIndex(2);
        SourceFile sourceFile1 = new SourceFile(new File("A.java"), " ");
        sourceFile1.getLogicalComponents().add(new NamedSourceCodeAspect("A"));
        unit1.setSourceFile(sourceFile1);
        units.add(unit1);

        UnitInfo unit2 = new UnitInfo();
        unit2.setLinesOfCode(40);
        unit2.setMcCabeIndex(12);
        SourceFile sourceFile2 = new SourceFile(new File("B.java"), " ");
        sourceFile2.getLogicalComponents().add(new NamedSourceCodeAspect("A"));
        unit2.setSourceFile(sourceFile2);
        units.add(unit2);

        UnitInfo unit3 = new UnitInfo();
        unit3.setLinesOfCode(100);
        unit3.setMcCabeIndex(32);
        SourceFile sourceFile3 = new SourceFile(new File("B.java"), " ");
        sourceFile3.getLogicalComponents().add(new NamedSourceCodeAspect("B"));
        unit3.setSourceFile(sourceFile3);
        units.add(unit3);

        LogicalDecomposition logicalDecomposition = new LogicalDecomposition("") {
            @Override
            public boolean isInScope(SourceFile sourceFile) {
                return true;
            }
        };
        List<RiskDistributionStats> instance = UnitUtils.getCyclomaticComplexityDistributionPerComponent(Arrays.asList(logicalDecomposition), units).get(0);
        assertEquals(instance.size(), 2);
        assertEquals(instance.get(0).getKey(), "A");
        assertEquals(instance.get(0).getLowRiskValue(), 20);
        assertEquals(instance.get(0).getMediumRiskValue(), 0);
        assertEquals(instance.get(0).getHighRiskValue(), 40);
        assertEquals(instance.get(0).getVeryHighRiskValue(), 0);
        assertEquals(instance.get(1).getKey(), "B");
        assertEquals(instance.get(1).getLowRiskValue(), 0);
        assertEquals(instance.get(1).getMediumRiskValue(), 0);
        assertEquals(instance.get(1).getHighRiskValue(), 0);
        assertEquals(instance.get(1).getVeryHighRiskValue(), 100);
    }

    @Test
    public void getAggregateUnitSizeRiskDistribution() throws Exception {
        List<UnitInfo> units = new ArrayList<>();
        UnitInfo unit1 = new UnitInfo();
        unit1.setLinesOfCode(10);
        unit1.setSourceFile(new SourceFile(new File("A.java"), " "));
        units.add(unit1);

        UnitInfo unit2 = new UnitInfo();
        unit2.setLinesOfCode(60);
        unit2.setSourceFile(new SourceFile(new File("B.java"), " "));
        units.add(unit2);

        UnitInfo unit3 = new UnitInfo();
        unit3.setLinesOfCode(210);
        unit3.setSourceFile(new SourceFile(new File("B.java"), " "));
        units.add(unit3);

        List<RiskDistributionStats> aggregateUnitSizeRiskDistribution = UnitUtils.getAggregateUnitSizeRiskDistribution(units, param -> "X");
        assertEquals(aggregateUnitSizeRiskDistribution.size(), 1);
        assertEquals(aggregateUnitSizeRiskDistribution.get(0).getKey(), "X");
        assertEquals(aggregateUnitSizeRiskDistribution.get(0).getLowRiskValue(), 10);
        assertEquals(aggregateUnitSizeRiskDistribution.get(0).getMediumRiskValue(), 0);
        assertEquals(aggregateUnitSizeRiskDistribution.get(0).getHighRiskValue(), 60);
        assertEquals(aggregateUnitSizeRiskDistribution.get(0).getVeryHighRiskValue(), 210);
    }

    @Test
    public void getAggregateCyclomaticComplexityRiskDistribution() throws Exception {
        List<UnitInfo> units = new ArrayList<>();
        UnitInfo unit1 = new UnitInfo();
        unit1.setLinesOfCode(10);
        unit1.setMcCabeIndex(1);
        unit1.setSourceFile(new SourceFile(new File("A.java"), " "));
        units.add(unit1);

        UnitInfo unit2 = new UnitInfo();
        unit2.setLinesOfCode(11);
        unit2.setSourceFile(new SourceFile(new File("B.java"), " "));
        unit2.setMcCabeIndex(2);
        units.add(unit2);

        UnitInfo unit3 = new UnitInfo();
        unit3.setLinesOfCode(50);
        unit3.setMcCabeIndex(30);
        unit3.setSourceFile(new SourceFile(new File("B.java"), " "));
        units.add(unit3);

        List<RiskDistributionStats> aggregateCyclomaticComplexityRiskDistribution = UnitUtils.getAggregateCyclomaticComplexityRiskDistribution(units, param -> "Y");
        assertEquals(aggregateCyclomaticComplexityRiskDistribution.size(), 1);
        assertEquals(aggregateCyclomaticComplexityRiskDistribution.get(0).getKey(), "Y");
        assertEquals(aggregateCyclomaticComplexityRiskDistribution.get(0).getLowRiskValue(), 21);
        assertEquals(aggregateCyclomaticComplexityRiskDistribution.get(0).getMediumRiskValue(), 0);
        assertEquals(aggregateCyclomaticComplexityRiskDistribution.get(0).getHighRiskValue(), 0);
        assertEquals(aggregateCyclomaticComplexityRiskDistribution.get(0).getVeryHighRiskValue(), 50);
    }

    @Test
    public void getUnitSizeDistribution() throws Exception {
        List<UnitInfo> units = new ArrayList<>();
        UnitInfo unit1 = new UnitInfo();
        unit1.setLinesOfCode(20);
        units.add(unit1);

        UnitInfo unit2 = new UnitInfo();
        unit2.setLinesOfCode(50);
        units.add(unit2);

        UnitInfo unit3 = new UnitInfo();
        unit3.setLinesOfCode(100);
        units.add(unit3);

        UnitInfo unit4 = new UnitInfo();
        unit4.setLinesOfCode(1);
        units.add(unit4);

        UnitInfo unit5 = new UnitInfo();
        unit5.setLinesOfCode(200);
        units.add(unit5);

        assertEquals(UnitUtils.getUnitSizeDistribution(units).getLowRiskValue(), 21);
        assertEquals(UnitUtils.getUnitSizeDistribution(units).getMediumRiskValue(), 50);
        assertEquals(UnitUtils.getUnitSizeDistribution(units).getHighRiskValue(), 100);
        assertEquals(UnitUtils.getUnitSizeDistribution(units).getVeryHighRiskValue(), 200);
    }

    @Test
    public void getCyclomaticComplexityDistribution() throws Exception {
        List<UnitInfo> units = new ArrayList<>();
        UnitInfo unit1 = new UnitInfo();
        unit1.setLinesOfCode(10);
        unit1.setMcCabeIndex(5);
        units.add(unit1);

        UnitInfo unit2 = new UnitInfo();
        unit2.setLinesOfCode(20);
        unit2.setMcCabeIndex(10);
        units.add(unit2);

        UnitInfo unit3 = new UnitInfo();
        unit3.setLinesOfCode(30);
        unit3.setMcCabeIndex(25);
        units.add(unit3);

        UnitInfo unit4 = new UnitInfo();
        unit4.setLinesOfCode(200);
        unit4.setMcCabeIndex(100);
        units.add(unit4);

        UnitInfo unit5 = new UnitInfo();
        unit5.setLinesOfCode(1);
        unit5.setMcCabeIndex(1);
        units.add(unit5);

        assertEquals(UnitUtils.getCyclomaticComplexityDistribution(units).getLowRiskValue(), 11);
        assertEquals(UnitUtils.getCyclomaticComplexityDistribution(units).getMediumRiskValue(), 20);
        assertEquals(UnitUtils.getCyclomaticComplexityDistribution(units).getHighRiskValue(), 30);
        assertEquals(UnitUtils.getCyclomaticComplexityDistribution(units).getVeryHighRiskValue(), 200);
    }
}
