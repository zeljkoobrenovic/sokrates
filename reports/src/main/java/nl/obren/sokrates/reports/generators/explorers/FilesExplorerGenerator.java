package nl.obren.sokrates.reports.generators.explorers;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.reports.dataexporters.files.FileListExporter;
import nl.obren.sokrates.reports.dataexporters.files.FilesExportInfo;
import nl.obren.sokrates.reports.utils.HtmlTemplateUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesAnalysisResults;

public class FilesExplorerGenerator {
    public static final String FILES_DATA = "\"${__FILES_DATA__}\"";
    private CodeAnalysisResults codeAnalysisResults;

    public FilesExplorerGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public String generateExplorer() {
        return generateExplorer(getMainFilesExportInfo());
    }

    private String generateExplorer(FilesExportInfo exportInfo) {
        String html = HtmlTemplateUtils.getResource("/templates/Files.html");
        try {
            html = html.replace(FILES_DATA, new JsonGenerator().generate(exportInfo));
        } catch (JsonProcessingException e) {
            html = html.replace(FILES_DATA, "{}");
            e.printStackTrace();
        }

        return html;
    }

    private FilesExportInfo getMainFilesExportInfo() {
        FilesAnalysisResults filesAnalysisResults = codeAnalysisResults.getFilesAnalysisResults();
        FilesExportInfo exportInfo = new FilesExportInfo("main",
                "Main Files Explorer",
                codeAnalysisResults.getMainAspectAnalysisResults().getFilesCount(),
                codeAnalysisResults.getMainAspectAnalysisResults().getLinesOfCode());
        FileListExporter fileListExporter = new FileListExporter(filesAnalysisResults.getAllFiles());
        exportInfo.setFiles(fileListExporter.getAllFilesData());
        return exportInfo;
    }
}
