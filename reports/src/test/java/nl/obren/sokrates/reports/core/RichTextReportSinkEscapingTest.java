package nl.obren.sokrates.reports.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tripwire for the escaping convention: a repository-controlled string (a file, folder, unit or
 * component name, a git author name or email) must not be concatenated raw into a {@link RichTextReport}
 * method that takes HTML by contract. The text-taking primitives in {@link #TEXT_PRIMITIVES} are the
 * default path; the HTML methods are for markup built on purpose.
 *
 * <p><b>What it reads:</b> every {@code .java} file under {@code reports/src/main/java} except
 * {@code RichTextReport.java} itself. In each file, every identifier declared with the type
 * {@code RichTextReport} (field, parameter or local — the declaration must spell the type) is a receiver;
 * every call {@code <receiver>.<sink>(...)} where {@code <sink>} is one of {@link #HTML_SINKS} has its
 * (balanced) argument text scanned; a call to, or a method reference ({@code ::getName}) of, one of
 * {@link #REPOSITORY_CONTROLLED_GETTERS} inside that text is a violation unless it sits inside an
 * argument of one of the {@link #SAFE_WRAPPERS} escapers, in one of the argument positions that wrapper
 * escapes (positions are counted by top-level commas; a comma inside an array initialiser or a generic
 * type argument shifts the count, so a positional wrapper can be misjudged either way in such contrived
 * arguments). Comments and string literals are blanked
 * first, so a getter name inside a literal does not count and a parenthesis inside a literal does not
 * unbalance the scan.
 *
 * <p><b>What it does not read</b> (so a clean run is a tripwire, not a proof): a getter result that
 * reaches a sink through a local variable, a field, a {@code StringBuilder}, or a helper method other
 * than the listed safe wrappers; a receiver reached through a method call ({@code r().addTableCell(..)})
 * or an untyped lambda parameter ({@code reports.forEach(r -> r.addTableCell(..))}); the {@code cli} and
 * {@code codeanalyzer} modules (no report calls there); and the client-rendered templates, which escape
 * in JavaScript. {@code cli}'s {@code ReportHtmlEscapingTest} is the end-to-end check that covers the
 * paths this scan cannot see. Safe wrappers are matched on the qualified callee as written: a static
 * import ({@code escape(x)}) or a line break before {@code .escape} is not recognised and is reported,
 * which is the safe direction.
 *
 * <p>To fix a finding, use the {@code ...Text} primitive (or {@code HtmlEscapeUtils.escape} when the
 * argument mixes text with markup). To add a method to {@code RichTextReport}, classify it here:
 * {@link #HTML_SINKS}, {@link #STRUCTURAL} (its string arguments are ids, styles or links, which this
 * scan does not check at all; ids land in an {@code id='…'} attribute and an {@code onclick} JS string,
 * for which {@code escape} is not the right encoder, so a repository-controlled id would need its own
 * encoder and is not covered here — today they carry code or config values)
 * or {@link #TEXT_PRIMITIVES} (and then cover it in {@code RichTextReportTest}).
 */
class RichTextReportSinkEscapingTest {

    /** The escaping primitives: text in, escaped HTML out. Classified by an explicit list, never by name suffix. */
    static final Set<String> TEXT_PRIMITIVES = new HashSet<>(Arrays.asList(
            "addText", "addParagraphText", "addListItemText", "addTableCellText", "addContentInDivText",
            "addTabText", "startSubSectionText"));

    /** RichTextReport methods whose String arguments are HTML by contract. */
    static final Set<String> HTML_SINKS = new HashSet<>(Arrays.asList(
            "addHtmlContent", "addTableCell", "addTableCellWithTitle", "addMultiColumnTableCell",
            "addTableHeader", "addTableHeaderLeft", "addListItem", "addParagraph", "addParagraphWithTooltip",
            "addEmphasisedParagraph", "addQuoteParagraph",
            "addLevel1Header", "addLevel2Header", "addLevel3Header", "addLevel4Header",
            "addContentInDiv", "addContentInDivWithTooltip", "addContentInSpan", "addTextArea",
            "startSection", "startSubSection", "startSubSectionNoMargins", "startDivWithLabel", "startDiv",
            "addTab", "addLinkInTab", "addNewTabLink", "addDetailsBlock", "startDetailsBlock",
            "startDetailsBlockMinimalistic", "startShowMoreBlockDisappear",
            "addSvgFigure", "addGraphvizFigure", "addHiddenGraphvizFigure"));

    /** RichTextReport methods whose String arguments are ids, styles, links or report metadata. */
    static final Set<String> STRUCTURAL = new HashSet<>(Arrays.asList(
            "startTable", "startDataTable", "startTableCell", "startTableCellColSpan", "startMultiColumnTableCell", "startTableRow",
            "startUnorderedList", "startSpan", "startTabContentSection", "startNewTabLink", "addAnchor",
            "setId", "setFileName", "setDisplayName", "setGroup", "setDescription", "setLogoLink", "setParentUrl",
            "setRichTextFragments", "setBreadcrumbs", "setReportsFolder", "setEmbedded", "setRenderLogo", "setNavigation"));

    /** Getters whose result comes from the analyzed repository (paths, names, git identities). */
    static final Set<String> REPOSITORY_CONTROLLED_GETTERS = new HashSet<>(Arrays.asList(
            "getName", "getRelativePath", "getPath", "getShortName", "getEmail", "getUserName",
            "getFromComponent", "getToComponent"));

    /**
     * Calls that make an argument safe for an HTML sink, with the 0-based argument positions they escape
     * (empty set = every argument). Matched on the qualified callee as written, or on any suffix of it
     * that starts at a dot; an unqualified entry therefore also matches a bare call from the same class.
     */
    static final Map<String, Set<Integer>> SAFE_WRAPPERS = new HashMap<>();
    static {
        SAFE_WRAPPERS.put("HtmlEscapeUtils.escape", Collections.emptySet());
        SAFE_WRAPPERS.put("HtmlEscapeUtils.viewerFileHref", Collections.singleton(1)); // encodes the path; the aspect is concatenated as is
        SAFE_WRAPPERS.put("HtmlEscapeUtils.encodeFragmentComponent", Collections.emptySet());
        SAFE_WRAPPERS.put("SystemUtils.getSafeFileName", Collections.emptySet());
        SAFE_WRAPPERS.put("DataExporter.dependenciesFileNamePrefix", Collections.emptySet()); // safe file name of both components
        SAFE_WRAPPERS.put("getFeatureOfInterestCard", Collections.singleton(0)); // SummaryUtils escapes the name (SummaryUtilsFeatureCardTest)
        SAFE_WRAPPERS.put("getPercentageSvg", Collections.singleton(1));         // SimpleOneBarChart escapes textLeft only; textRight is HTML by contract
    }

    private static final Pattern RECEIVER_DECLARATION = Pattern.compile("\\bRichTextReport\\s+([A-Za-z_]\\w*)");
    private static final Pattern CALLEE_BEFORE_PAREN = Pattern.compile("([A-Za-z_][\\w.]*)\\s*$");

    @Test
    void everyPublicRichTextReportMethodTakingAStringIsClassified() {
        Set<String> unclassified = new TreeSet<>();
        for (Method method : RichTextReport.class.getMethods()) {
            if (method.getDeclaringClass() != RichTextReport.class || !Modifier.isPublic(method.getModifiers())) continue;
            boolean takesString = Arrays.stream(method.getParameterTypes()).anyMatch(t -> t == String.class || t == String[].class);
            if (!takesString) continue;
            String name = method.getName();
            if (TEXT_PRIMITIVES.contains(name) || HTML_SINKS.contains(name) || STRUCTURAL.contains(name)) continue;
            unclassified.add(name);
        }
        assertTrue(unclassified.isEmpty(), "new RichTextReport method(s) not classified as HTML sink, structural or text primitive: " + unclassified);
        Set<String> declared = Arrays.stream(RichTextReport.class.getMethods()).map(Method::getName).collect(Collectors.toSet());
        Set<String> stale = Stream.of(HTML_SINKS, STRUCTURAL, TEXT_PRIMITIVES).flatMap(Set::stream)
                .filter(n -> !declared.contains(n)).collect(Collectors.toCollection(TreeSet::new));
        assertTrue(stale.isEmpty(), "classified names that no longer exist on RichTextReport: " + stale);
        Set<String> overlap = TEXT_PRIMITIVES.stream().filter(n -> HTML_SINKS.contains(n) || STRUCTURAL.contains(n)).collect(Collectors.toSet());
        assertTrue(overlap.isEmpty(), "a method cannot be both a text primitive and a sink: " + overlap);
    }

    @Test
    void noRepositoryControlledGetterReachesAnHtmlSinkUnescaped() throws IOException {
        Path sources = Paths.get(System.getProperty("basedir", "")).resolve("src/main/java").toAbsolutePath();
        assertTrue(Files.isDirectory(sources), "reports main sources not found at " + sources);

        List<Path> files;
        try (Stream<Path> paths = Files.walk(sources)) {
            files = paths.filter(p -> p.toString().endsWith(".java") && !p.getFileName().toString().equals("RichTextReport.java"))
                    .sorted().collect(Collectors.toList());
        }
        Scan scan = new Scan();
        for (Path file : files) {
            scan.file(sources.relativize(file).toString(), Files.readString(file, UTF_8));
        }
        // Controls: the scan must have read the real generators, or an empty result proves nothing. Measured
        // 2026-09: 115 files, 33 with receivers, ~1500 sink calls, 24 getters (escaped or not) inside them.
        // Migrating sites to the text primitives lowers the last two; lower the threshold then, with the new number.
        assertTrue(scan.filesScanned > 100, "expected the whole reports module to be read, scanned " + scan.filesScanned + " files");
        assertTrue(scan.filesWithReceivers > 25, "expected the report generators to be scanned, found receivers in " + scan.filesWithReceivers + " files");
        assertTrue(scan.sinkCalls > 1000, "expected many sink calls, saw " + scan.sinkCalls);
        assertTrue(scan.getterCallsSeen > 10, "expected repository-controlled getters inside sink calls (escaped or not), saw " + scan.getterCallsSeen);

        assertTrue(scan.violations.isEmpty(), "repository-controlled strings concatenated raw into an HTML-by-contract RichTextReport method "
                + "(use the ...Text primitive, or HtmlEscapeUtils.escape when mixing with markup):\n  " + String.join("\n  ", scan.violations));
    }

    @Test
    void scanFlagsARawGetterAndAcceptsTheEscapedAndTextForms() {
        String[] lines = {
                "class G { void g(RichTextReport report, SourceFile f, SimpleOneBarChart chart) {",
                "  report.addTableCell(\"<b>\" + f.getRelativePath() + \"</b>\");",             // 2 raw: violation
                "  report.addTableCell(HtmlEscapeUtils.escape(f.getRelativePath()));",          // 3 escaped: fine
                "  report.addTableCellText(f.getRelativePath());",                             // 4 text primitive: fine
                "  report.startTabContentSection(f.getName(), true);",                          // 5 structural: not checked
                "  report.addListItem(\"f.getName() in a literal (\" + 1 + \")\");",            // 6 literal: fine
                "  report.addParagraph(StringUtils.abbreviate(f.getName(), 30));",              // 7 raw through a non-escaping helper: violation
                "  report.addListItem(f.getParent().getName() + \" (\" + 1 + \")\");",          // 8 chained call: violation
                "  report.addHtmlContent(getName());",                                         // 9 no receiver at all: not a value's getter, fine
                "  report.addEmphasisedParagraph(f.getShortName());",                          // 10 a plain HTML sink: violation
                "  report.addContentInDiv(chart.getPercentageSvg(1, f.getName(), \"x\"));",     // 11 getter in the escaped textLeft: fine
                "  report.addContentInDiv(chart.getPercentageSvg(1, \"x\", f.getName()));",     // 12 getter in the HTML-by-contract textRight: violation
                "  report.addTableCell(escape(f.getName()));",                                 // 13 unqualified escaper is not recognised: violation (safe direction)
                "  report.addTableCell(files.stream().map(SourceFile :: getRelativePath).collect(Collectors.joining(\", \")));", // 14 method reference (spaces allowed): violation
                "  report.addTableCell(HtmlEscapeUtils.escape(files.stream().map(SourceFile::getName).collect(Collectors.joining())));", // 15 escaped: fine
                "  report.addTableCell(\"<a href='\" + HtmlEscapeUtils.viewerFileHref(f.getName(), \"p\") + \"'>\");", // 16 getter in the aspect, which viewerFileHref does not encode: violation
                "  report.addTableCell(names.stream().map(this::getName).collect(Collectors.joining()));", // 17 the generator's own method: fine
                "} }"};
        Scan scan = new Scan();
        scan.file("G.java", String.join("\n", lines));
        List<Integer> flaggedLines = scan.violations.stream().map(v -> Integer.parseInt(v.substring("G.java:".length(), v.indexOf(' ')))).collect(Collectors.toList());
        assertEquals(Arrays.asList(2, 7, 8, 10, 12, 13, 14, 16), flaggedLines, scan.violations.toString());
        assertEquals(14, scan.sinkCalls, "the text-primitive and structural calls are not sink calls");
    }

    /** A source scan: receivers by declaration, sink calls by balanced parentheses, getters by enclosing-call stack. */
    static class Scan {
        final List<String> violations = new ArrayList<>();
        int filesScanned;
        int filesWithReceivers;
        int sinkCalls;
        int getterCallsSeen;

        void file(String name, String source) {
            filesScanned++;
            String code = blankCommentsAndLiterals(source);
            Set<String> receivers = new HashSet<>();
            Matcher declarations = RECEIVER_DECLARATION.matcher(code);
            while (declarations.find()) receivers.add(declarations.group(1));
            if (receivers.isEmpty()) return;
            filesWithReceivers++;
            String alternatives = receivers.stream().map(Pattern::quote).collect(Collectors.joining("|"));
            Matcher calls = Pattern.compile("(?<![\\w.])(?:this\\.)?(?:" + alternatives + ")\\.(\\w+)\\s*\\(").matcher(code);
            while (calls.find()) {
                String method = calls.group(1);
                int open = calls.end() - 1;
                int close = matchingParen(code, open);
                if (close < 0) continue;
                if (!HTML_SINKS.contains(method)) continue;
                sinkCalls++;
                scanArguments(name, code, open, close, method);
            }
        }

        /** One enclosing call while scanning: its callee as written and the index of the argument currently being read. */
        private static class Frame {
            final String callee;
            int argIndex;
            Frame(String callee) { this.callee = callee; }
        }

        private void scanArguments(String file, String code, int open, int close, String sink) {
            List<Frame> stack = new ArrayList<>();
            for (int i = open + 1; i < close; i++) {
                char c = code.charAt(i);
                if (c == '(') {
                    Matcher callee = CALLEE_BEFORE_PAREN.matcher(code.substring(open + 1, i));
                    boolean found = callee.find();
                    String name = found ? callee.group(1) : "";
                    int nameStart = found ? open + 1 + callee.start(1) : i;
                    // a.getName() and a.b().getName() are calls on a value; a bare getName() is the generator's own
                    boolean onAValue = name.contains(".") || (nameStart > 0 && code.charAt(nameStart - 1) == '.');
                    int j = i + 1;
                    while (j < close && Character.isWhitespace(code.charAt(j))) j++;
                    boolean getterCall = j < close && code.charAt(j) == ')' && REPOSITORY_CONTROLLED_GETTERS.contains(lastSegment(name)) && onAValue;
                    if (getterCall) {
                        checkGetter(file, code, i, sink, name + "()", stack);
                    }
                    stack.add(new Frame(name));
                } else if (c == ':' && i + 1 < close && code.charAt(i + 1) == ':') {
                    // a method reference such as SourceFile::getName inside a stream pipeline
                    Matcher ref = Pattern.compile("^::\\s*(\\w+)").matcher(code.substring(i, Math.min(close, i + 60)));
                    boolean onThis = code.substring(Math.max(open, i - 8), i).trim().endsWith("this");
                    if (ref.find() && REPOSITORY_CONTROLLED_GETTERS.contains(ref.group(1)) && !onThis) {
                        checkGetter(file, code, i, sink, "::" + ref.group(1), stack);
                    }
                    i++;
                } else if (c == ')') {
                    if (!stack.isEmpty()) stack.remove(stack.size() - 1);
                } else if (c == ',' && !stack.isEmpty()) {
                    stack.get(stack.size() - 1).argIndex++;
                }
            }
        }

        private void checkGetter(String file, String code, int at, String sink, String what, List<Frame> stack) {
            getterCallsSeen++;
            if (stack.stream().noneMatch(Scan::escapesCurrentArgument)) {
                violations.add(file + ":" + lineOf(code, at) + " " + sink + "(...) carries " + what + " unescaped");
            }
        }

        private static boolean escapesCurrentArgument(Frame frame) {
            for (Map.Entry<String, Set<Integer>> safe : SAFE_WRAPPERS.entrySet()) {
                boolean nameMatches = frame.callee.equals(safe.getKey()) || frame.callee.endsWith("." + safe.getKey());
                if (nameMatches && (safe.getValue().isEmpty() || safe.getValue().contains(frame.argIndex))) return true;
            }
            return false;
        }

        private static String lastSegment(String callee) {
            int dot = callee.lastIndexOf('.');
            return dot < 0 ? callee : callee.substring(dot + 1);
        }

        private static int lineOf(String code, int index) {
            int line = 1;
            for (int i = 0; i < index; i++) if (code.charAt(i) == '\n') line++;
            return line;
        }

        private static int matchingParen(String code, int open) {
            int depth = 0;
            for (int i = open; i < code.length(); i++) {
                char c = code.charAt(i);
                if (c == '(') depth++;
                else if (c == ')' && --depth == 0) return i;
            }
            return -1;
        }

        /** Replaces the bodies of comments and string/char literals with spaces, keeping every newline so line numbers hold. */
        static String blankCommentsAndLiterals(String source) {
            StringBuilder out = new StringBuilder(source.length());
            int i = 0, n = source.length();
            while (i < n) {
                char c = source.charAt(i);
                if (c == '/' && i + 1 < n && source.charAt(i + 1) == '/') {
                    while (i < n && source.charAt(i) != '\n') { out.append(' '); i++; }
                } else if (c == '/' && i + 1 < n && source.charAt(i + 1) == '*') {
                    int end = source.indexOf("*/", i + 2);
                    end = end < 0 ? n : end + 2;
                    for (; i < end; i++) out.append(source.charAt(i) == '\n' ? '\n' : ' ');
                } else if (c == '"' || c == '\'') {
                    char quote = c;
                    out.append(' '); i++;
                    while (i < n && source.charAt(i) != quote) {
                        if (source.charAt(i) == '\\' && i + 1 < n) { out.append(' '); i++; }
                        out.append(source.charAt(i) == '\n' ? '\n' : ' '); i++;
                    }
                    if (i < n) { out.append(' '); i++; }
                } else {
                    out.append(c); i++;
                }
            }
            return out.toString();
        }
    }
}
