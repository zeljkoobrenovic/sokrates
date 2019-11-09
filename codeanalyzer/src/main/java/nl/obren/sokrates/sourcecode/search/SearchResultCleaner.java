package nl.obren.sokrates.sourcecode.search;

import nl.obren.sokrates.common.utils.RegexUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SearchResultCleaner {
    private boolean trim = true;
    private String startCleaningPattern = "";
    private String endCleaningPattern = "";
    private List<ReplacePair> replacePairs = new ArrayList<>();

    public SearchResultCleaner() {
        for (int i = 0; i < 100; i++) {
            replacePairs.add(new ReplacePair("", ""));
        }
    }

    public List<CleanedFoundText> getCleanedTextList(List<FoundText> foundTextList) {
        List<CleanedFoundText> cleanedTextList = new ArrayList<>();

        foundTextList.forEach(foundText -> cleanedTextList.add(new CleanedFoundText(foundText, clean(foundText.getText()))));

        return cleanedTextList;
    }

    public String clean(String text) {
        String cleanedText = text;
        cleanedText = removeStart(cleanedText);
        cleanedText = removeEnd(cleanedText);
        cleanedText = replace(cleanedText);
        cleanedText = trim(cleanedText);
        return cleanedText;
    }

    private String trim(String target) {
        if (trim) {
            target = target.trim();
        }
        return target;
    }

    private String removeStart(String target) {
        if (StringUtils.isNotBlank(startCleaningPattern)) {
            String start = RegexUtils.getMatchedRegex(target, startCleaningPattern);
            if (start != null && target.startsWith(start)) {
                target = target.substring(start.length());
            }
        }
        return target;
    }

    private String removeEnd(String target) {
        if (StringUtils.isNotBlank(endCleaningPattern)) {
            String end = RegexUtils.getLastMatchedRegex(target, endCleaningPattern);
            if (end != null && target.endsWith(end)) {
                target = target.substring(0, target.length() - end.length());
            }
        }
        return target;
    }

    private String replace(String target) {
        String result[] = {target};
        replacePairs.forEach(replacePair -> {
            result[0] = replacePair.replaceIn(result[0]);
        });
        return result[0];
    }

    public boolean isTrim() {
        return trim;
    }

    public void setTrim(boolean trim) {
        this.trim = trim;
    }

    public String getStartCleaningPattern() {
        return startCleaningPattern;
    }

    public void setStartCleaningPattern(String startCleaningPattern) {
        this.startCleaningPattern = startCleaningPattern;
    }

    public String getEndCleaningPattern() {
        return endCleaningPattern;
    }

    public void setEndCleaningPattern(String endCleaningPattern) {
        this.endCleaningPattern = endCleaningPattern;
    }

    public List<ReplacePair> getReplacePairs() {
        return replacePairs;
    }

    public void setReplacePairs(List<ReplacePair> replacePairs) {
        this.replacePairs = replacePairs;
    }
}
