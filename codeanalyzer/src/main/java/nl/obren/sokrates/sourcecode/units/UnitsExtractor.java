/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.units;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzer;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;

import java.util.ArrayList;
import java.util.List;

public class UnitsExtractor {
    public List<UnitInfo> getUnits(List<SourceFile> sourceFiles, ProgressFeedback progressFeedback) {
        List<UnitInfo> units = new ArrayList<>();

        int index[] = {0};

        sourceFiles.forEach(sourceFile -> {
            if (progressFeedback.canceled()) {
                return;
            }
            progressFeedback.progress(++index[0], sourceFiles.size());
            if (index[0] % 1000 == 1 || index[0] == sourceFiles.size()) {
                progressFeedback.setDetailedText("Analyzing units of file " + index[0] + "/" + sourceFiles.size() +
                        ": " + sourceFile.getRelativePath());
            }
            LanguageAnalyzer languageAnalyzer = LanguageAnalyzerFactory.getInstance().getLanguageAnalyzer(sourceFile);
            units.addAll(languageAnalyzer.extractUnits(sourceFile));
        });

        return units;
    }
}
