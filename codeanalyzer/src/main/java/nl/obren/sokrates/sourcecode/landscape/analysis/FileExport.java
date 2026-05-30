package nl.obren.sokrates.sourcecode.landscape.analysis;

import nl.obren.sokrates.sourcecode.ExtensionGroupExtractor;

public class FileExport {
    private String repository;
    private String path;

    private String scope;

    private int linesOfCode;

    public FileExport() {
    }

    public FileExport(String repository, String path, String scope, int linesOfCode) {
        this.repository = repository;
        this.path = path;
        this.scope = scope;
        this.linesOfCode = linesOfCode;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public int getLinesOfCode() {
        return linesOfCode;
    }

    public void setLinesOfCode(int linesOfCode) {
        this.linesOfCode = linesOfCode;
    }

    /**
     * The file's language, derived from its path extension. Serialized into the explorer JSON
     * so the files explorer can render a matching language icon.
     */
    public String getMainLang() {
        return path != null ? ExtensionGroupExtractor.getExtension(path).toLowerCase().trim() : "";
    }
}
