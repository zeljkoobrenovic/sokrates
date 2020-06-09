/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.charts.SimpleOneBarChart;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.dataexporters.DataExportUtils;
import nl.obren.sokrates.reports.dataexporters.DataExporter;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.reports.utils.ScopesRenderer;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.LogicalDecompositionAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.DependencyFinderPattern;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.aspects.MetaRule;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.dependencies.DependencyEvidence;
import nl.obren.sokrates.sourcecode.dependencies.DependencyUtils;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class LogicalComponentsReportGenerator {
    private CodeAnalysisResults codeAnalysisResults;
    private boolean elaborate = true;
    private RichTextReport report;
    private int dependencyVisualCounter = 1;

    public LogicalComponentsReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addCodeOrganizationToReport(RichTextReport report) {
        this.report = report;
        addSummary();
        // addErrors();
        addFooter();
    }

    private void addFooter() {
        report.addLineBreak();
        report.addHorizontalLine();
        report.addParagraph(RichTextRenderingUtils.italic(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())));
    }

    private void addSummary() {
        report.startSection("Intro", "");
        if (elaborate) {
            appendIntroduction();
        }
        report.endSection();

        report.startSection("Logical Decompositions Overview", "");
        int size = codeAnalysisResults.getLogicalDecompositionsAnalysisResults().size();
        report.addParagraph("Analyzed system has <b>" + size + "</b> logical decomposition" + (size > 1 ? "s" : "") + ":");
        report.startUnorderedList();
        codeAnalysisResults.getLogicalDecompositionsAnalysisResults().forEach(logicalDecomposition -> {
            int componentsCount = logicalDecomposition.getComponents().size();
            report.addListItem(logicalDecomposition.getLogicalDecomposition().getName() + " (" + componentsCount + " component" + (componentsCount > 1 ? "s" : "") + ")");
        });
        report.endUnorderedList();
        report.endSection();

        int[] sectionIndex = {1};
        codeAnalysisResults.getLogicalDecompositionsAnalysisResults().forEach(logicalDecomposition -> {
            analyzeLogicalDecomposition(sectionIndex[0], logicalDecomposition);
            sectionIndex[0]++;
        });
        report.endSection();
    }

    private void analyzeLogicalDecomposition(int sectionIndex, LogicalDecompositionAnalysisResults logicalDecomposition) {
        report.startSection("Logical Decomposition #" + sectionIndex + ": " + logicalDecomposition.getKey().toUpperCase(), getDecompositionDescription(logicalDecomposition));

        List<NumericMetric> fileCountPerExtension = logicalDecomposition.getFileCountPerComponent();
        List<NumericMetric> linesOfCodePerExtension = logicalDecomposition.getLinesOfCodePerComponent();

        ScopesRenderer renderer = new ScopesRenderer();
        renderer.setLinesOfCodeInMain(codeAnalysisResults.getMainAspectAnalysisResults().getLinesOfCode());

        renderer.setTitle("Components");
        renderer.setDescription("");
        renderer.setFileCountPerComponent(fileCountPerExtension);
        // renderer.setFilesListPath();
        renderer.setLinesOfCode(linesOfCodePerExtension);
        renderer.setMaxFileCount(codeAnalysisResults.getMaxFileCount());
        renderer.setMaxLinesOfCode(codeAnalysisResults.getMaxLinesOfCode());
        List<AspectAnalysisResults> components = logicalDecomposition.getComponents();
        String filePathPrefix = DataExportUtils.getComponentFilePrefix(logicalDecomposition.getKey());
        renderer.setAspectsFileListPaths(components.stream().map(aspect -> aspect.getAspect().getFileSystemFriendlyName(filePathPrefix)).collect(Collectors.toList()));
        renderer.renderReport(report, "The \"" + logicalDecomposition.getLogicalDecomposition().getName() + "\" logical decomposition has <b>" + logicalDecomposition.getLogicalDecomposition().getComponents().size() + "</b> components.");

        List<MetaRule> metaComponents = logicalDecomposition.getLogicalDecomposition().getMetaComponents();
        if (metaComponents.size() > 0) {
            report.startSubSection("Meta-Rules for Componentization", "");
            report.addListItem("The following explicit meta-rules for components are defined:");
            describeMetaRules(metaComponents);
            report.endSection();
        }

        report.startSubSection("Alternative Visuals", "");
        report.startUnorderedList();
        report.addListItem("<a href='visuals/bubble_chart_components_" + (sectionIndex) + ".html'>Bubble Chart</a>");
        report.addListItem("<a href='visuals/tree_map_components_" + (sectionIndex) + ".html'>Tree Map</a>");
        report.endUnorderedList();
        report.endSection();

        List<ComponentDependency> componentDependencies = logicalDecomposition.getComponentDependencies();
        report.startSubSection("Dependencies", "Dependencies among components are <b>static</b> code dependencies among files in different components.");
        if (componentDependencies != null && componentDependencies.size() > 0) {
            addComponentDependenciesSection(logicalDecomposition, componentDependencies);
        } else {
            report.addParagraph("No component dependencies found.");
        }
        report.endSection();
        report.endSection();
    }

    private void addComponentDependenciesSection(LogicalDecompositionAnalysisResults logicalDecomposition, List<ComponentDependency> componentDependencies) {
        report.startUnorderedList();
        report.addListItem("Analyzed system has <b>" + componentDependencies.size() + "</b> links (arrows) between components.");
        report.addListItem("The number on the arrow represents the number of files from referring component that depend on files in referred component.");
        report.addListItem("These " + componentDependencies.size() + " links contain <a href='../data/text/" + DataExporter.dependenciesFileNamePrefix("", "", logicalDecomposition.getKey()) + ".txt'><b>" + DependencyUtils.getDependenciesCount(componentDependencies) + "</b> dependencies</a>.");
        int cyclicDependencyPlacesCount = DependencyUtils.getCyclicDependencyPlacesCount(componentDependencies);
        int cyclicDependencyCount = DependencyUtils.getCyclicDependencyCount(componentDependencies);
        if (cyclicDependencyPlacesCount > 0) {
            String numberOfPlacesText = cyclicDependencyPlacesCount == 1
                    ? "is <b>1</b> place"
                    : "are <b>" + cyclicDependencyPlacesCount + "</b> places";
            report.addListItem("There " + numberOfPlacesText + " (" + (cyclicDependencyPlacesCount * 2) + " links) with <b>cyclic</b> dependencies (<b>" + cyclicDependencyCount + "</b> " +
                    "file dependencies).");
        }

        describeDependencyFinder(logicalDecomposition);

        report.endUnorderedList();

        List<String> componentNames = new ArrayList<>();
        logicalDecomposition.getComponents().forEach(c -> componentNames.add(c.getName()));
        GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
        graphvizDependencyRenderer.setOrientation(logicalDecomposition.getLogicalDecomposition().getRenderingOptions().getOrientation());

        addDependencyGraphVisuals(componentDependencies, componentNames, graphvizDependencyRenderer);
        report.addLineBreak();
        report.addLineBreak();
        addMoreDetailsSection(logicalDecomposition, componentDependencies);
        report.addLineBreak();
        report.addLineBreak();
        report.addLineBreak();
    }

    private void addDependencyGraphVisuals(List<ComponentDependency> componentDependencies, List<String> componentNames, GraphvizDependencyRenderer graphvizDependencyRenderer) {
        String graphvizContent = graphvizDependencyRenderer.getGraphvizContent(componentNames, componentDependencies);
        String graphId = "dependencies_" + dependencyVisualCounter++;
        report.addGraphvizFigure(graphId, "", graphvizContent);
        report.addLineBreak();
        report.addLineBreak();
        addDownloadLinks(graphId);
    }

    private void addMoreDetailsSection(LogicalDecompositionAnalysisResults logicalDecomposition, List<ComponentDependency> componentDependencies) {
        report.startShowMoreBlock("Show more details about dependencies...");
        report.startDiv("width: 100%; overflow-x: auto");
        report.startTable();
        report.addTableHeader("From Component<br/>&nbsp;--> To Component", "From Component<br/>(files with dependencies)", "Details");
        Collections.sort(componentDependencies, (o1, o2) -> o2.getCount() - o1.getCount());
        componentDependencies.forEach(componentDependency -> {
            addDependencyRow(logicalDecomposition, componentDependency);
        });
        report.endTable();
        report.endDiv();
        report.endShowMoreBlock();
    }

    private void addDownloadLinks(String graphId) {
        report.startDiv("");
        report.addHtmlContent("Download: ");
        report.addNewTabLink("SVG", "visuals/" + graphId + ".svg");
        report.addHtmlContent(" ");
        report.addNewTabLink("DOT", "visuals/" + graphId + ".dot.txt");
        report.addHtmlContent(" ");
        report.addNewTabLink("(open online Graphviz editor)", "https://www.zeljkoobrenovic.com/tools/graphviz/");
        report.endDiv();
    }

    private void describeDependencyFinder(LogicalDecompositionAnalysisResults logicalDecomposition) {
        List<DependencyFinderPattern> rules = logicalDecomposition.getLogicalDecomposition().getDependenciesFinder().getRules();
        if (rules.size() > 0) {
            report.addListItem("The following explicit rules for finding dependencies are defined:");
            report.startUnorderedList();
            rules.forEach(rule -> {
                SourceFileFilter filter = new SourceFileFilter(rule.getPathPattern(), rule.getContentPattern());
                report.addListItem("\"" + rule.getComponent() + "\" <== " + filter.toString());
            });
            report.endUnorderedList();
        }
        List<MetaRule> metaRules = logicalDecomposition.getLogicalDecomposition().getDependenciesFinder().getMetaRules();
        if (metaRules.size() > 0) {
            report.addListItem("The following explicit meta-rules for finding dependencies are defined:");
            describeMetaRules(metaRules);
        }
    }

    private void describeMetaRules(List<MetaRule> metaRules) {
        report.startUnorderedList();
        metaRules.forEach(rule -> {
            SourceFileFilter filter = new SourceFileFilter(rule.getPathPattern(), rule.getContentPattern());
            report.addListItem(filter.toString());
            report.startUnorderedList();
            rule.getNameOperations().forEach(op -> {
                StringBuilder params = new StringBuilder();
                op.getParams().forEach(param -> {
                    if (params.length() > 0) params.append(", ");
                    params.append("\"" + param + "\"");
                });
                report.addListItem(op.getOp() + " (" + params.toString() + ")");
            });
            report.endUnorderedList();
        });
        report.endUnorderedList();
    }

    private void addDependencyRow(LogicalDecompositionAnalysisResults logicalDecomposition, ComponentDependency componentDependency) {
        report.startTableRow();
        report.addTableCell(componentDependency.getFromComponent() + "<br/>&nbsp&nbsp;-->&nbsp" + componentDependency.getToComponent()
        );
        report.addHtmlContent("<td>");
        int locFromDuplications = componentDependency.getLocFrom();
        NamedSourceCodeAspect fromComponentByName = logicalDecomposition.getLogicalDecomposition().getComponentByName(componentDependency.getFromComponent());
        String percentageHtmlFragment = null;
        int dependencyCount = componentDependency.getCount();
        if (fromComponentByName != null) {
            percentageHtmlFragment = getFromDependecyCoverageSvg(locFromDuplications, fromComponentByName, dependencyCount);
        }

        report.addShowMoreBlock("",
                "<textarea style='width:90%; height: 20em;'>"
                        + componentDependency.getEvidence().stream().map(DependencyEvidence::getPathFrom).collect(Collectors.joining("\n")) +
                        "</textarea>",
                (percentageHtmlFragment != null ? "" + percentageHtmlFragment : dependencyCount + " files (" + locFromDuplications + " LOC)<br/>")
        );
        report.addHtmlContent("</td>");
        report.addTableCell("<a href='../data/text/" + DataExporter.dependenciesFileNamePrefix(componentDependency.getFromComponent(), componentDependency.getToComponent(), logicalDecomposition.getKey()) + ".txt'><b>" + dependencyCount + "</b> source " + (dependencyCount == 1 ? "file" : "files") + "</a>");
        report.endTableRow();
    }

    private String getFromDependecyCoverageSvg(int locFromDuplications, NamedSourceCodeAspect fromComponentByName, int dependencyCount) {
        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(320);
        chart.setMaxBarWidth(100);
        chart.setBarHeight(14);
        chart.setBarStartXOffset(2);
        chart.setSmallerFontSize();

        double percentage = 100.0 * locFromDuplications / fromComponentByName.getLinesOfCode();
        String percentageText = FormattingUtils.getFormattedPercentage(percentage) + "%";

        String textRight = dependencyCount + " "
                + (dependencyCount == 1 ? "file" : "files") + ", "
                + locFromDuplications + " LOC (" + percentageText + ")";

        return chart.getPercentageSvg(percentage, "", textRight);
    }

    private String getDecompositionDescription(LogicalDecompositionAnalysisResults logicalDecomposition) {
        int numberOfComponents = logicalDecomposition.getComponents().size();
        StringBuilder description = new StringBuilder();
        LogicalDecomposition definition = logicalDecomposition.getLogicalDecomposition();
        if (definition.getComponentsFolderDepth() > 0) {
            description.append("The decompositions is based on folder structure at <b>level " + definition.getComponentsFolderDepth() + "</b> (relative to the source code root).");
        } else if (logicalDecomposition.getComponents() != null && numberOfComponents > 0) {
            description.append("The \"" + definition.getName() + "\" logical decomposition in based on <b>" + numberOfComponents + "</b> explicitly defined components.");
        }
        return description.toString();
    }

    private void appendIntroduction() {
        report.addParagraph(getShortIntro());
        report.addHtmlContent(getLongIntro());

        report.startShowMoreBlock("Learn more...");
        report.startUnorderedList();
        report.addListItem("To learn more about good practices on componentization and dependencies, Sokrates recommends the following resources:");
        report.startUnorderedList();
        report.addListItem("<a target='_blank' href='https://www.martinfowler.com/ieeeSoftware/coupling.pdf'>Reduce Coupling</a>, MartinFlower.com (IEEE Software article)");
        report.addListItem("<a target='_blank' href='https://sourcemaking.com/refactoring/smells/couplers'>Couplers Code Smells</a>, sourcemaking.com");
        report.endUnorderedList();
        report.endUnorderedList();

        report.endShowMoreBlock();
    }

    private String getShortIntro() {
        String shortIntro = "";
        shortIntro += "<b>Logical decomposition</b> is a representation of the organization of the <b>main</b> source code, where every and each file is put in exactly one <b>logical component</b>.";
        return shortIntro;
    }

    private String getLongIntro() {
        String longIntro = "<ul>\n";
        longIntro += "<li>A software system can have <b>one</b> or <b>more</b> logical decompositions.</li>\n";
        longIntro += "<li>A logical decomposition can be defined in two ways in Sokrates.</li>\n";
        longIntro += "<li>First approach is based on the <b>folders structure</b>. " +
                "Components are mapped to folders at defined <b>folder depth</b> relative to the source code root.</li>\n";
        longIntro += "<li>Second approach is based on <b>explicit</b> definition of each component. In such explicit definitions, components are explicitly <b>named</b> and their files are selected based on explicitly defined path and content <b>filters</b>.</li>\n";
        longIntro += "<li>A logical decomposition is considered <b>invalid</b> if a file is selected into <b>two or more components</b>." +
                "This constraint is introduced in order to facilitate measuring of <b>dependencies</b> among components.</li>\n";
        longIntro += "<li>Files not assigned to any component are put into a special \"<b>Unclassified</b>\" component.</li>\n";

        longIntro += "</ul>\n";
        return longIntro;
    }


}
