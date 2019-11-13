package nl.obren.sokrates.common.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexUtils {
    private static final Log LOG = LogFactory.getLog(RegexUtils.class);
    public static final int MAX_TEXT_LENGTH = 1000;

    public static boolean matchesEntirely(String regexPattern, String content) {
        try {
            return Pattern.compile(regexPattern).matcher(content).matches();
        } catch (PatternSyntaxException e) {
            LOG.debug(e);
            return false;
        }
    }

    public static boolean doesNotMatchAnyPattern(String line, List<String> patterns) {
        return !matchesAnyPattern(line, patterns);
    }

    public static boolean matchesAnyPattern(String line, List<String> patterns) {
        for (String patternString : patterns) {
            try {
                if (Pattern.compile(patternString).matcher(line).matches()) {
                    return true;
                }
            } catch (PatternSyntaxException e) {
                LOG.debug(e);
            }
        }
        return false;
    }

    public static String getMatchedRegex(String text, String regex) {
        try {
            Pattern soe = Pattern.compile(regex);
            Matcher matcher = soe.matcher(StringUtils.left(text, MAX_TEXT_LENGTH));

            if (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                return unifyEndOfLineCharacters(text.substring(start, end));
            }
        } catch (PatternSyntaxException e) {
            LOG.debug(e);
        } catch (StackOverflowError e) {
            LOG.error(e);
        }
        return null;
    }

    public static List<String> getMatchedRegexesNoLimits(String text, String regex) {
        List<String> matches = new ArrayList<>();
        try {
            Pattern soe = Pattern.compile(regex);
            Matcher matcher = soe.matcher(text);

            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                matches.add(unifyEndOfLineCharacters(text.substring(start, end)));
            }
        } catch (PatternSyntaxException e) {
            LOG.debug(e);
        } catch (StackOverflowError e) {
            LOG.error(e);
        }
        return matches;
    }

    public static String getLastMatchedRegex(String text, String regex) {
        try {
            Pattern soe = Pattern.compile(regex);
            Matcher matcher = soe.matcher(text);

            String result = null;
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                result = unifyEndOfLineCharacters(text.substring(start, end));
            }
            return result;
        } catch (PatternSyntaxException e) {
            LOG.debug(e);
        } catch (StackOverflowError e) {
            LOG.error(e);
        }
        return null;
    }

    private static String unifyEndOfLineCharacters(String content) {
        return content.replace("\r\n", "\n").replace("\r", "\n").replace("\t", "    ");
    }
}
