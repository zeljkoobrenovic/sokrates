/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexUtils {
    public static final int MAX_TEXT_LENGTH = 1000;
    private static final Log LOG = LogFactory.getLog(RegexUtils.class);
    private static Map<String, Pattern> compiledPatterns = new HashMap<>();

    // Returns a Pattern for the given regex, compiling it at most once and caching it for reuse.
    // Throws PatternSyntaxException for an invalid pattern (callers decide how to handle it).
    public static Pattern getCompiledPattern(String regexPattern) {
        Pattern pattern = compiledPatterns.get(regexPattern);
        if (pattern == null) {
            pattern = Pattern.compile(regexPattern);
            compiledPatterns.put(regexPattern, pattern);
        }
        return pattern;
    }

    public static boolean matchesEntirely(String regexPattern, String content) {
        try {
            return getCompiledPattern(regexPattern).matcher(content).matches();
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
            // Delegate to matchesEntirely so each pattern is compiled once and cached; this is a hot
            // path (bot / team / ignore-contributor / scope matching). Same semantics as before:
            // full-string match, an invalid pattern is treated as non-matching.
            if (matchesEntirely(patternString, line)) {
                return true;
            }
        }
        return false;
    }

    // Case-insensitive full-string match. Used for identity matching (emails, userNames, teams) where
    // "Alice@Corp.com" and "alice@corp.com" must be treated as the same person. The pattern is compiled
    // with CASE_INSENSITIVE (+ UNICODE_CASE) and cached under a distinct key so it never collides with
    // the case-sensitive compilation of the same string.
    public static boolean matchesEntirelyIgnoreCase(String regexPattern, String content) {
        try {
            String cacheKey = "(?iu)" + regexPattern;
            Pattern pattern = compiledPatterns.get(cacheKey);
            if (pattern == null) {
                pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                compiledPatterns.put(cacheKey, pattern);
            }
            return pattern.matcher(content).matches();
        } catch (PatternSyntaxException e) {
            LOG.debug(e);
            return false;
        }
    }

    public static boolean matchesAnyPatternIgnoreCase(String line, List<String> patterns) {
        if (patterns == null) {
            return false;
        }
        for (String patternString : patterns) {
            if (matchesEntirelyIgnoreCase(patternString, line)) {
                return true;
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

    public static void reset() {
        compiledPatterns.clear();
    }
}
