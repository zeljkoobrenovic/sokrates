package nl.obren.sokrates.sourcecode.githistory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One git commit-message trailer ("Key: value" line at the end of the message body), e.g.
 * "Co-authored-by: Jane Doe <jane@acme.com>". Read from the optional git-commit-trailers.txt
 * sidecar (see {@link GitHistoryUtils#GIT_COMMIT_TRAILERS_FILE_NAME}).
 */
public class CommitTrailer {
    // "Key: value" — key starts with a letter, letters/digits/dashes, followed by ":" and a space.
    private static final Pattern TRAILER_LINE = Pattern.compile("^([A-Za-z][A-Za-z0-9-]*):\\s+(.+)$");
    private static final Pattern NAME_EMAIL = Pattern.compile("^(.*?)\\s*<([^<>]*)>\\s*$");

    private String key = "";
    private String value = "";

    public CommitTrailer() {
    }

    public CommitTrailer(String key, String value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Parses a "Key: value" line; returns null when the line is not a trailer.
     */
    public static CommitTrailer parse(String line) {
        if (line == null) {
            return null;
        }
        Matcher matcher = TRAILER_LINE.matcher(line.trim());
        if (!matcher.matches()) {
            return null;
        }
        return new CommitTrailer(matcher.group(1), matcher.group(2).trim());
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean hasKey(String otherKey) {
        return key != null && key.equalsIgnoreCase(otherKey);
    }

    /**
     * The display name part of a "Name <email>" value (the whole value when there is no
     * &lt;email&gt; part).
     */
    public String getName() {
        Matcher matcher = NAME_EMAIL.matcher(value);
        return matcher.matches() ? matcher.group(1).trim() : value.trim();
    }

    /**
     * The lowercased email part of a "Name <email>" value, or "" when there is none.
     */
    public String getEmail() {
        Matcher matcher = NAME_EMAIL.matcher(value);
        return matcher.matches() ? matcher.group(2).trim().toLowerCase() : "";
    }

    @Override
    public String toString() {
        return key + ": " + value;
    }
}
