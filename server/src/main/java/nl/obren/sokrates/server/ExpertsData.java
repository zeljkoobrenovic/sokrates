package nl.obren.sokrates.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.obren.sokrates.server.api.Api;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * "Who knows this?" from two data.zip entries: {@code text/mainFileContributors.txt} (one line per file and
 * contributor, written by the analysis with the identities already resolved) and {@code files.json} (each file's
 * logical components as {@code <decomposition>::<component>}), whose columns and fields are found by name.
 */
class ExpertsData {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    static final int TOP_REPOSITORIES = 3;

    record FileContributor(String path, String contributor, int commits, int commits365, String lastCommit) {
    }

    /** One person's changes to one file of one repository, the person already in their final identity. */
    record Change(String repository, String path, String contributor, boolean bot, int commits, int commits365, String lastCommit) {
    }

    /** What part of the code is asked about; every given part must match (none: all main code). */
    record Scope(String path, String component, String decomposition, Set<String> languages) {
        static Scope of(String path, String component, String decomposition, String languages) {
            Set<String> set = new LinkedHashSet<>();
            if (StringUtils.isNotBlank(languages)) {
                for (String language : languages.split(",")) {
                    if (StringUtils.isNotBlank(language)) {
                        set.add(StringUtils.removeStart(language.trim().toLowerCase(Locale.ROOT), "."));
                    }
                }
            }
            return new Scope(StringUtils.trimToNull(path), StringUtils.trimToNull(component), StringUtils.trimToNull(decomposition), set);
        }

        boolean hasComponent() {
            return component != null;
        }

        String describe() {
            List<String> parts = new ArrayList<>();
            if (component != null) {
                parts.add("component " + component);
            }
            if (path != null) {
                parts.add("path " + path);
            }
            if (!languages.isEmpty()) {
                parts.add("language " + String.join(",", languages));
            }
            return parts.isEmpty() ? "all main code" : String.join(", ", parts);
        }

        /** Whether a file is in scope; {@code componentFiles} is null when no component was asked for. */
        boolean matches(String file, Set<String> componentFiles) {
            return (componentFiles == null || componentFiles.contains(file))
                    && (path == null || inPath(file, path))
                    && (languages.isEmpty() || languages.contains(extension(file)));
        }
    }

    static List<FileContributor> readFileContributors(String text) {
        List<FileContributor> rows = new ArrayList<>();
        if (StringUtils.isBlank(text)) {
            return rows;
        }
        String[] lines = text.split("\\r?\\n");
        List<String> header = List.of(lines[0].split("\t"));
        int path = header.indexOf("path");
        int contributor = header.indexOf("contributor");
        int commits = header.indexOf("# commits");
        int commits365 = header.indexOf("# commits (365d)");
        int last = header.indexOf("last commit");
        for (int i = 1; i < lines.length; i++) {
            String[] cells = lines[i].split("\t", -1);
            if (path < 0 || contributor < 0 || cells.length <= Math.max(path, contributor)) {
                continue;
            }
            rows.add(new FileContributor(cells[path], cells[contributor], number(cells, commits), number(cells, commits365),
                    last >= 0 && last < cells.length ? StringUtils.trimToNull(cells[last]) : null));
        }
        return rows;
    }

    /** The main files of a component: by "decomposition::component", or by component name in any (or the given) decomposition. */
    static Set<String> filesOfComponent(String filesJson, String component, String decomposition) {
        Set<String> files = new LinkedHashSet<>();
        if (filesJson == null) {
            return files;
        }
        try {
            for (JsonNode file : MAPPER.readTree(filesJson)) {
                for (JsonNode node : file.path("components")) {
                    String key = node.asText("");
                    String name = key.contains("::") ? key.substring(key.indexOf("::") + 2) : key;
                    String keyDecomposition = key.contains("::") ? key.substring(0, key.indexOf("::")) : "";
                    boolean match = key.equalsIgnoreCase(component)
                            || (name.equalsIgnoreCase(component) && (StringUtils.isBlank(decomposition) || keyDecomposition.equalsIgnoreCase(decomposition)));
                    if (match) {
                        files.add(file.path("relativePath").asText(""));
                    }
                }
            }
        } catch (Exception e) {
            return files;
        }
        return files;
    }

    /** A file path matches itself and, as a folder, everything below it ("src/payments" and "src/payments/"). */
    static boolean inPath(String file, String path) {
        String folder = StringUtils.removeEnd(StringUtils.removeStart(path.trim(), "/"), "/");
        return folder.isEmpty() || file.equals(folder) || file.startsWith(folder + "/");
    }

    static String extension(String file) {
        String name = file.substring(file.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        return dot <= 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * @param label     the repository name, or the landscape for several repositories
     * @param files     the number of files in scope (including those nobody changed)
     * @param acrossRepositories whether to fill the repository counts
     */
    static Api.Experts aggregate(String label, String scope, int files, List<Change> changes, boolean acrossRepositories,
                                 Integer notCovered, int limit) {
        Map<String, Person> people = new LinkedHashMap<>();
        Set<String> changed365 = new HashSet<>();
        Set<String> repositories = new HashSet<>();
        for (Change change : changes) {
            people.computeIfAbsent(change.contributor(), Person::new).add(change);
            repositories.add(change.repository());
            if (change.commits365() > 0) {
                changed365.add(change.repository() + "\u0000" + change.path());
            }
        }
        List<Api.Expert> experts = people.values().stream().map(person -> person.toExpert(acrossRepositories)).collect(Collectors.toList());
        experts.sort(Comparator.comparingInt(Api.Expert::fileChanges365Days).reversed()
                .thenComparing(Comparator.comparingInt(Api.Expert::fileChanges).reversed())
                .thenComparing(Api.Expert::contributor));
        int recentChanges = experts.stream().filter(e -> !e.bot()).mapToInt(Api.Expert::fileChanges365Days).sum();
        Double topShare = recentChanges == 0 ? null : SokratesQueryService.round(100.0 * experts.stream().filter(e -> !e.bot())
                .findFirst().map(Api.Expert::fileChanges365Days).orElse(0) / recentChanges);
        return new Api.Experts(label, scope, files, changed365.size(), experts.size(), topShare,
                acrossRepositories ? repositories.size() : null, acrossRepositories ? notCovered : null,
                experts.size() > limit ? new ArrayList<>(experts.subList(0, limit)) : experts);
    }

    private static class Person {
        private final String contributor;
        private boolean bot;
        private final Set<String> files = new HashSet<>();
        private int changes;
        private int changes365;
        private String last;
        private final Map<String, int[]> perRepository = new HashMap<>(); // changes365, changes

        Person(String contributor) {
            this.contributor = contributor;
        }

        void add(Change change) {
            bot |= change.bot();
            files.add(change.repository() + "\u0000" + change.path());
            changes += change.commits();
            changes365 += change.commits365();
            if (change.lastCommit() != null && (last == null || change.lastCommit().compareTo(last) > 0)) {
                last = change.lastCommit();
            }
            int[] counts = perRepository.computeIfAbsent(change.repository(), r -> new int[2]);
            counts[0] += change.commits365();
            counts[1] += change.commits();
        }

        Api.Expert toExpert(boolean acrossRepositories) {
            List<String> top = null;
            if (acrossRepositories) {
                top = perRepository.entrySet().stream()
                        .sorted(Comparator.comparingInt((Map.Entry<String, int[]> e) -> e.getValue()[0]).reversed()
                                .thenComparing(Comparator.comparingInt((Map.Entry<String, int[]> e) -> e.getValue()[1]).reversed())
                                .thenComparing(Map.Entry::getKey))
                        .limit(TOP_REPOSITORIES).map(Map.Entry::getKey).collect(Collectors.toList());
            }
            return new Api.Expert(contributor, bot, files.size(), changes, changes365, last,
                    acrossRepositories ? perRepository.size() : null, top);
        }
    }

    private static int number(String[] cells, int column) {
        if (column < 0 || column >= cells.length) {
            return 0;
        }
        try {
            return Integer.parseInt(cells[column].trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
