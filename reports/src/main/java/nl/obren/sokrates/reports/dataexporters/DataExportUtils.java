/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.dataexporters;

import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;

public class DataExportUtils {
    public static String getAspectFileListFileName(NamedSourceCodeAspect aspect, String prefix) {
        return "aspect_" + aspect.getFileSystemFriendlyName(prefix) + ".txt";
    }

    public static String getComponentFilePrefix(String logicalDecompositionName) {
        return "component_" + logicalDecompositionName + "_";
    }

    public static String getCrossCuttingConcernFilePrefix(String concernGroup) {
        return "concern_" + concernGroup + "_";
    }
}
