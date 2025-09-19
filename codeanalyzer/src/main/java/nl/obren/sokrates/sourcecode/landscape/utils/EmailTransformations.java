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
        if (cache.containsKey(email)) {
            return cache.get(email);
        }

        String contributorId = email;
        if (operationStatements.size() > 0) {
            ComplexOperation operation = new ComplexOperation(operationStatements);
            contributorId = operation.exec(contributorId);
        }

        if (peopleConfig != null) {
            contributorId = peopleConfig.getPersonFromEmailPatterns(contributorId).getName();
        }

        cache.put(email, contributorId);

        return contributorId;
    }
}
