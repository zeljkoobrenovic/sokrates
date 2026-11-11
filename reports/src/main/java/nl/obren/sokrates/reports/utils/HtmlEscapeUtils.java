/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.utils;

import java.nio.charset.StandardCharsets;

/**
 * Escaping for repository-controlled strings (file and folder names, unit names, git author
 * names and emails) that the server-rendered reports concatenate into HTML.
 *
 * <p>Two contexts, two encoders — pick the one the sink needs:
 * <ul>
 *   <li>{@link #escape(String)} for element content and for attribute values, whether the report
 *       quotes the attribute with {@code '} (the house style) or {@code "}. It rewrites the five
 *       HTML metacharacters only, so ordinary names come out byte-for-byte unchanged (unlike
 *       {@code StringEscapeUtils.escapeHtml4}, which also turns non-ASCII letters into entities and
 *       leaves {@code '} alone).</li>
 *   <li>{@link #viewerFileHref(String, String)} for the source-viewer link, whose path travels in
 *       the URL fragment: {@code #}, {@code &}, {@code %} and spaces would otherwise cut the
 *       parameter short in {@code src/viewer.html}'s {@code param()} parser. The path is
 *       percent-encoded (slashes kept, so links for ordinary paths are unchanged), which also leaves
 *       nothing for an attribute to escape.</li>
 * </ul>
 */
public class HtmlEscapeUtils {

    /**
     * HTML-escapes {@code text} for use as element content or as a single- or double-quoted
     * attribute value. {@code null} becomes the empty string.
     */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * The link to a cached source file in the shared viewer: {@code ../src/viewer.html#aspect=<aspect>&file=<path>}.
     * The path is percent-encoded with {@link #encodeFragmentComponent(String)}; the viewer's
     * {@code param()} runs {@code decodeURIComponent} on it, so the archive key it looks up is the
     * raw relative path again. The result is safe to place in an {@code href} attribute as is.
     */
    public static String viewerFileHref(String aspect, String relativePath) {
        return "../src/viewer.html#aspect=" + aspect + "&file=" + encodeFragmentComponent(relativePath);
    }

    /**
     * Percent-encodes a path for the {@code file=} parameter of the viewer's URL fragment.
     * Keeps RFC 3986 unreserved characters and {@code /} (so a normal path reads the same as
     * before); everything else — including {@code #}, {@code &}, {@code %}, {@code +}, spaces,
     * quotes and non-ASCII (as UTF-8 bytes) — becomes {@code %XX}. {@code +} must be encoded
     * because the viewer turns a literal {@code +} into a space before decoding.
     */
    public static String encodeFragmentComponent(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xff;
            if (isUnreservedFragmentByte(c)) {
                sb.append((char) c);
            } else {
                sb.append('%').append(Character.toUpperCase(Character.forDigit(c >> 4, 16)))
                        .append(Character.toUpperCase(Character.forDigit(c & 0xf, 16)));
            }
        }
        return sb.toString();
    }

    /** ASCII letters and digits, the RFC 3986 unreserved marks and the slash, which stay literal in a fragment. */
    private static boolean isUnreservedFragmentByte(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || c == '-' || c == '.' || c == '_' || c == '~' || c == '/';
    }
}
