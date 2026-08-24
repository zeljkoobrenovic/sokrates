package nl.obren.sokrates.sourcecode.lang.plsql;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.dependencies.HeuristicDependenciesExtractor;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlSqlHeuristicDependenciesExtractor extends HeuristicDependenciesExtractor {
    public static final String PACKAGE_PREFIX = "PACKAGE ";
    public static final String BODY_KEYWORD = "BODY";

    @Override
    public List<DependencyAnchor> extractDependencyAnchors(SourceFile sourceFile) {
        List<DependencyAnchor> anchors = new ArrayList<>();

        anchors.addAll(getAnchorsFromPackage(sourceFile));
        anchors.addAll(getAnchorsFromUsage(sourceFile));

        return anchors;
    }

    private List<DependencyAnchor> getAnchorsFromPackage(SourceFile sourceFile) {
        List<DependencyAnchor> anchors = new ArrayList<>();

        String content = sourceFile.getContent();
        String fileNameNoExtension = FilenameUtils.removeExtension(sourceFile.getFile().getName());
        content = content.replace("\t", " ");
        content = SourceCodeCleanerUtils.normalizeLineEnds(content);

        int startIndexOfPackageName;
        if(content.contains(PACKAGE_PREFIX)) {
            startIndexOfPackageName = content.indexOf(PACKAGE_PREFIX);
        } else {
            startIndexOfPackageName = content.indexOf(PACKAGE_PREFIX.toLowerCase());
        }
        if (startIndexOfPackageName >= 0) {
            int startOfName = skipWhitespace(content, startIndexOfPackageName + PACKAGE_PREFIX.length());
            if (isBodyKeywordAt(content, startOfName)) {
                startOfName = skipWhitespace(content, startOfName + BODY_KEYWORD.length());
            }
            int endIndexOfPackageName = indexOfWhitespace(content, startOfName);
            if (endIndexOfPackageName >= 0) {
                String packageName = content.substring(startOfName, endIndexOfPackageName).trim();
                String codeFragment = content.substring(startIndexOfPackageName,
                        endIndexOfPackageName + 1).trim();
                anchors.add((createAnchor(packageName, codeFragment, sourceFile)));
            }
        }

        return anchors;
    }

    private boolean isBodyKeywordAt(String content, int index) {
        int afterKeyword = index + BODY_KEYWORD.length();
        return content.regionMatches(true, index, BODY_KEYWORD, 0, BODY_KEYWORD.length())
                && afterKeyword < content.length()
                && Character.isWhitespace(content.charAt(afterKeyword));
    }

    private int skipWhitespace(String content, int fromIndex) {
        int index = fromIndex;
        while (index < content.length() && Character.isWhitespace(content.charAt(index))) {
            index++;
        }
        return index;
    }

    private int indexOfWhitespace(String content, int fromIndex) {
        for (int i = fromIndex; i < content.length(); i++) {
            if (Character.isWhitespace(content.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private DependencyAnchor createAnchor(String packageName, String code, SourceFile sourceFile) {
        DependencyAnchor anchor = new DependencyAnchor(packageName);
        anchor.setCodeFragment(code);
        anchor.getDependencyPatterns().add("\\s*" + packageName + "[.]+\\S+");
        anchor.getSourceFiles().add(sourceFile);
        return anchor;
    }

    private List<DependencyAnchor> getAnchorsFromUsage(SourceFile sourceFile) {
        List<DependencyAnchor> anchors = new ArrayList<>();

        String fileNameNoExtension = FilenameUtils.removeExtension(sourceFile.getFile().getName());
        CleanedContent cleanedContent = getCleanContent(sourceFile);
        List<String> lines = SourceCodeCleanerUtils.splitInLines(cleanedContent.getCleanedContent());
        Pattern pattern = Pattern.compile("[\\p{L}\\p{N}_][\\p{L}\\p{M}\\p{N}_]*[.]\\p{L}[\\p{L}\\p{M}]*");
        Matcher matcher = null;
        List<String> packages = new ArrayList<>();
        for (String line : lines) {
            matcher = pattern.matcher(line);
            while (matcher.find()) {
                String newPackage = matcher.group();
                String packageName = newPackage.substring(0,newPackage.indexOf("."));
                if(!packages.contains(packageName)) {
                    packages.add(packageName);
                    anchors.add(createAnchor(fileNameNoExtension, line.trim(), sourceFile));
                }
            }
        }

        return anchors;
    }

    private CleanedContent getCleanContent(SourceFile sourceFile) {
        CleanedContent normallyCleanedContent = LanguageAnalyzerFactory.getInstance().getLanguageAnalyzer(sourceFile)
                .cleanForLinesOfCodeCalculations(sourceFile);
        normallyCleanedContent.setCleanedContent(extraCleanContent(normallyCleanedContent.getCleanedContent()));

        return normallyCleanedContent;
    }

    protected String extraCleanContent(String content) {
        String cleanedContent = emptyStrings(content);

        cleanedContent = SourceCodeCleanerUtils.normalizeLineEnds(cleanedContent);

        return cleanedContent;
    }

    private String emptyStrings(String cleanedContent) {
        cleanedContent = cleanedContent.replaceAll("'.*?'", "''");
        return cleanedContent;
    }

    protected boolean isUsageSignature(String line) {
        return line.trim().startsWith("CREATE ")
                || line.trim().startsWith("create ")
                || line.trim().startsWith("DECLARE ")
                || line.trim().startsWith("declare ")
                || line.trim().startsWith("BEGIN ")
                || line.trim().startsWith("begin ");
    }
}
