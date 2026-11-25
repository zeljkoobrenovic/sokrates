package nl.obren.sokrates.server;

import com.fasterxml.jackson.core.type.TypeReference;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.sourcecode.githistory.GitHistoryUtils;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import nl.obren.sokrates.sourcecode.landscape.utils.EmailTransformations;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * One person across repositories, as a landscape identifies them: its {@code ignoreContributors},
 * {@code transformContributorEmails} and {@code config-people.json} applied the way LandscapeContributorsAggregator
 * applies them, so the server merges people exactly as the landscape report does. Without a landscape, the
 * repositories' own identities are used as they are.
 */
class LandscapeIdentities {
    private static final Log LOG = LogFactory.getLog(LandscapeIdentities.class);
    static final LandscapeIdentities AS_ANALYZED = new LandscapeIdentities(null, null, 0);

    private final LandscapeConfiguration configuration;
    private final PeopleConfig peopleConfig;
    private final long stamp;

    private LandscapeIdentities(LandscapeConfiguration configuration, PeopleConfig peopleConfig, long stamp) {
        this.configuration = configuration;
        this.peopleConfig = peopleConfig;
        this.stamp = stamp;
    }

    /** Changes when the landscape's config.json or config-people.json does. */
    static long stamp(File landscapeFolder) {
        return new File(landscapeFolder, "config.json").lastModified() * 31 + new File(landscapeFolder, "config-people.json").lastModified();
    }

    long getStamp() {
        return stamp;
    }

    static LandscapeIdentities load(File landscapeFolder) {
        long stamp = stamp(landscapeFolder);
        LandscapeConfiguration configuration = null;
        PeopleConfig people = null;
        try {
            File config = new File(landscapeFolder, "config.json");
            if (config.exists()) {
                configuration = (LandscapeConfiguration) new JsonMapper().getObject(FileUtils.readFileToString(config, StandardCharsets.UTF_8), LandscapeConfiguration.class);
            }
            File peopleFile = new File(landscapeFolder, "config-people.json");
            if (peopleFile.exists()) {
                // the static getObject returns null on a config it cannot read: then people are not merged (logged)
                people = JsonMapper.getObject(FileUtils.readFileToString(peopleFile, StandardCharsets.UTF_8), new TypeReference<PeopleConfig>() {
                });
                if (people == null) {
                    LOG.warn("Could not read " + peopleFile.getPath() + "; people are not merged across identities");
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not read the landscape configuration in " + landscapeFolder.getPath() + ": " + e.getMessage());
        }
        return new LandscapeIdentities(configuration, people == null ? new PeopleConfig() : people, stamp);
    }

    /** The canonical id of a contributor, or null when the landscape ignores them. */
    String canonical(String email, String userName) {
        if (StringUtils.isBlank(email)) {
            return null;
        }
        if (configuration == null) {
            return email;
        }
        String id = email.toLowerCase(Locale.ROOT);
        if (GitHistoryUtils.shouldIgnore(id, configuration.getIgnoreContributors())) {
            return null;
        }
        id = EmailTransformations.transformEmail(id, userName, configuration.getTransformContributorEmails(), peopleConfig);
        if (StringUtils.isBlank(id) || GitHistoryUtils.shouldIgnore(id, configuration.getIgnoreContributors())) {
            return null;
        }
        return id;
    }
}
