package nl.obren.sokrates.reports.landscape.ai;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.obren.sokrates.reports.generators.explorers.AiCostEstimatorData;
import nl.obren.sokrates.reports.generators.explorers.AiCostEstimatorGenerator;
import nl.obren.sokrates.sourcecode.landscape.analysis.RepositoryAnalysisResults;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Merges the repositories' AI Cost Estimator data (data/aiCostEstimator.json inside each
 * repository's data/data.zip, written by every analysis) into the landscape's estimator: the tasks
 * of all repositories (each tagged with its repository), authors merged by email, the noise counts
 * summed. Repositories analyzed by an older version have no such entry and are skipped. The newest
 * {@link #MAX_TASKS} tasks are kept, so the page stays responsive on big landscapes.
 */
public class AiCostEstimatorAggregator {
    private static final Log LOG = LogFactory.getLog(AiCostEstimatorAggregator.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    static final int MAX_TASKS = 50000;

    /**
     * @param repositories    the landscape's repositories
     * @param landscapeFolder the folder the landscape report is written to (the links are relative to it)
     * @param prefix          the landscape's repositoryReportsUrlPrefix ("../" by default)
     */
    public static AiCostEstimatorData aggregate(List<RepositoryAnalysisResults> repositories, File landscapeFolder, String prefix) {
        List<String> names = new ArrayList<>();
        List<String> urls = new ArrayList<>();
        List<AiCostEstimatorData> data = new ArrayList<>();
        for (RepositoryAnalysisResults repository : repositories) {
            String analysisResultsPath = repository.getSokratesRepositoryLink().getAnalysisResultsPath().replace("\\", "/");
            // <repo>/reports/data/analysisResults.json -> <repo>/reports/data, resolved lexically (see AiInsightsAggregator)
            File dataFolder = new File(landscapeFolder, prefix + analysisResultsPath).toPath().toAbsolutePath().normalize().getParent().toFile();
            AiCostEstimatorData repositoryData = read(dataFolder);
            if (repositoryData == null) {
                continue;
            }
            String reportsRelative = analysisResultsPath.replaceAll("/data/analysisResults\\.json$", "");
            names.add(repository.getAnalysisResults().getMetadata().getName());
            urls.add(prefix + reportsRelative + "/html/index.html#ai-cost");
            data.add(repositoryData);
        }
        return merge(names, urls, data, MAX_TASKS);
    }

    /** The repository's estimator data from data.zip, or the loose file; null when there is none. */
    static AiCostEstimatorData read(File dataFolder) {
        try {
            File zip = new File(dataFolder, "data.zip");
            String json = null;
            if (zip.isFile()) {
                try (ZipFile zipFile = new ZipFile(zip)) {
                    ZipEntry entry = zipFile.getEntry(AiCostEstimatorGenerator.DATA_FILE_NAME);
                    if (entry != null) {
                        try (InputStream in = zipFile.getInputStream(entry)) {
                            json = IOUtils.toString(in, StandardCharsets.UTF_8);
                        }
                    }
                }
            } else {
                File loose = new File(dataFolder, AiCostEstimatorGenerator.DATA_FILE_NAME);
                if (loose.isFile()) {
                    json = new String(java.nio.file.Files.readAllBytes(loose.toPath()), StandardCharsets.UTF_8);
                }
            }
            return json == null ? null : MAPPER.readValue(json, AiCostEstimatorData.class);
        } catch (IOException e) {
            LOG.warn("Skipping the AI cost estimator data in " + dataFolder.getPath() + ": " + e.getMessage());
            return null;
        }
    }

    /** One landscape estimator from the repositories' data (same order as names and urls), newest maxTasks tasks kept. */
    static AiCostEstimatorData merge(List<String> names, List<String> urls, List<AiCostEstimatorData> data, int maxTasks) {
        AiCostEstimatorData merged = new AiCostEstimatorData();
        Map<String, Integer> authorIndex = new HashMap<>();
        TreeSet<String> prefixes = new TreeSet<>();
        List<AiCostEstimatorData.Task> tasks = new ArrayList<>();
        for (int r = 0; r < data.size(); r++) {
            AiCostEstimatorData repository = data.get(r);
            merged.getRepositories().add(new AiCostEstimatorData.Repository(names.get(r), urls.get(r)));
            merged.getNoise().add(repository.getNoise());
            merged.setTotalCommitsCount(merged.getTotalCommitsCount() + repository.getTotalCommitsCount());
            merged.setKeptCommitsCount(merged.getKeptCommitsCount() + repository.getKeptCommitsCount());
            merged.setAnalyzedCommitsCount(merged.getAnalyzedCommitsCount() + repository.getAnalyzedCommitsCount());
            prefixes.addAll(repository.getTicketPrefixes());
            int[] remap = new int[repository.getAuthors().size()];
            for (int a = 0; a < remap.length; a++) {
                AiCostEstimatorData.Author author = repository.getAuthors().get(a);
                remap[a] = authorIndex.computeIfAbsent(author.getEmail().toLowerCase(), email -> {
                    merged.getAuthors().add(author);
                    return merged.getAuthors().size() - 1;
                });
            }
            for (AiCostEstimatorData.Task task : repository.getTasks()) {
                task.setRepo(r);
                task.getCommits().forEach(commit -> commit.setAuthor(commit.getAuthor() < remap.length ? remap[commit.getAuthor()] : 0));
                tasks.add(task);
            }
        }
        merged.setTicketPrefixes(new ArrayList<>(prefixes));
        merged.setTotalTasksCount(tasks.size());
        tasks.sort(Comparator.comparing(AiCostEstimatorData.Task::getStart).reversed());
        merged.setTasks(new ArrayList<>(tasks.subList(0, Math.min(maxTasks, tasks.size()))));
        return merged;
    }
}
