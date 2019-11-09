package nl.obren.sokrates.reports.dataexporters.dependencies;

import nl.obren.sokrates.reports.dataexporters.files.FileExportInfo;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;

import java.util.ArrayList;
import java.util.List;

public class DependencyAnchorExportInfo {
    private String anchor;
    private String codeFragment;
    private List<String> dependencyPatterns = new ArrayList<>();
    private List<FileExportInfo> files = new ArrayList<>();

    public static DependencyAnchorExportInfo getInstance(DependencyAnchor dependencyAnchor) {
        DependencyAnchorExportInfo exportInfo = new DependencyAnchorExportInfo();

        exportInfo.setAnchor(dependencyAnchor.getAnchor());
        exportInfo.setCodeFragment(dependencyAnchor.getCodeFragment());
        exportInfo.setDependencyPatterns(dependencyAnchor.getDependencyPatterns());
        dependencyAnchor.getSourceFiles().forEach(sourceFile -> {
            exportInfo.getFiles().add(FileExportInfo.getInstance(sourceFile));
        });

        return exportInfo;
    }

    public String getAnchor() {
        return anchor;
    }

    public void setAnchor(String anchor) {
        this.anchor = anchor;
    }

    public String getCodeFragment() {
        return codeFragment;
    }

    public void setCodeFragment(String codeFragment) {
        this.codeFragment = codeFragment;
    }

    public List<String> getDependencyPatterns() {
        return dependencyPatterns;
    }

    public void setDependencyPatterns(List<String> dependencyPatterns) {
        this.dependencyPatterns = dependencyPatterns;
    }

    public List<FileExportInfo> getFiles() {
        return files;
    }

    public void setFiles(List<FileExportInfo> files) {
        this.files = files;
    }
}
