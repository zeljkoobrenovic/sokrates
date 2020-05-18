/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import nl.obren.sokrates.common.io.JsonGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageSupportExporter {
    LanguageAnalyzerFactory languageFactory = LanguageAnalyzerFactory.getInstance();
    Map<String, LanguageInfoExport> analyzerMap = new HashMap<>();

    public static void main(String args[]) throws JsonProcessingException {
        new LanguageSupportExporter().export();
    }

    private void export() throws JsonProcessingException {
        List<LanguageInfoExport> export = new ArrayList<>();

        languageFactory.getAnalyzersMap().keySet().forEach(extension -> {
            LanguageAnalyzer languageAnalyzerByExtension = languageFactory.getLanguageAnalyzerByExtension(extension);
            processAnalyzer(extension, languageAnalyzerByExtension);
        });

        analyzerMap.keySet().forEach(key -> {
            export.add(analyzerMap.get(key));
        });
    }

    private void processAnalyzer(String extension, LanguageAnalyzer languageAnalyzerByExtension) {
        String simpleName = languageAnalyzerByExtension.getClass().getSimpleName();
        LanguageInfoExport languageInfoExport = analyzerMap.get(simpleName);
        if (languageInfoExport == null) {
            languageInfoExport = new LanguageInfoExport();
            languageInfoExport.setLanguageAnalyzer(languageAnalyzerByExtension);
            analyzerMap.put(simpleName, languageInfoExport);
        }
        languageInfoExport.getExtensions().add(extension);
    }

    class LanguageInfoExport {
        private List<String> extensions = new ArrayList<>();
        private String name;
        private List<String> featuresDescription;

        @JsonIgnore
        private LanguageAnalyzer languageAnalyzer;

        public List<String> getExtensions() {
            return extensions;
        }

        public void setExtensions(List<String> extensions) {
            this.extensions = extensions;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @JsonIgnore
        public LanguageAnalyzer getLanguageAnalyzer() {
            return languageAnalyzer;
        }

        @JsonIgnore
        public void setLanguageAnalyzer(LanguageAnalyzer languageAnalyzer) {
            this.languageAnalyzer = languageAnalyzer;
            this.name = languageAnalyzer.getClass().getSimpleName();
            this.featuresDescription = languageAnalyzer.getFeaturesDescription();
        }


        public List<String> getFeaturesDescription() {
            return featuresDescription;
        }

        public void setFeaturesDescription(List<String> featuresDescription) {
            this.featuresDescription = featuresDescription;
        }
    }
}
