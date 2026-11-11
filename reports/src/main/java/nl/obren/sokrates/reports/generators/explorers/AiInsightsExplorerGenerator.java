/*
 * Copyright (c) 2026 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.explorers;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.obren.sokrates.common.renderingutils.ExplorerTemplate;
import nl.obren.sokrates.reports.core.ReportNavigation;
import nl.obren.sokrates.reports.core.RichTextReport;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The findings of the sokrates-skills AI scanners in the repository report. The scanners write
 * <code>&lt;reports&gt;/ai-insights/&lt;scanner&gt;.json</code> (format owned by sokrates-skills: scan-core
 * <code>schema/findings.schema.json</code>; only <code>scanner</code> and <code>findings[]</code> are required).
 * When there are any, the report gets <code>explorers/ai-insights.html</code> (the sokrates-skills explorer
 * without its own sidebar, following the report's theme; template <code>ai-insights-explorer.html</code>),
 * the icons it uses, an index tab that shows it, and sidebar groups (AI Insights: Overview, Attention Items
 * and the basic scanners; AI Deep Dives: the evaluative scanners) whose items open it on
 * <code>?view=&lt;overview|attention|scanner id&gt;</code>. The scanner metadata and icons are bundled copies of
 * sokrates-skills' scan-core <code>templates/scanners.json</code> and <code>templates/icons/</code>.
 */
public class AiInsightsExplorerGenerator {
    private static final Log LOG = LogFactory.getLog(AiInsightsExplorerGenerator.class);

    public static final String INSIGHTS_FOLDER = "ai-insights";
    public static final String TAB_ID = "ai-insights";
    public static final String PAGE = "ai-insights.html";
    static final String ICONS_FOLDER = "ai-insights-icons";
    private static final String EXPLORERS_FOLDER = "explorers";
    private static final String RESOURCES = "ai-insights/";
    // The page and its icons live in <reports>/explorers/; the index in <reports>/html/.
    private static final String FRAME_SRC = "../" + EXPLORERS_FOLDER + "/" + PAGE;

    private final List<Map<String, Object>> docs;
    private final List<Map<String, Object>> meta;

    private AiInsightsExplorerGenerator(List<Map<String, Object>> docs, List<Map<String, Object>> meta) {
        this.docs = docs;
        this.meta = meta;
    }

    /** The findings documents in <code>&lt;reports&gt;/ai-insights/</code>, in the scanners' display order (none when there are none). */
    public static AiInsightsExplorerGenerator load(File reportsFolder) {
        List<Map<String, Object>> meta = loadMeta();
        List<Map<String, Object>> docs = new ArrayList<>();
        File folder = new File(reportsFolder, INSIGHTS_FOLDER);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (files != null) {
            Arrays.sort(files);
            ObjectMapper mapper = new ObjectMapper();
            for (File file : files) {
                Map<String, Object> doc = readFindings(mapper, file);
                if (doc != null) {
                    doc.put("_file", "../" + INSIGHTS_FOLDER + "/" + file.getName());
                    attachVisual(doc, folder);
                    docs.add(doc);
                }
            }
        }
        Map<String, Integer> order = new HashMap<>();
        for (int i = 0; i < meta.size(); i++) {
            order.put(String.valueOf(meta.get(i).get("id")), i);
        }
        docs.sort(Comparator.comparing((Map<String, Object> d) -> order.getOrDefault(scanner(d), Integer.MAX_VALUE))
                .thenComparing(AiInsightsExplorerGenerator::scanner));
        return new AiInsightsExplorerGenerator(docs, meta);
    }

    // A findings document: an object with a "scanner" and a "findings" list, other than the merged
    // combined-report.json (the page builds its own cross-scanner views). Anything else is skipped.
    @SuppressWarnings("unchecked")
    private static Map<String, Object> readFindings(ObjectMapper mapper, File file) {
        try {
            Object value = mapper.readValue(file, Object.class);
            if (!(value instanceof Map)) {
                return null;
            }
            Map<String, Object> doc = (Map<String, Object>) value;
            Object scanner = doc.get("scanner");
            if (!(scanner instanceof String) || ((String) scanner).isEmpty() || "combined".equals(scanner)
                    || !(doc.get("findings") instanceof List)) {
                return null;
            }
            return doc;
        } catch (IOException e) {
            LOG.warn("Skipping the unreadable AI findings file " + file.getPath() + ": " + e.getMessage());
            return null;
        }
    }

    // An optional summary_visual.file (relative to the ai-insights folder) is shown when the file exists.
    @SuppressWarnings("unchecked")
    private static void attachVisual(Map<String, Object> doc, File folder) {
        Object visual = doc.get("summary_visual");
        if (!(visual instanceof Map)) {
            return;
        }
        Map<String, Object> map = (Map<String, Object>) visual;
        Object file = map.get("file");
        String path = file == null ? "" : file.toString().replace('\\', '/');
        if (path.isEmpty() || path.startsWith("/") || path.contains("..") || !new File(folder, path).isFile()) {
            doc.remove("summary_visual");
            return;
        }
        map.put("src", "../" + INSIGHTS_FOLDER + "/" + path);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> loadMeta() {
        try (InputStream in = AiInsightsExplorerGenerator.class.getClassLoader().getResourceAsStream(RESOURCES + "scanners.json")) {
            if (in == null) {
                return new ArrayList<>();
            }
            Map<String, Object> doc = new ObjectMapper().readValue(in, Map.class);
            Object scanners = doc.get("scanners");
            List<Map<String, Object>> list = new ArrayList<>();
            if (scanners instanceof List) {
                ((List<Object>) scanners).forEach(s -> {
                    if (s instanceof Map && ((Map<String, Object>) s).get("id") != null) {
                        list.add((Map<String, Object>) s);
                    }
                });
            }
            return list;
        } catch (IOException e) {
            LOG.warn("Cannot read the AI scanner metadata: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private static String scanner(Map<String, Object> doc) {
        return String.valueOf(doc.get("scanner"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> findings(Map<String, Object> doc) {
        List<Map<String, Object>> list = new ArrayList<>();
        ((List<Object>) doc.get("findings")).forEach(f -> {
            if (f instanceof Map) {
                list.add((Map<String, Object>) f);
            }
        });
        return list;
    }

    public boolean isEmpty() {
        return docs.isEmpty();
    }

    public List<String> getScanners() {
        List<String> scanners = new ArrayList<>();
        docs.forEach(d -> scanners.add(scanner(d)));
        return scanners;
    }

    private Map<String, Object> metaOf(String scanner) {
        return meta.stream().filter(m -> scanner.equals(String.valueOf(m.get("id")))).findFirst().orElse(null);
    }

    private String nameOf(String scanner) {
        Map<String, Object> m = metaOf(scanner);
        if (m != null && m.get("name") != null) {
            return m.get("name").toString();
        }
        String words = scanner.replaceAll("-scan$", "").replace('-', ' ').replace('_', ' ');
        StringBuilder name = new StringBuilder();
        for (String word : words.split(" ")) {
            if (!word.isEmpty()) {
                name.append(name.length() > 0 ? " " : "").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return name.toString();
    }

    private boolean isDeepDive(String scanner) {
        Map<String, Object> m = metaOf(scanner);
        return m != null && "deep-dive".equals(m.get("tier"));
    }

    private static String severity(Map<String, Object> finding) {
        Object severity = finding.get("severity");
        return severity instanceof String && Arrays.asList("critical", "high", "medium", "low").contains(severity) ? (String) severity : "info";
    }

    // "12 findings, 2 high, 3 medium" (above-info severities only).
    private static String countsText(List<Map<String, Object>> findings) {
        StringBuilder text = new StringBuilder(findings.size() + (findings.size() == 1 ? " finding" : " findings"));
        for (String severity : Arrays.asList("critical", "high", "medium", "low")) {
            long count = findings.stream().filter(f -> severity.equals(severity(f))).count();
            if (count > 0) {
                text.append(", ").append(count).append(" ").append(severity);
            }
        }
        return text.toString();
    }

    private List<Map<String, Object>> allFindings() {
        List<Map<String, Object>> all = new ArrayList<>();
        docs.forEach(d -> all.addAll(findings(d)));
        return all;
    }

    private String iconPath(String id) {
        return "../" + EXPLORERS_FOLDER + "/" + ICONS_FOLDER + "/" + id + ".png";
    }

    private static boolean hasBundledIcon(String id) {
        return AiInsightsExplorerGenerator.class.getClassLoader().getResource(RESOURCES + "icons/" + id + ".png") != null;
    }

    /**
     * The sidebar groups, placed by the caller after the analyses and before the Index group: AI Insights
     * (Overview, Attention Items, the basic scanners) and, when there are any, AI Deep Dives.
     */
    public void addNavigation(ReportNavigation navigation) {
        if (isEmpty()) {
            return;
        }
        List<Map<String, Object>> all = allFindings();
        long attention = all.stream().filter(f -> !"info".equals(severity(f))).count();
        ReportNavigation.Group insights = navigation.addGroup("AI Insights");
        addItem(navigation, insights, "overview", "Overview",
                "Findings of " + docs.size() + (docs.size() == 1 ? " AI scanner" : " AI scanners") + ": " + countsText(all) + ".");
        addItem(navigation, insights, "attention", "Attention Items",
                attention + (attention == 1 ? " finding" : " findings") + " with a severity above info, across all scanners.");
        ReportNavigation.Group deepDives = null;
        for (Map<String, Object> doc : docs) {
            String scanner = scanner(doc);
            ReportNavigation.Group group = insights;
            if (isDeepDive(scanner)) {
                if (deepDives == null) {
                    deepDives = navigation.addGroup("AI Deep Dives");
                }
                group = deepDives;
            }
            // The header subtitle stays one line: the counts (the page shows the scanner's description).
            addItem(navigation, group, scanner, nameOf(scanner), countsText(findings(doc)) + ".");
        }
        navigation.setPageTitle(TAB_ID + "/overview", "AI Insights");
    }

    private void addItem(ReportNavigation navigation, ReportNavigation.Group group, String view, String label, String subtitle) {
        group.addFrameTabItem(TAB_ID, view, label, "ai", FRAME_SRC + "?view=" + URLEncoder.encode(view, UTF_8), subtitle);
        if (hasBundledIcon(view)) {
            navigation.setPageIcon(TAB_ID + "/" + view, iconPath(view), true);
        }
    }

    /** The index tab whose iframe the sidebar items point at their view (loaded when first shown). */
    public void addTab(RichTextReport indexReport) {
        if (isEmpty()) {
            return;
        }
        indexReport.startTabContentSection(TAB_ID, false);
        indexReport.addLineBreak();
        indexReport.addHtmlContent("<iframe data-sk-src='" + FRAME_SRC + "?view=overview' "
                + "style='width: 100%; border: none; height: calc(100vh - 220px); overflow: hidden; margin-top: -12px'></iframe>");
        indexReport.endTabContentSection();
    }

    /** Writes explorers/ai-insights.html and the icons it uses (nothing when there are no findings). */
    public void export(File reportsFolder) {
        if (isEmpty()) {
            return;
        }
        File folder = new File(reportsFolder, EXPLORERS_FOLDER);
        File iconsFolder = new File(folder, ICONS_FOLDER);
        iconsFolder.mkdirs();
        Map<String, String> icons = new LinkedHashMap<>();
        List<String> ids = new ArrayList<>(Arrays.asList("overview", "attention"));
        ids.addAll(getScanners());
        for (String id : ids) {
            if (copyIcon(id, iconsFolder)) {
                icons.put(id, ICONS_FOLDER + "/" + id + ".png");
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("docs", docs);
        data.put("meta", meta);
        data.put("icons", icons);
        data.put("reportIndex", "../html/index.html");
        try {
            String html = new ExplorerTemplate().render("ai-insights-explorer.html", data, new HashMap<>());
            FileUtils.write(new File(folder, PAGE), html, UTF_8);
        } catch (IOException e) {
            LOG.error("Cannot write the AI Insights page: " + e.getMessage());
        }
    }

    private static boolean copyIcon(String id, File iconsFolder) {
        try (InputStream in = AiInsightsExplorerGenerator.class.getClassLoader().getResourceAsStream(RESOURCES + "icons/" + id + ".png")) {
            if (in == null) {
                return false;
            }
            FileUtils.writeByteArrayToFile(new File(iconsFolder, id + ".png"), IOUtils.toByteArray(in));
            return true;
        } catch (IOException e) {
            LOG.warn("Cannot copy the AI scanner icon " + id + ": " + e.getMessage());
            return false;
        }
    }
}
