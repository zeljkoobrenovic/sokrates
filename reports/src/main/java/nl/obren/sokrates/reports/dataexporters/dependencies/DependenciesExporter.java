package nl.obren.sokrates.reports.dataexporters.dependencies;

import nl.obren.sokrates.reports.dataexporters.duplication.DuplicateFileBlockExportInfo;
import nl.obren.sokrates.reports.dataexporters.duplication.DuplicationExportInfo;
import nl.obren.sokrates.reports.dataexporters.files.FileExportInfo;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;

import java.util.ArrayList;
import java.util.List;

public class DependenciesExporter {
    private List<Dependency> dependencies = new ArrayList<>();

    public DependenciesExporter(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    public List<DependencyExportInfo> getDependenciesExportInfo() {
        List<DependencyExportInfo> export = new ArrayList<>();

        dependencies.forEach(dependency -> {
            DependencyExportInfo exportInfo = new DependencyExportInfo();

            exportInfo.setFrom(DependencyAnchorExportInfo.getInstance(dependency.getFrom()));
            exportInfo.setTo(DependencyAnchorExportInfo.getInstance(dependency.getTo()));

            dependency.getFromFiles().forEach(fromFile -> {
                exportInfo.getFromFiles().add(SourceFileDependencyExportInfo.getInstance(fromFile));
            });

            export.add(exportInfo);
        });

        return export;
    }
}
