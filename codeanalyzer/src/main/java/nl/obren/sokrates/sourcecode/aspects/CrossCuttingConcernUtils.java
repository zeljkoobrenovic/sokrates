package nl.obren.sokrates.sourcecode.aspects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

public class CrossCuttingConcernUtils {
    private static final Log LOG = LogFactory.getLog(CrossCuttingConcernUtils.class);


    public static CrossCuttingConcernsGroup getGroupByName(List<CrossCuttingConcernsGroup> groups, String name) {
        for (CrossCuttingConcernsGroup group : groups) {
            if (group.getName().equalsIgnoreCase(name)) {
                return group;
            }
        }

        return null;
    }

}
