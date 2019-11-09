package nl.obren.sokrates.reports.core;

import java.util.List;

public interface ReportGenerator<T extends Scope> {
    List<RichTextReport> report(T scope, AnalysisEngine analysisEngine);
}

