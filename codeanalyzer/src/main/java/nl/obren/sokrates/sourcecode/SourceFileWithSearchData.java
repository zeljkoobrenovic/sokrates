package nl.obren.sokrates.sourcecode;

import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.search.FoundLine;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

public class SourceFileWithSearchData {
    private static final Log LOG = LogFactory.getLog(SourceFileWithSearchData.class);

    private SourceFile sourceFile;
    private List<FoundLine> linesWithSearchedContent = new ArrayList<>();
    private int foundInstancesCount = 0;
    private int linesOfCodeCount = -1;

    public SourceFileWithSearchData(SourceFile sourceFile) {
        this.sourceFile = sourceFile;
        this.linesOfCodeCount = sourceFile.getLinesOfCode();
    }

    public int getFoundInstancesCount() {
        return foundInstancesCount;
    }

    public void setFoundInstancesCount(int foundInstancesCount) {
        this.foundInstancesCount = foundInstancesCount;
    }

    public SourceFile getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(SourceFile sourceFile) {
        this.sourceFile = sourceFile;
    }

    public List<String> getLines() {
        String content = sourceFile.getContent();
        return StringUtils.isNotBlank(content)
                ? SourceCodeCleanerUtils.splitInLines(content)
                : new ArrayList<>();
    }

    public String getPath() {
        return sourceFile.getFile().getPath();
    }

    public List<FoundLine> getLinesWithSearchedContent() {
        return linesWithSearchedContent;
    }

    public int getLineCount() {
        return linesOfCodeCount == -1 ? getLines().size() : linesOfCodeCount;
    }

    public void clearSearchData() {
        foundInstancesCount = 0;
        linesWithSearchedContent.clear();
    }
}
