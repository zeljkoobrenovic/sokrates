package nl.obren.sokrates.reports.landscape.ai;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.obren.sokrates.sourcecode.landscape.analysis.RepositoryAnalysisResults;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Collects the AI scanner findings of every repository of a landscape — the
 * {@code <repository>/reports/ai-insights/<scanner>.json} files the sokrates-skills scanners write
 * (via the -postAnalysis hook or by hand) — into one {@link AiInsightsLandscapeExport}: the
 * repositories with results, their scanners, and every finding (title, severity, confidence,
 * group, a shortened description and recommendation, a deep link into the repository's own AI
 * Insights explorer). The per-repository format is owned by sokrates-skills (scan-core); this
 * reads it leniently — only {@code scanner} and {@code findings[]} are required — and skips
 * {@code combined-report.json} (a machine artifact that repeats the others).
 */
public class AiInsightsAggregator {
    public static final String AI_INSIGHTS_FOLDER = "ai-insights";
    public static final List<String> SEVERITIES = Arrays.asList("critical", "high", "medium", "low", "info");
    static final int DESCRIPTION_LIMIT = 320;
    static final int RECOMMENDATION_LIMIT = 240;
    static final int SUMMARY_LIMIT = 400;

    private static final Log LOG = LogFactory.getLog(AiInsightsAggregator.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * @param repositories    the landscape's repositories
     * @param landscapeFolder the folder the landscape report is written to (the links are relative to it)
     * @param prefix          the landscape's repositoryReportsUrlPrefix ("../" by default)
     */
    public static AiInsightsLandscapeExport aggregate(List<RepositoryAnalysisResults> repositories, File landscapeFolder, String prefix) {
        AiInsightsLandscapeExport export = new AiInsightsLandscapeExport();
        for (RepositoryAnalysisResults repository : repositories) {
            String analysisResultsPath = repository.getSokratesRepositoryLink().getAnalysisResultsPath().replace("\\", "/");
            // <repo>/reports/data/analysisResults.json -> <repo>/reports
            // normalized lexically: the "../" prefix must resolve even when the landscape folder does not exist yet
            File reportsFolder = new File(landscapeFolder, prefix + analysisResultsPath).toPath().toAbsolutePath().normalize().getParent().getParent().toFile();
            File aiFolder = new File(reportsFolder, AI_INSIGHTS_FOLDER);
            if (!aiFolder.isDirectory()) {
                continue;
            }
            String reportsRelative = analysisResultsPath.replaceAll("/data/analysisResults\\.json$", "");
            String insightsUrl = prefix + reportsRelative + "/" + AI_INSIGHTS_FOLDER + "/index.html";
            String reportUrl = prefix + reportsRelative + "/index.html";
            String name = repository.getAnalysisResults().getMetadata().getName();
            AiRepositoryExport repositoryExport = new AiRepositoryExport();
            repositoryExport.setName(name);
            repositoryExport.setInsightsUrl(insightsUrl);
            repositoryExport.setReportUrl(reportUrl);
            File[] files = aiFolder.listFiles((dir, fileName) -> fileName.endsWith(".json") && !fileName.equals("combined-report.json"));
            Arrays.sort(files == null ? new File[0] : files);
            for (File file : files == null ? new File[0] : files) {
                try {
                    addScanner(export, repositoryExport, name, insightsUrl, MAPPER.readTree(FileUtils.readFileToString(file, StandardCharsets.UTF_8)));
                } catch (Exception e) {
                    LOG.warn("Skipping " + file.getPath() + ": " + e.getMessage());
                }
            }
            if (!repositoryExport.getScanners().isEmpty()) {
                export.getRepositories().add(repositoryExport);
            }
        }
        export.getFindings().sort(Comparator.comparingInt((AiFindingExport f) -> severityRank(f.getSeverity()))
                .thenComparing(AiFindingExport::getRepo).thenComparing(AiFindingExport::getScanner).thenComparing(AiFindingExport::getTitle));
        export.getRepositories().sort(Comparator.comparingInt(AiRepositoryExport::getAttention).reversed().thenComparing(AiRepositoryExport::getName));
        return export;
    }

    /** Adds one scanner document (the parsed findings JSON) of a repository; ignored when it has no scanner name or findings list. */
    static void addScanner(AiInsightsLandscapeExport export, AiRepositoryExport repository, String repoName, String insightsUrl, JsonNode document) {
        String scanner = document.path("scanner").asText("");
        JsonNode findings = document.path("findings");
        if (StringUtils.isBlank(scanner) || !findings.isArray() || "combined".equals(scanner)) {
            return;
        }
        AiScannerExport scannerExport = new AiScannerExport();
        scannerExport.setScanner(scanner);
        scannerExport.setVersion(document.path("scanner_version").asText(""));
        scannerExport.setAnalyzedAt(document.path("analyzed_at").asText(""));
        scannerExport.setSummary(shorten(document.path("summary").asText(""), SUMMARY_LIMIT));
        for (JsonNode node : findings) {
            AiFindingExport finding = new AiFindingExport();
            finding.setRepo(repoName);
            finding.setScanner(scanner);
            finding.setId(node.path("id").asText(""));
            finding.setGroup(node.path("group").asText(""));
            finding.setTitle(node.path("title").asText(""));
            String severity = node.path("severity").asText("info").toLowerCase();
            finding.setSeverity(SEVERITIES.contains(severity) ? severity : "info");
            finding.setConfidence(node.path("confidence").asText(""));
            finding.setDescription(shorten(node.path("description").asText(""), DESCRIPTION_LIMIT));
            finding.setRecommendation(shorten(node.path("recommendation").asText(""), RECOMMENDATION_LIMIT));
            node.path("tags").forEach(tag -> finding.getTags().add(tag.asText("")));
            finding.setUrl(insightsUrl + (StringUtils.isNotBlank(finding.getId()) ? "#" + URLEncoder.encode(finding.getId(), StandardCharsets.UTF_8) : ""));
            export.getFindings().add(finding);
            scannerExport.setFindings(scannerExport.getFindings() + 1);
            repository.setFindings(repository.getFindings() + 1);
            repository.getFindingsBySeverity().merge(finding.getSeverity(), 1, Integer::sum);
            if (!"info".equals(finding.getSeverity())) {
                scannerExport.setAttention(scannerExport.getAttention() + 1);
                repository.setAttention(repository.getAttention() + 1);
            }
        }
        repository.getScanners().add(scannerExport);
        if (scannerExport.getAnalyzedAt().compareTo(repository.getScannedAt()) > 0) {
            repository.setScannedAt(scannerExport.getAnalyzedAt());
        }
    }

    static int severityRank(String severity) {
        int index = SEVERITIES.indexOf(severity);
        return index < 0 ? SEVERITIES.size() : index;
    }

    static String shorten(String text, int limit) {
        String value = StringUtils.defaultString(text).trim();
        return value.length() <= limit ? value : value.substring(0, limit - 1).trim() + "…";
    }

    /** The totals per severity over all findings, in severity order (for the page's tiles). */
    public static Map<String, Integer> totalsBySeverity(AiInsightsLandscapeExport export) {
        Map<String, Integer> totals = new java.util.LinkedHashMap<>();
        SEVERITIES.forEach(severity -> totals.put(severity, 0));
        export.getFindings().forEach(finding -> totals.merge(finding.getSeverity(), 1, Integer::sum));
        return totals;
    }
}
