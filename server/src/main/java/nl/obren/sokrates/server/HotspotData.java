package nl.obren.sokrates.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.obren.sokrates.server.api.Api;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * Hotspots from two data.zip entries: {@code mainFiles.json} (per main file: lines of code and the McCabe sum of its
 * units) and {@code text/mainFilesWithHistory.txt} (tab-separated, with a header: days changed, commits per window,
 * contributors, last update), whose columns are found by their header names.
 * <p>
 * The default ranking is the Highlights tab's: complexity × days changed in the past 365 days. Analyses made before the
 * export had the {@code # active days (365d)} column fall back to complexity × commits in the past 90 days.
 */
class HotspotData {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    // FileListsDataExporter.ACTIVE_DAYS_365_COLUMN (the server reads the text, it does not depend on the exporter's constant)
    static final String ACTIVE_DAYS_365_COLUMN = "# active days (365d)";

    record FileHistory(String path, Integer activeDays365, int activeDays, int commits, int commits30Days, int commits90Days,
                       int contributors, String lastUpdated) {
    }

    /** One way to count changes: the window label, the basis text and the per-file count. */
    private record Measure(String window, String changes, ToIntFunction<FileHistory> count) {
    }

    private static final Measure DAYS_365 = new Measure("365 days", "days changed in the past 365 days", f -> f.activeDays365() == null ? 0 : f.activeDays365());
    private static final Measure COMMITS_90 = new Measure("90 days", "commits in the past 90 days", FileHistory::commits90Days);
    private static final Measure DAYS_ALL = new Measure("all time", "days changed (all time)", FileHistory::activeDays);

    /** Fills path → McCabe sum and path → lines of code; true when some file has units. */
    static boolean readComplexity(String json, Map<String, Integer> complexity, Map<String, Integer> linesOfCode) {
        boolean anyUnits = false;
        if (json == null) {
            return false;
        }
        try {
            JsonNode files = MAPPER.readTree(json);
            for (JsonNode file : files) {
                String path = file.path("relativePath").asText("");
                if (path.isEmpty()) {
                    continue;
                }
                int mcCabe = file.path("unitsMcCabeIndexSum").asInt(0);
                anyUnits |= mcCabe > 0;
                complexity.put(path, mcCabe);
                linesOfCode.put(path, file.path("linesOfCode").asInt(0));
            }
        } catch (Exception e) {
            return false;
        }
        return anyUnits;
    }

    static List<FileHistory> readHistory(String text) {
        List<FileHistory> history = new ArrayList<>();
        if (StringUtils.isBlank(text)) {
            return history;
        }
        String[] lines = text.split("\\r?\\n");
        Map<String, Integer> columns = new HashMap<>();
        String[] header = lines[0].split("\t");
        for (int i = 0; i < header.length; i++) {
            columns.put(header[i].trim(), i);
        }
        boolean has365 = columns.containsKey(ACTIVE_DAYS_365_COLUMN);
        for (int i = 1; i < lines.length; i++) {
            String[] cells = lines[i].split("\t");
            if (cells.length < 2) {
                continue;
            }
            history.add(new FileHistory(cells[0],
                    has365 ? number(cells, columns.get(ACTIVE_DAYS_365_COLUMN)) : null,
                    number(cells, columns.get("# active days")),
                    number(cells, columns.get("# commits")), number(cells, columns.get("# commits (30d)")),
                    number(cells, columns.get("# commits (90d)")), number(cells, columns.get("# contributors")),
                    text(cells, columns.get("last updated"))));
        }
        return history;
    }

    /**
     * @param window "365" (the default; 90-day commits for older analyses), "90" (commits in the past 90 days) or
     *               "all" (days changed, all time). When nothing changed in the window, all time is used.
     */
    static Api.Hotspots rank(String repository, Map<String, Integer> complexity, Map<String, Integer> linesOfCode, boolean anyUnits,
                             List<FileHistory> history, String window, int limit) {
        String complexityMeasure = anyUnits ? "McCabe sum of the file's units" : "lines of code";
        Map<String, FileHistory> byPath = new HashMap<>();
        history.forEach(file -> byPath.put(file.path(), file));
        if (history.isEmpty()) {
            List<Api.Hotspot> hotspots = score(complexity, linesOfCode, anyUnits, byPath, null);
            return new Api.Hotspots(repository, "none", complexityMeasure + " (no git history)", null, null, top(hotspots, limit));
        }
        Measure measure = measure(window, history.stream().anyMatch(file -> file.activeDays365() != null));
        List<Api.Hotspot> hotspots = score(complexity, linesOfCode, anyUnits, byPath, measure);
        if (hotspots.isEmpty() && measure != DAYS_ALL) {
            measure = DAYS_ALL;
            hotspots = score(complexity, linesOfCode, anyUnits, byPath, measure);
        }
        return new Api.Hotspots(repository, measure.window(), complexityMeasure + " × " + measure.changes(), null, null, top(hotspots, limit));
    }

    private static Measure measure(String window, boolean has365) {
        String value = StringUtils.trimToEmpty(window).toLowerCase(Locale.ROOT);
        switch (value) {
            case "":
            case "365":
                return has365 ? DAYS_365 : COMMITS_90;
            case "90":
                return COMMITS_90;
            case "all":
                return DAYS_ALL;
            default:
                throw new IllegalArgumentException("window must be 365, 90 or all");
        }
    }

    // Complexity × changes per file (files that did not change are left out); complexity alone without a measure.
    private static List<Api.Hotspot> score(Map<String, Integer> complexity, Map<String, Integer> linesOfCode, boolean anyUnits,
                                           Map<String, FileHistory> byPath, Measure measure) {
        List<Api.Hotspot> hotspots = new ArrayList<>();
        complexity.forEach((path, mcCabe) -> {
            int lines = linesOfCode.getOrDefault(path, 0);
            int value = anyUnits ? mcCabe : lines;
            if (value <= 0) {
                return;
            }
            FileHistory file = byPath.get(path);
            long changes = measure == null ? 1 : (file == null ? 0 : measure.count().applyAsInt(file));
            if (changes == 0) {
                return;
            }
            hotspots.add(new Api.Hotspot(path, lines, value,
                    file == null ? null : file.activeDays365(), file == null ? 0 : file.activeDays(),
                    file == null ? 0 : file.commits30Days(), file == null ? 0 : file.commits90Days(), file == null ? 0 : file.commits(),
                    file == null ? 0 : file.contributors(), file == null ? null : file.lastUpdated(), value * changes));
        });
        hotspots.sort(Comparator.comparingLong(Api.Hotspot::score).reversed().thenComparing(Api.Hotspot::path));
        return hotspots;
    }

    private static List<Api.Hotspot> top(List<Api.Hotspot> hotspots, int limit) {
        return hotspots.size() > limit ? new ArrayList<>(hotspots.subList(0, limit)) : hotspots;
    }

    private static int number(String[] cells, Integer column) {
        if (column == null || column >= cells.length) {
            return 0;
        }
        try {
            return Integer.parseInt(cells[column].trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String text(String[] cells, Integer column) {
        return column == null || column >= cells.length ? null : StringUtils.trimToNull(cells[column]);
    }
}
