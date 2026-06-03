package nl.obren.sokrates.sourcecode.landscape.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import nl.obren.sokrates.sourcecode.operations.ComplexOperation;
import nl.obren.sokrates.sourcecode.operations.OperationStatement;
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
        // The transformed id depends on the email AND on the transform statements and people config,
        // so the cache key must include all three. Keying on email alone returned one landscape's
        // mapping for another's identical email when several are processed in one JVM. Object identity
        // is enough: callers reuse the same statements/config instances throughout a landscape.
        String key = email + "::" + System.identityHashCode(operationStatements) + "::" + System.identityHashCode(peopleConfig);
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        String contributorId = email;
        if (operationStatements.size() > 0) {
            ComplexOperation operation = new ComplexOperation(operationStatements);
            contributorId = operation.exec(contributorId);
        }

        if (peopleConfig != null) {
            contributorId = peopleConfig.getPersonFromEmailPatterns(contributorId).getName();
        }

        cache.put(key, contributorId);

        return contributorId;
    }
}
