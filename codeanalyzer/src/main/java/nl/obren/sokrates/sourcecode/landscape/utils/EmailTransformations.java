package nl.obren.sokrates.sourcecode.landscape.utils;

import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import nl.obren.sokrates.sourcecode.landscape.init.LandscapeAnalysisInitiator;
import nl.obren.sokrates.sourcecode.operations.ComplexOperation;
import nl.obren.sokrates.sourcecode.operations.OperationStatement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

public class EmailTransformations {
    private static final Log LOG = LogFactory.getLog(EmailTransformations.class);
    public static String transformEmail(final String email, List<OperationStatement> operationStatements, PeopleConfig peopleConfig) {
        String contributorId = email;
        if (operationStatements.size() > 0) {
            ComplexOperation operation = new ComplexOperation(operationStatements);
            contributorId = operation.exec(contributorId);
            if (!contributorId.equals(email)) {
                LOG.info(email + " -> " + contributorId);
            }
        }

        return peopleConfig.getPerson(contributorId);
    }
}
