package nl.obren.sokrates.reports.landscape.statichtml;

import com.fasterxml.jackson.core.type.TypeReference;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.sourcecode.analysis.FileHistoryAnalysisConfig;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.githistory.FileUpdate;
import nl.obren.sokrates.sourcecode.githistory.GitHistoryUtils;
import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Single-repository counterpart of {@link LandscapeAnalysisCommands#updatePeopleConfigByUserName}.
 *
 * <p>Updates (or creates) one repository's {@code _sokrates/config-people.json} by grouping all
 * contributor emails that share a display name (userName) under one entry. It reads ONLY the
 * repository's {@code git-history.txt} (the file produced by {@code extractGitHistory}), so it can run
 * without {@code generateReports} — but after {@code extractGitHistory}. The contributor-detection
 * conventions (ignored contributors, bots, email transforms, anonymization) are taken from the same
 * {@code config.json} the analysis uses ({@link FileHistoryAnalysisConfig}), and the grouping/merge is
 * the shared, additive {@link PeopleConfigByUserNameUpdater} logic.
 */
public class RepositoryPeopleConfigCommands {
    private static final Log LOG = LogFactory.getLog(RepositoryPeopleConfigCommands.class);

    public static final String PEOPLE_CONFIG_FILE_NAME = "config-people.json";

    /**
     * @param sokratesConfigFile the repository's {@code _sokrates/config.json}.
     * @return the {@code config-people.json} file that was written (next to the config file), or null
     * if the git history could not be read.
     */
    public static File updatePeopleConfigByUserName(File sokratesConfigFile) {
        if (sokratesConfigFile == null || !sokratesConfigFile.exists()) {
            LOG.error("Configuration file \"" + sokratesConfigFile + "\" does not exist.");
            return null;
        }

        CodeConfiguration codeConfiguration = readCodeConfiguration(sokratesConfigFile);
        if (codeConfiguration == null) {
            return null;
        }

        File sokratesConfigFolder = sokratesConfigFile.getParentFile();
        FileHistoryAnalysisConfig historyConfig = codeConfiguration.getFileHistoryAnalysis();
        File historyFile = historyConfig.getFilesHistoryFile(sokratesConfigFolder);
        if (!historyFile.exists()) {
            LOG.error("Git history file \"" + historyFile.getPath() + "\" does not exist. "
                    + "Run extractGitHistory first.");
            return null;
        }

        // Collect raw (email, userName) pairs straight from git-history.txt (no analysis run). The
        // line parser applies the config's ignore/bots/transform/anonymize rules to the email. We parse
        // line-by-line via parseLine rather than GitHistoryUtils.getHistoryFromFile, because the latter
        // memoizes its result in a JVM-wide static cache keyed on nothing — it would return another
        // repository's history if one had been read earlier in the same process.
        List<PeopleConfigByUserNameUpdater.ContributorIdentity> identities = new ArrayList<>();
        List<String> lines;
        try {
            lines = FileUtils.readLines(historyFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Could not read " + historyFile.getPath(), e);
            return null;
        }
        lines.forEach(line -> {
            FileUpdate fileUpdate = GitHistoryUtils.parseLine(line, historyConfig);
            if (fileUpdate != null) {
                identities.add(new PeopleConfigByUserNameUpdater.ContributorIdentity(
                        fileUpdate.getAuthorEmail(), fileUpdate.getUserName(), fileUpdate.getDate()));
            }
        });

        File peopleConfigFile = new File(sokratesConfigFolder, PEOPLE_CONFIG_FILE_NAME);
        PeopleConfig peopleConfig = readPeopleConfig(peopleConfigFile);

        int addedCount = new PeopleConfigByUserNameUpdater().update(peopleConfig, identities);

        try {
            FileUtils.write(peopleConfigFile, new JsonGenerator().generate(peopleConfig), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Could not write " + peopleConfigFile.getPath(), e);
            return null;
        }

        LOG.info("Updated " + peopleConfigFile.getPath() + ": "
                + peopleConfig.getPeople().size() + " people, " + addedCount + " email pattern(s) added.");

        return peopleConfigFile;
    }

    private static CodeConfiguration readCodeConfiguration(File sokratesConfigFile) {
        try {
            String json = FileUtils.readFileToString(sokratesConfigFile, StandardCharsets.UTF_8);
            return (CodeConfiguration) new JsonMapper().getObject(json, CodeConfiguration.class);
        } catch (IOException e) {
            LOG.error("Could not read " + sokratesConfigFile.getPath(), e);
            return null;
        }
    }

    private static PeopleConfig readPeopleConfig(File peopleConfigFile) {
        if (!peopleConfigFile.exists()) {
            return new PeopleConfig();
        }
        try {
            PeopleConfig peopleConfig = new JsonMapper().getObject(
                    FileUtils.readFileToString(peopleConfigFile, StandardCharsets.UTF_8),
                    new TypeReference<PeopleConfig>() {
                    });
            return peopleConfig != null ? peopleConfig : new PeopleConfig();
        } catch (IOException e) {
            LOG.error("Could not read " + peopleConfigFile.getPath(), e);
            return new PeopleConfig();
        }
    }
}
