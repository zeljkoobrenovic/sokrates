/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.utils;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.charts.SimpleOneBarChart;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.HtmlEscapeUtils;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScopesRenderer {
    private List<String> aspectsFileListPaths;
    private List<NumericMetric> fileCountPerComponent;
    private List<NumericMetric> linesOfCode;
    private String title;
    private String description;
    private int maxFileCount;
    private int maxLinesOfCode;
    private int filesCount = 0;
    private int linesCount = 0;
    private int linesOfCodeInMain = 1;
    private int totalNumberOfRegexMatches = 0;
    private NamedSourceCodeAspect aspect;
    private boolean inSection = true;
    private String filesListPath;
    private String explorers = "";
    private boolean sort = true;
    private String metric = "LOC";
    private boolean describe = true;
    private String activeColor = "#00aced";

    public List<String> getAspectsFileListPaths() {
        return aspectsFileListPaths;
    }

    public void setAspectsFileListPaths(List<String> aspectsFileListPaths) {
        this.aspectsFileListPaths = aspectsFileListPaths;
    }

    public int getFilesCount() {
        return filesCount;
    }

    public void setFilesCount(int filesCount) {
        this.filesCount = filesCount;
    }

    public int getLinesCount() {
        return linesCount;
    }

    public void setLinesCount(int linesCount) {
        this.linesCount = linesCount;
    }

    public List<NumericMetric> getFileCountPerComponent() {
        return fileCountPerComponent;
    }

    public void setFileCountPerComponent(List<NumericMetric> fileCountPerComponent) {
        this.fileCountPerComponent = fileCountPerComponent;
    }

    public List<NumericMetric> getLinesOfCode() {
        return linesOfCode;
    }

    public void setLinesOfCode(List<NumericMetric> linesOfCode) {
        this.linesOfCode = linesOfCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMaxFileCount() {
        return maxFileCount;
    }

    public void setMaxFileCount(int maxFileCount) {
        this.maxFileCount = maxFileCount;
    }

    public int getMaxLinesOfCode() {
        return maxLinesOfCode;
    }

    public void setMaxLinesOfCode(int maxLinesOfCode) {
        this.maxLinesOfCode = maxLinesOfCode;
    }

    public int getTotalNumberOfRegexMatches() {
        return totalNumberOfRegexMatches;
    }

    public void setTotalNumberOfRegexMatches(int totalNumberOfRegexMatches) {
        this.totalNumberOfRegexMatches = totalNumberOfRegexMatches;
    }

    public boolean isInSection() {
        return inSection;
    }

    public void setInSection(boolean inSection) {
        this.inSection = inSection;
    }

    public int getLinesOfCodeInMain() {
        return linesOfCodeInMain;
    }

    public void setLinesOfCodeInMain(int linesOfCodeInMain) {
        this.linesOfCodeInMain = linesOfCodeInMain;
    }

    public String getFilesListPath() {
        return filesListPath;
    }

    public void setFilesListPath(String filesListPath) {
        this.filesListPath = filesListPath;
    }

    public void renderReport(RichTextReport report, String description) {
        renderReport(report, description, null);
    }

    public void renderReport(RichTextReport report, String description, String extraIntroHtmlFragment) {
        updateCountVariables();
        if (fileCountPerComponent.size() > 0) {
            if (linesOfCode.size() > 0 && linesCount > 0) {
                List<ScopeRendererItem> renderingList = getRenderingList();
                if (inSection) {
                    report.startSubSection(title, description);
                    if (describe) {
                        renderIntro(report, extraIntroHtmlFragment, renderingList);
                    }
                }

                report.startScrollingDiv();
                getSvgBars(report, renderingList);
                report.endDiv();

                if (inSection) {
                    if (describe) {
                        // The scope details close the sub-section, below the bars.
                        renderScopeDetailsBlock(report);
                    }
                    report.endSection();
                }
            }
        }
    }

    /** The optional intro fragment and the explorer links (the scope details follow the bars). */
    private void renderIntro(RichTextReport report, String extraIntroHtmlFragment, List<ScopeRendererItem> renderingList) {
        if (StringUtils.isNotBlank(extraIntroHtmlFragment)) {
            report.addHtmlContent(extraIntroHtmlFragment);
        }
        renderExplorerLinks(report);
        report.addLineBreak();
    }

    private List<ScopeRendererItem> getRenderingList() {
        List<ScopeRendererItem> items = new ArrayList<>();
        for (int i = 0; i < linesOfCode.size(); i++) {
            ScopeRendererItem item = new ScopeRendererItem();
            item.setLinesOfCode(linesOfCode.get(i));
            String filesFragment = "";
            if (describe && fileCountPerComponent.size() > i) {
                item.setFilesCount(fileCountPerComponent.get(i));
                int count = fileCountPerComponent.get(i).getValue().intValue();
                filesFragment = FormattingUtils.formatCountPlural(count, "file", "files");
                if (aspectsFileListPaths != null && aspectsFileListPaths.size() > i) {
                    if (count > 0) {
                        filesFragment = "<u><a href='#' onclick=\"return downloadDataFile('text/aspect_" + aspectsFileListPaths.get(i)
                                + ".txt')\">" + filesFragment + "</a></u>";
                    }
                }
                item.setFilesFragment(filesFragment);
            }
            items.add(item);
        }

        if (sort) {
            Collections.sort(items, (o1, o2) -> -Integer.compare(o1.getLinesOfCode().getValue().intValue(), o2.getLinesOfCode().getValue().intValue()));
        }

        return items;
    }

    private void getSvgBars(RichTextReport report, List<ScopeRendererItem> renderingList) {
        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(800);
        chart.setMaxBarWidth(200);
        chart.setBarHeight(20);
        chart.setActiveColor(activeColor);

        report.startDiv("width: 100%; overflow-x: auto");
        report.startDiv("min-width: 1000px");
        renderingList.forEach(rendererItem -> {
            int metricLinesOfCode = rendererItem.getLinesOfCode().getValue().intValue();
            double percentage = maxLinesOfCode > 0 ? 100.0 * metricLinesOfCode / maxLinesOfCode : 0;
            String filesFragment = StringUtils.defaultIfBlank(rendererItem.getFilesFragment(), "");

            report.addContentInDiv(chart.getPercentageSvg(percentage, rendererItem.getLinesOfCode().getName(),
                    "" + metricLinesOfCode + " " + metric + " (" +
                            HtmlEscapeUtils.escape(FormattingUtils.getFormattedPercentage(percentage))
                            + "%) " + filesFragment), "");
        });
        report.endDiv();
        report.endDiv();
    }

    private void updateCountVariables() {
        filesCount = 0;
        linesCount = 0;
        for (int i = 0; i < fileCountPerComponent.size(); i++) {
            filesCount += fileCountPerComponent.get(i).getValue().intValue();
        }
        for (int i = 0; i < linesOfCode.size(); i++) {
            linesCount += linesOfCode.get(i).getValue().intValue();
        }
    }

    private String describeFilters(SourceFileFilter filter) {
        String description = !filter.getException() ? "" : "except";
        description += " files with ";
        boolean add = false;
        if (StringUtils.isNotBlank(filter.getPathPattern())) {
            description += "paths like \"<b>" + filter.getPathPattern() + "</b>\"";
            add = true;
        }
        if (StringUtils.isNotBlank(filter.getContentPattern())) {
            if (add) {
                description += " AND ";
            }
            description += "any line of content like \"<b>" + filter.getContentPattern() + "</b>\"";
        }
        return description + ".";
    }

    public void renderDetails(RichTextReport report, boolean renderTitle) {
        updateCountVariables();
        if (renderTitle) {
            report.addHtmlContent("<h3>" + title + "</h3>");
        }
        renderExplorerLinks(report);
        report.addLineBreak();
        renderScopeDetailsBlock(report);
    }

    // The collapsed "scope details..." block: the selection criteria, the description and the matches.
    private void renderScopeDetailsBlock(RichTextReport report) {
        report.addLineBreak();
        report.startDetailsBlock("scope details...");
        boolean criteriaDefined = aspect != null && aspect.getSourceFileFilters().size() > 0;
        if (criteriaDefined) {
            renderCriteria(report);
        }
        if (filesCount == 0) {
            report.startUnorderedList();
            report.addListItem("There are no \"" + title.toLowerCase() + "\" files.");
            report.endUnorderedList();
        } else {
            if (StringUtils.isNotBlank(description)) {
                report.startUnorderedList();
                report.addListItem(description);
                report.endUnorderedList();
            }
            report.startUnorderedList();
            if (criteriaDefined) {
                renderMatchesSummary(report);
            } else {
                report.addListItem("<b>" + RichTextRenderingUtils.renderNumber(filesCount) + "</b> files, " +
                        "<b>" + RichTextRenderingUtils.renderNumber(linesCount) + "</b> " + metric + " ("
                        + "<b>" + RichTextRenderingUtils.renderNumber(maxLinesOfCode > 0 ? 100.0 * linesCount / maxLinesOfCode : 0) + "%</b> vs. main code).");
            }
            report.endUnorderedList();
        }
        report.endDetailsBlock();
    }

    private void renderExplorerLinks(RichTextReport report) {
        if (StringUtils.isNotBlank(explorers)) {
            report.startDiv("");
            report.addHtmlContent("Explore:&nbsp;&nbsp;");
            report.addNewTabLink("circles", "visuals/zoomable_circles.html#" + explorers);
            report.addHtmlContent("&nbsp;|&nbsp;");
            report.addNewTabLink("sunburst", "visuals/zoomable_sunburst.html#" + explorers);
            report.endDiv();
        }
    }

    private void renderCriteria(RichTextReport report) {
        report.startUnorderedList();
        report.addListItem("The following criteria are used to filter files:");

        report.startUnorderedList();
        aspect.getSourceFileFilters().forEach(filter -> report.addListItem(describeFilters(filter)));
        report.endUnorderedList();

        report.endUnorderedList();
    }

    /** How many files match the criteria, with their lines, per-component split and content-pattern matches. */
    private void renderMatchesSummary(RichTextReport report) {
        report.addListItem(filesFragment() + " match" + (filesCount == 1 ? "es" : "") + " defined criteria (" +
                "<b>" + RichTextRenderingUtils.renderNumber(linesCount) + "</b> " + metric + ", "
                + "<b>" + RichTextRenderingUtils.renderNumber(linesOfCodeInMain > 0 ? 100.0 * linesCount / linesOfCodeInMain : 0) + "%</b> vs. main code)"
                + (fileCountPerComponent.size() == 1 ? ". All matches are in " + HtmlEscapeUtils.escape(fileCountPerComponent.get(0).getName()) + " files." : ":"));
        report.startUnorderedList();
        if (fileCountPerComponent.size() > 1) {
            addPerComponentItems(report);
        }
        report.endUnorderedList();
        if (totalNumberOfRegexMatches > 0) {
            report.addListItem(totalNumberOfRegexMatches == 1
                    ? "<b>1</b> line matches the content pattern."
                    : "<b>" + RichTextRenderingUtils.renderNumber(totalNumberOfRegexMatches) + "</b> lines match the content pattern.");
        }
    }

    /** "<n> file(s)", linked to the exported file list when there is one. */
    private String filesFragment() {
        String filesPhrase = filesCount == 1 ? "file" : "files";
        return StringUtils.isNotBlank(filesListPath)
                ? "<a href='#' onclick=\"return downloadDataFile('text/" + filesListPath + "')\"><b>" + filesCount + "</b> " + filesPhrase + "</a>"
                : "<b>" + filesCount + "</b> " + filesPhrase;
    }

    private void addPerComponentItems(RichTextReport report) {
        for (int i = 0; i < fileCountPerComponent.size(); i++) {
            NumericMetric fileCountMetric = fileCountPerComponent.get(i);
            NumericMetric linesOfCodeMetric = linesOfCode.get(i);
            report.addListItem("<b>" + RichTextRenderingUtils.renderNumber(fileCountMetric.getValue().intValue()) + "</b>"
                    + " " + HtmlEscapeUtils.escape(fileCountMetric.getName()) + " files"
                    + " (<b>" + RichTextRenderingUtils.renderNumber(linesOfCodeMetric.getValue().intValue()) + "</b> " + metric + ")");
        }
    }

    public NamedSourceCodeAspect getAspect() {
        return aspect;
    }

    public void setAspect(NamedSourceCodeAspect aspect) {
        this.aspect = aspect;
    }

    public String getExplorers() {
        return explorers;
    }

    public void setExplorers(String explorers) {
        this.explorers = explorers;
    }

    public boolean isSort() {
        return sort;
    }

    public void setSort(boolean sort) {
        this.sort = sort;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public void setDescribe(boolean describe) {
        this.describe = describe;
    }

    public boolean isDescribe() {
        return describe;
    }

    public String getActiveColor() {
        return activeColor;
    }

    public void setActiveColor(String activeColor) {
        this.activeColor = activeColor;
    }
}
