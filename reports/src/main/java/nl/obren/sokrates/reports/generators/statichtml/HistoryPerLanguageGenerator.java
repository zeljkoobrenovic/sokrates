package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.sourcecode.analysis.results.HistoryPerExtension;
import nl.obren.sokrates.sourcecode.landscape.MergeExtension;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HistoryPerLanguageGenerator {
    public static final int MAX_HISTORY_WINDOW_SIZE = 20;
    public static final int MAX_NUMBER_OF_EXTENSIONS = 100;
    private Mode mode = Mode.COMMITS;
    private List<String> extensions = null;

    private int currentYear = Calendar.getInstance().get(Calendar.YEAR);
    private int max = 1;
    private int firstYear = 0;
    private List<HistoryPerExtension> history = new ArrayList<>();
    private RichTextReport report;

    private List<MergeExtension> mergeExtensions = new ArrayList<>();

    private HistoryPerLanguageGenerator(Mode mode, List<HistoryPerExtension> history) {
        this.mode = mode;
        this.history = history;
    }

    public static HistoryPerLanguageGenerator getInstanceCommits(List<HistoryPerExtension> history) {
        return new HistoryPerLanguageGenerator(Mode.COMMITS, history);
    }

    public static HistoryPerLanguageGenerator getInstanceCommits(List<HistoryPerExtension> history, List<String> extensions) {
        HistoryPerLanguageGenerator historyPerLanguageGenerator = new HistoryPerLanguageGenerator(Mode.COMMITS, history);
        historyPerLanguageGenerator.setExtensions(extensions);
        return historyPerLanguageGenerator;
    }

    public static HistoryPerLanguageGenerator getInstanceContributors(List<HistoryPerExtension> history) {
        return new HistoryPerLanguageGenerator(Mode.CONTRIBUTORS, history);
    }

    public static HistoryPerLanguageGenerator getInstanceContributors(List<HistoryPerExtension> history, List<String> extensions) {
        HistoryPerLanguageGenerator historyPerLanguageGenerator = new HistoryPerLanguageGenerator(Mode.CONTRIBUTORS, history);
        historyPerLanguageGenerator.setExtensions(extensions);
        return historyPerLanguageGenerator;
    }

    public void addHistoryPerLanguage(RichTextReport report) {
        this.report = report;
        List<HistoryPerExtension> mergedHistory = new ArrayList<>();
        Map<String, HistoryPerExtension> mergedHistoryMap = new HashMap<>();

        this.history.forEach(languageHistory -> {
            String extension = languageHistory.getExtension();
            String mergedExtension = this.getMergedExtension(extension);
            String year = languageHistory.getYear();

            String key = mergedExtension + "::" + year;

            if (mergedHistoryMap.containsKey(key)) {
                HistoryPerExtension existing = mergedHistoryMap.get(key);
                existing.setCommitsCount(existing.getCommitsCount() + languageHistory.getCommitsCount());
                existing.getContributors().addAll(languageHistory.getContributors());
            } else {
                HistoryPerExtension newItem = new HistoryPerExtension(languageHistory.getExtension(),
                        languageHistory.getYear(),
                        languageHistory.getCommitsCount());
                newItem.getContributors().addAll(languageHistory.getContributors());
                mergedHistoryMap.put(key, newItem);
                mergedHistory.add(newItem);
            }
        });

        this.max = mergedHistory.stream().mapToInt(value -> getValue(value)).max().orElse(0);
        this.firstYear = mergedHistory.stream().mapToInt(value -> getYearInteger(value)).min().orElse(0);

        report.startTable();
        addHeader();
        getExtensions().stream().limit(MAX_NUMBER_OF_EXTENSIONS).forEach(extension -> {
            addExtensionRow(extension);
        });
        report.endTable();
    }

    public int getYearInteger(HistoryPerExtension value) {
        try {
            return Integer.parseInt(value.getYear());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private List<String> getExtensions() {
        List<String> list;

        if (this.extensions != null && this.extensions.size() > 0) {
            list = this.extensions;
        } else {
            Set<String> extensions = new HashSet<>();

            this.history.forEach(h -> extensions.add(h.getExtension()));

            list = new ArrayList(extensions);
        }

        Collections.sort(list, (a, b) -> getSortValue(b) - getSortValue(a));

        return list;
    }

    public void setExtensions(List<String> extensions) {
        this.extensions = extensions;
    }

    private int getSortValue(String extension) {
        int thisYear = getCommitsCount(extension, currentYear) != null ? getCommitsCount(extension, currentYear) : 0;
        int prevYear = getCommitsCount(extension, currentYear - 1) != null ? getCommitsCount(extension, currentYear - 1) : 0;
        return thisYear + prevYear;
    }

    public int getValue(HistoryPerExtension value) {
        return mode == Mode.COMMITS ? value.getCommitsCount() : value.getContributors().size();
    }

    private void addExtensionRow(String extension) {
        report.startTableRow();
        report.addTableCell(DataImageUtils.getLangDataImageDiv30(extension), "text-align: center; color: grey; font-size: 80%; vertical-align: bottom; border: none;");
        for (int year = endYear(); year >= startYear(); year--) {
            addCell(extension, year);
        }
        report.addTableCell(extension, "text-align: left; color: grey; font-size: 80%; vertical-align: middle; border: none;");
        report.endTableRow();
    }

    private void addCell(String name, int year) {
        Integer value = getCommitsCount(name, year);
        boolean recent = year >= currentYear - 1;
        if (value != null) {
            int height = 1 + (int) (64.0 * value.intValue() / max);
            report.startTableCell("border: none; padding: 1px; text-align: center; vertical-align: bottom");
            report.addContentInDiv("" + value.intValue(),
                    "margin: auto; color: grey; font-size: 60%");
            report.addContentInDivWithTooltip("", "",
                    "margin: auto; width: 32px; " +
                            "background-color: " + getBackgroundColor() + "; " +
                            "opacity: " + (recent ? "1.0" : "0.5") + "; " +
                            "height:" + height + "px");
            report.endTableCell();
        } else {
            report.startTableCell("border: none; padding: 1px; text-align: center; vertical-align: bottom");
            report.addContentInDivWithTooltip("", "",
                    "margin: auto; width: 32px; background-color: lightgrey; height: 1px");
            report.endTableCell();
        }
    }

    private String getBackgroundColor() {
        return mode == Mode.COMMITS ? "#343434" : "skyblue";
    }

    private Integer getCommitsCount(String name, int year) {
        Stream<HistoryPerExtension> historyPerExtensionFiltered = this.history.stream().filter(h -> h.getExtension().equalsIgnoreCase(name) && h.getYear().equalsIgnoreCase(year + ""));
        List<HistoryPerExtension> list = historyPerExtensionFiltered.collect(Collectors.toList());
        return list.size() > 0 ? list.stream().mapToInt(item -> getValue(item)).sum() : null;
    }

    private void addHeader() {
        report.startTableRow();
        report.addTableCell("", "border: none; text-align: center");
        for (int year = endYear(); year >= startYear(); year--) {
            report.addTableCell("" + year, "border: none; padding: 0; font-size: 60%; text-align: center");
        }
        report.addTableCell("", "border: none; text-align: center");
        report.endTableRow();
    }

    private int endYear() {
        return this.currentYear;
    }

    private int startYear() {
        return Math.max(firstYear, this.endYear() - MAX_HISTORY_WINDOW_SIZE);
    }

    public List<MergeExtension> getMergeExtensions() {
        return mergeExtensions;
    }

    public void setMergeExtensions(List<MergeExtension> mergeExtensions) {
        this.mergeExtensions = mergeExtensions;
    }

    private String getMergedExtension(String extension) {
        for (MergeExtension mergeExtension : mergeExtensions) {
            if (mergeExtension.getSecondary().equalsIgnoreCase(extension)) {
                return mergeExtension.getPrimary().toLowerCase();
            }
        }
        return extension.toLowerCase();
    }

    public enum Mode {COMMITS, CONTRIBUTORS}
}
