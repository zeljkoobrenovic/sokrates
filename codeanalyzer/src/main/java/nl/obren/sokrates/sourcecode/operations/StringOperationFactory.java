package nl.obren.sokrates.sourcecode.operations;

import nl.obren.sokrates.sourcecode.operations.impl.*;

import java.util.HashMap;
import java.util.Map;

public class StringOperationFactory {
    private Map<String, Class> classesMap = new HashMap<>();

    public StringOperationFactory() {
        classesMap.put("extract", ExtractRegexOperation.class);
        classesMap.put("remove", RemoveRegexOperation.class);
        classesMap.put("replace", ReplaceOperation.class);
        classesMap.put("trim", TrimOperation.class);
        classesMap.put("uppercase", UpperCaseOperation.class);
        classesMap.put("lowercase", LowerCaseOperation.class);
        classesMap.put("append", AppendOperation.class);
        classesMap.put("prepend", PrependOperation.class);
    }

    public StringOperation getOperation(OperationStatement operationStatement) {
        StringOperation operation = null;
        Class opClass = classesMap.get(operationStatement.getOp().toLowerCase());
        if (opClass != null) {
            try {
                operation = (StringOperation) opClass.newInstance();
                operation.setParams(operationStatement.getParams());
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return operation != null ? operation : new NoOpsOperation(operationStatement.getParams());
    }
}
