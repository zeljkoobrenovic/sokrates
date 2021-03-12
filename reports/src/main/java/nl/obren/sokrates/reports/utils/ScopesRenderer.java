/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.utils;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.charts.SimpleOneBarChart;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.text.DecimalFormat;
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
        updateCountVariables();
        if (fileCountPerComponent.size() > 0) {
            if (linesOfCode.size() > 0 && linesCount > 0) {
                List<ScopeRendererItem> renderingList = getRenderingList();
                if (inSection) {
                    report.startSubSection(title, description);
                    renderDetails(report, false);
                    if (renderingList.size() > 1) {
                        report.startUnorderedList();
                        NumericMetric firstMetric = renderingList.get(0).getLinesOfCode();
                        double firstPercentage = 100.0 * firstMetric.getValue().doubleValue() / linesCount;
                        report.addListItem("\"" + firstMetric.getName() + "\" is biggest, containing <b>" + new DecimalFormat("##.##").format(firstPercentage) + "%</b> of code.");
                        if (renderingList.size() >= 2) {
                            NumericMetric lastMetric = renderingList.get(renderingList.size() - 1).getLinesOfCode();
                            double lastPercentage = 100.0 * lastMetric.getValue().doubleValue() / linesCount;
                            report.addListItem("\"" + lastMetric.getName() + "\" is smallest, containing <b>" + new DecimalFormat("##.##").format(lastPercentage) + "%</b> of code.");
                        }
                        report.endUnorderedList();
                    }
                    report.addLineBreak();
                    report.addLineBreak();
                }

                report.startScrollingDiv();
                getSvgBars(report, renderingList);
                report.endDiv();

                if (inSection) report.endSection();
            }
        }
    }

    private List<ScopeRendererItem> getRenderingList() {
        List<ScopeRendererItem> items = new ArrayList<>();
        for (int i = 0; i < linesOfCode.size(); i++) {
            ScopeRendererItem item = new ScopeRendererItem();
            item.setLinesOfCode(linesOfCode.get(i));
            String filesFragment = "";
            if (fileCountPerComponent.size() > i) {
                item.setFilesCount(fileCountPerComponent.get(i));
                filesFragment = fileCountPerComponent.get(i).getValue() + " files";
                if (aspectsFileListPaths != null && aspectsFileListPaths.size() > i) {
                    filesFragment = "<u><a href='../data/text/aspect_" + aspectsFileListPaths.get(i)
                            + ".txt'>" + filesFragment + "</a></u>";
                }
                item.setFilesFragment(filesFragment);
            }
            items.add(item);
        }

        Collections.sort(items, (o1, o2) -> -Integer.compare(o1.getLinesOfCode().getValue().intValue(), o2.getLinesOfCode().getValue().intValue()));

        return items;
    }

    private void getSvgBars(RichTextReport report, List<ScopeRendererItem> renderingList) {
        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(800);
        chart.setMaxBarWidth(200);
        chart.setBarHeight(20);

        report.startDiv("width: 100%; overflow-x: auto");
        report.startDiv("min-width: 1000px");
        renderingList.forEach(rendererItem -> {
            int metricLinesOfCode = rendererItem.getLinesOfCode().getValue().intValue();
            double percentage = 100.0 * metricLinesOfCode / maxLinesOfCode;
            String filesFragment = StringUtils.defaultIfBlank(rendererItem.getFilesFragment(), "");

            report.addContentInDiv(chart.getPercentageSvg(percentage, rendererItem.getLinesOfCode().getName(),
                    "" + metricLinesOfCode + " LOC (" +
                            StringEscapeUtils.escapeHtml4(FormattingUtils.getFormattedPercentage(percentage))
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

        boolean criteriaDefined = aspect != null && aspect.getSourceFileFilters().size() > 0;
        if (criteriaDefined) {
            report.startUnorderedList();
            report.addListItem("The following criteria are used to filter files:");
            report.startUnorderedList();
            aspect.getSourceFileFilters().forEach(filter -> report.addListItem(describeFilters(filter)));
            report.endUnorderedList();
            report.endUnorderedList();
        }

        if (filesCount == 0) {
            report.startUnorderedList();
            report.addListItem("There are no \"" + title.toLowerCase() + "\" files.");
            report.endUnorderedList();
            return;
        }

        if (StringUtils.isNotBlank(description)) {
            report.startUnorderedList();
            report.addListItem(description);
            report.endUnorderedList();
        }
        report.startUnorderedList();
        if (criteriaDefined) {
            String filesFragment;
            String filesPhrase = filesCount == 1 ? "file" : "files";
            if (StringUtils.isNotBlank(filesListPath)) {
                filesFragment = "<a href='../data/text/" + filesListPath + "'><b>" + filesCount + "</b> " + filesPhrase + "</a>";
            } else {
                filesFragment = "<b>" + filesCount + "</b> " + filesPhrase;
            }
            report.addListItem(filesFragment + " match" + (filesCount == 1 ? "es" : "") + " defined criteria (" +
                    "<b>" + RichTextRenderingUtils.renderNumber(linesCount) + "</b> lines of code, "
                    + "<b>" + RichTextRenderingUtils.renderNumber(100.0 * linesCount / linesOfCodeInMain) + "%</b> vs. main code)"
                    + (fileCountPerComponent.size() == 1 ? ". All matches are in " + fileCountPerComponent.get(0).getName() + " files." : ":"));
            report.startUnorderedList();
            if (fileCountPerComponent.size() > 1) {
                for (int i = 0; i < fileCountPerComponent.size(); i++) {
                    NumericMetric fileCountMetric = fileCountPerComponent.get(i);
                    NumericMetric linesOfCodeMetric = linesOfCode.get(i);
                    report.addListItem("<b>" + RichTextRenderingUtils.renderNumber(fileCountMetric.getValue().intValue()) + "</b>"
                            + " " + fileCountMetric.getName() + " files"
                            + " (<b>" + RichTextRenderingUtils.renderNumber(linesOfCodeMetric.getValue().intValue()) + "</b> lines of code)");
                }
            }
            report.endUnorderedList();
            if (totalNumberOfRegexMatches > 0) {
                report.addListItem(totalNumberOfRegexMatches == 1
                        ? "<b>1</b> line matches the content pattern."
                        : "<b>" + RichTextRenderingUtils.renderNumber(totalNumberOfRegexMatches) + "</b> lines match the content pattern.");
            }
        } else {
            report.addListItem("<b>" + RichTextRenderingUtils.renderNumber(filesCount) + "</b> files, " +
                    "<b>" + RichTextRenderingUtils.renderNumber(linesCount) + "</b> lines of code ("
                    + "<b>" + RichTextRenderingUtils.renderNumber(100.0 * linesCount / maxLinesOfCode) + "%</b> vs. main code).");
        }

        report.endUnorderedList();

    }

    public NamedSourceCodeAspect getAspect() {
        return aspect;
    }

    public void setAspect(NamedSourceCodeAspect aspect) {
        this.aspect = aspect;
    }
}
