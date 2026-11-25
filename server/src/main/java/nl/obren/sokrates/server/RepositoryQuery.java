package nl.obren.sokrates.server;

import nl.obren.sokrates.server.api.Api;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * The repository filter, a subset of the landscape repositories list's search syntax: terms split by {@code ;};
 * {@code mainLang:<l1>,<l2>} (the main language is one of these), {@code includesLang:<l1>,<l2>} (some code, in any
 * scope, in one of these; each such term is its own constraint); any other term matches the name, id or
 * description, and several plain terms are a union. Languages are lowercased file extensions (java, cs, tf).
 */
class RepositoryQuery {
    private final List<String> plainTerms = new ArrayList<>();
    private List<String> mainLanguages;
    private final List<List<String>> includedLanguages = new ArrayList<>();

    static RepositoryQuery parse(String query) {
        RepositoryQuery result = new RepositoryQuery();
        for (String term : SokratesQueryService.terms(query)) {
            if (term.startsWith("mainlang:")) {
                result.mainLanguages = languages(term.substring("mainlang:".length()));
            } else if (term.startsWith("includeslang:")) {
                result.includedLanguages.add(languages(term.substring("includeslang:".length())));
            } else {
                result.plainTerms.add(term);
            }
        }
        return result;
    }

    private static List<String> languages(String list) {
        return Arrays.stream(list.split(",")).map(String::trim).filter(StringUtils::isNotBlank)
                .map(language -> language.toLowerCase(Locale.ROOT)).collect(Collectors.toList());
    }

    boolean matches(Api.RepositoryItem item, List<String> languages) {
        if (mainLanguages != null && (item.mainLanguage() == null || !mainLanguages.contains(item.mainLanguage()))) {
            return false;
        }
        for (List<String> anyOf : includedLanguages) {
            if (anyOf.stream().noneMatch(languages::contains)) {
                return false;
            }
        }
        if (plainTerms.isEmpty()) {
            return true;
        }
        String text = (item.name() + " " + item.id() + " " + StringUtils.defaultString(item.description())).toLowerCase(Locale.ROOT);
        return plainTerms.stream().anyMatch(text::contains);
    }
}
