package nl.obren.sokrates.reports.dataexporters.dependencies;

import nl.obren.sokrates.reports.dataexporters.files.FileExportInfo;
import nl.obren.sokrates.sourcecode.dependencies.SourceFileDependency;

public class SourceFileDependencyExportInfo {
    private FileExportInfo file;
    private String codeFragment = "";

    public static SourceFileDependencyExportInfo getInstance(SourceFileDependency fromFile) {
        SourceFileDependencyExportInfo sourceFileDependencyExportInfo = new SourceFileDependencyExportInfo();
        sourceFileDependencyExportInfo.setFile(FileExportInfo.getInstance(fromFile.getSourceFile()));
        sourceFileDependencyExportInfo.setCodeFragment(fromFile.getCodeFragment());

        return sourceFileDependencyExportInfo;
    }

    public FileExportInfo getFile() {
        return file;
    }

    public void setFile(FileExportInfo file) {
        this.file = file;
    }

    public String getCodeFragment() {
        return codeFragment;
    }

    public void setCodeFragment(String codeFragment) {
        this.codeFragment = codeFragment;
    }
}
