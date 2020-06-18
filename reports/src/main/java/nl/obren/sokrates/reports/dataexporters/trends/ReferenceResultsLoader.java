/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.dataexporters.trends;

import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.reports.utils.ZipEntryContent;
import nl.obren.sokrates.reports.utils.ZipUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ReferenceResultsLoader {
    public CodeAnalysisResults getRefData(File file) {
        CodeAnalysisResults refData = null;
        try {
            if (file.exists()) {
                refData = getAnalysisResultsFromJson(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return refData;
    }

    private CodeAnalysisResults getAnalysisResultsFromJson(File file) throws IOException {
        if (file.isDirectory()) {
            file = new File(file, "analysisResults.zip");
        }

        if (FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("zip")) {
            Map<String, ZipEntryContent> entries = ZipUtils.unzipAllEntriesAsStrings(file);

            if (entries.get("analysisResults.json") != null) {
                String resultsJson = entries.get("analysisResults.json").getContent();
                CodeAnalysisResults results = (CodeAnalysisResults) new JsonMapper().getObject(resultsJson, CodeAnalysisResults.class);
                if (entries.get("config.json") != null) {
                    String configJson = entries.get("config.json").getContent();
                    CodeConfiguration codeConfiguration = (CodeConfiguration) new JsonMapper().getObject(configJson, CodeConfiguration.class);
                    results.setCodeConfiguration(codeConfiguration);
                }

                return results;
            }
        }

        return null;
    }
}
