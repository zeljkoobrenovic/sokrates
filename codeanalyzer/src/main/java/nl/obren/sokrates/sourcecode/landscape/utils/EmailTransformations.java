package nl.obren.sokrates.sourcecode.landscape.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import nl.obren.sokrates.sourcecode.landscape.PersonConfig;
import nl.obren.sokrates.sourcecode.operations.ComplexOperation;
import nl.obren.sokrates.sourcecode.operations.OperationStatement;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmailTransformations {
    private static final Log LOG = LogFactory.getLog(EmailTransformations.class);
    @JsonIgnore
    private static final Map<String, String> cache = new HashMap<>();

    public static String transformEmail(final String email, List<OperationStatement> operationStatements, PeopleConfig peopleConfig) {
        return transformEmail(email, null, operationStatements, peopleConfig);
    }

    /**
     * Collapse a contributor to a single canonical id so all of a person's identities aggregate into
     * one contributor. When {@code userName} is provided, the person is matched by email patterns OR
     * userName patterns (see {@link PeopleConfig#getPerson(String, String)}); pass {@code null} where
     * only the email is available (callers keep the email-only behavior).
     */
    public static String transformEmail(final String email, final String userName,
                                        List<OperationStatement> operationStatements, PeopleConfig peopleConfig) {
        // The transformed id depends on the email, the userName, AND on the transform statements and
        // people config, so the cache key must include all of them. Keying on email alone returned one
        // landscape's mapping for another's identical email when several are processed in one JVM.
        // Object identity is enough: callers reuse the same statements/config instances throughout a landscape.
        String key = email + "::" + (userName == null ? "" : userName)
                + "::" + System.identityHashCode(operationStatements) + "::" + System.identityHashCode(peopleConfig);
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        String contributorId = email;
        if (operationStatements.size() > 0) {
            ComplexOperation operation = new ComplexOperation(operationStatements);
            contributorId = operation.exec(contributorId);
        }

        if (peopleConfig != null) {
            // Collapse all of a person's emails to a single canonical id so they aggregate into one
            // contributor. Prefer the explicit `email`; when it is blank fall back to the display
            // name (`userName`) — this matches the historical behavior (before `name` was split into
            // `email`+`userName`, the collapse key was the display name) and is what legacy
            // config-people.json entries that only set a display name rely on. Only when BOTH are
            // blank do we keep the original id: overwriting with "" would drop the contributor
            // entirely (getAllContributors() skips blank ids).
            PersonConfig person = peopleConfig.getPerson(contributorId, userName);
            if (StringUtils.isNotBlank(person.getEmail())) {
                contributorId = person.getEmail();
            } else if (StringUtils.isNotBlank(person.getUserName())) {
                contributorId = person.getUserName();
            }
        }

        cache.put(key, contributorId);

        return contributorId;
    }
}
