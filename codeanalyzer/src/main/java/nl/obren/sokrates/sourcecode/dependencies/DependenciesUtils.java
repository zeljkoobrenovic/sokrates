package nl.obren.sokrates.sourcecode.dependencies;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.SourceCodeAspect;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzer;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;

import java.util.ArrayList;
import java.util.List;

public class DependenciesUtils {
    public static DependenciesAnalysis extractDependencies(SourceCodeAspect aspect, boolean skipDependencies) {
        List<Dependency> allDependencies = new ArrayList<>();
        DependenciesAnalysis dependenciesAnalysis = new DependenciesAnalysis();
        dependenciesAnalysis.setDependencies(allDependencies);
        if (!skipDependencies) {
            aspect.getAspectsPerExtensions().forEach(langAspect -> {
                if (langAspect.getSourceFiles().size() > 0) {
                    SourceFile sourceFileSample = langAspect.getSourceFiles().get(0);
                    LanguageAnalyzer languageAnalyzer = LanguageAnalyzerFactory.getInstance().getLanguageAnalyzer(sourceFileSample);
                    DependenciesAnalysis localAnalysis = languageAnalyzer.extractDependencies(langAspect.getSourceFiles(), new ProgressFeedback());
                    allDependencies.addAll(localAnalysis.getDependencies());
                    dependenciesAnalysis.getErrors().addAll(localAnalysis.getErrors());
                }
            });
        }

        return dependenciesAnalysis;
    }
}
