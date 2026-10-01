package nl.obren.sokrates.cli.git;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * The {@code source.json} marker the clone-and-analyze step leaves next to a kept analysis'
 * {@code config.json}: which repository URL it came from, which command produced it and when.
 * It is what {@code -prune} relies on — only analyses carrying this marker are ever deleted, so
 * analyses placed by hand (a moved {@code _sokrates} folder) are never touched.
 */
public class AnalysisSource {
    public static final String FILE_NAME = "source.json";

    private String url = "";
    private String command = "";
    private String analyzedOn = "";

    public AnalysisSource() {
    }

    public AnalysisSource(String url, String command, String analyzedOn) {
        this.url = url;
        this.command = command;
        this.analyzedOn = analyzedOn;
    }

    /** The marker of the analysis kept in {@code folder}, or null when there is none (or it is unreadable). */
    public static AnalysisSource read(File folder) {
        File file = new File(folder, FILE_NAME);
        if (!file.exists()) {
            return null;
        }
        try {
            AnalysisSource source = (AnalysisSource) new JsonMapper().getObject(FileUtils.readFileToString(file, StandardCharsets.UTF_8), AnalysisSource.class);
            return source != null && StringUtils.isNotBlank(source.getUrl()) ? source : null;
        } catch (Exception e) {
            return null;
        }
    }

    public void save(File folder) throws IOException {
        FileUtils.write(new File(folder, FILE_NAME), new JsonGenerator().generate(this), StandardCharsets.UTF_8);
    }

    /**
     * The form two URLs of the same repository are compared in: trimmed, without a trailing slash
     * or {@code .git}, lower-cased (hosts and the GitHub/GitLab namespaces are case-insensitive).
     */
    public static String normalizeUrl(String url) {
        String normalized = StringUtils.stripEnd(StringUtils.defaultString(url).trim(), "/");
        if (normalized.toLowerCase().endsWith(".git")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized.toLowerCase();
    }

    @JsonIgnore
    public String getNormalizedUrl() {
        return normalizeUrl(url);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getAnalyzedOn() {
        return analyzedOn;
    }

    public void setAnalyzedOn(String analyzedOn) {
        this.analyzedOn = analyzedOn;
    }
}
