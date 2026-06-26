package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import nl.obren.sokrates.sourcecode.landscape.PersonConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds/updates a {@link PeopleConfig} (config-people.json) by grouping contributor emails under
 * a shared display name (userName).
 *
 * <p>For each distinct userName, all emails that committed under that name are collected and joined
 * into the {@code email} field separated by {@code ";"}. When an entry with the same userName already
 * exists, only NEW emails are appended (to both the {@code email} field and a normalized set used for
 * de-duplication). This is purely additive: it never removes emails or entries, and it never reorders
 * existing emails — new ones are appended after the existing ones.
 */
public class PeopleConfigByUserNameUpdater {
    public static final String EMAIL_SEPARATOR = ";";

    /**
     * One (email, userName) pair observed in the git history of some repository.
     */
    public static class ContributorIdentity {
        private final String email;
        private final String userName;

        public ContributorIdentity(String email, String userName) {
            this.email = email != null ? email.trim() : "";
            this.userName = userName != null ? userName.trim() : "";
        }

        public String getEmail() {
            return email;
        }

        public String getUserName() {
            return userName;
        }
    }

    /**
     * Merges the observed identities into {@code peopleConfig}, grouping emails by userName. Mutates
     * and returns the same {@code peopleConfig} instance.
     *
     * @return the number of email addresses added (new entries + appended emails on existing ones).
     */
    public int update(PeopleConfig peopleConfig, List<ContributorIdentity> identities) {
        // userName -> existing PersonConfig (case-insensitive match on userName). Built from the
        // current config so additions land on the right entry.
        Map<String, PersonConfig> byUserName = new LinkedHashMap<>();
        // PersonConfig -> normalized (lowercased) set of emails already present, for de-duplication.
        Map<PersonConfig, Set<String>> existingEmails = new LinkedHashMap<>();

        peopleConfig.getPeople().forEach(person -> {
            // De-dup set is normalized (lowercased); the email FIELD keeps its original casing.
            Set<String> normalized = new LinkedHashSet<>();
            parseEmails(person.getEmail()).forEach(email -> normalized.add(email.toLowerCase()));
            existingEmails.put(person, normalized);
            String key = userNameKey(person.getUserName());
            if (StringUtils.isNotBlank(key)) {
                byUserName.putIfAbsent(key, person);
            }
        });

        // Group observed emails by userName, preserving first-seen order of both names and emails.
        Map<String, String> displayNameByKey = new LinkedHashMap<>();
        Map<String, Set<String>> emailsByUserName = new LinkedHashMap<>();
        identities.forEach(identity -> {
            String userName = identity.getUserName();
            String email = identity.getEmail();
            if (StringUtils.isBlank(userName) || StringUtils.isBlank(email)) {
                return;
            }
            String key = userNameKey(userName);
            displayNameByKey.putIfAbsent(key, userName);
            emailsByUserName.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(email);
        });

        int addedCount = 0;
        for (Map.Entry<String, Set<String>> entry : emailsByUserName.entrySet()) {
            String key = entry.getKey();
            Set<String> observedEmails = entry.getValue();

            PersonConfig person = byUserName.get(key);
            if (person == null) {
                // New entry for this userName.
                person = new PersonConfig();
                person.setUserName(displayNameByKey.get(key));
                peopleConfig.getPeople().add(person);
                byUserName.put(key, person);
                existingEmails.put(person, new LinkedHashSet<>());
            }

            Set<String> present = existingEmails.get(person);
            List<String> toAppend = new ArrayList<>();
            for (String email : observedEmails) {
                if (present.add(email.toLowerCase())) {
                    toAppend.add(email);
                }
            }
            if (!toAppend.isEmpty()) {
                List<String> emails = new ArrayList<>(parseEmails(person.getEmail()));
                emails.addAll(toAppend);
                person.setEmail(String.join(EMAIL_SEPARATOR, emails));
                addedCount += toAppend.size();
            }

            // Keep emailPatterns in lockstep with the emails: each email must be matched by a pattern
            // (RegexUtils.matchesAnyPattern is a full, case-sensitive match against the lowercased
            // contributor email), otherwise the entry would never match any contributor. Add a literal
            // regex per email not already covered by an existing pattern.
            addEmailPatterns(person, observedEmails);
        }

        return addedCount;
    }

    // Ensures person.emailPatterns covers each of `emails` (full-match, case-insensitive). Adds a
    // quoted literal regex for any email not already matched by a current pattern; never removes.
    private void addEmailPatterns(PersonConfig person, Set<String> emails) {
        List<String> patterns = person.getEmailPatterns();
        for (String email : emails) {
            String lower = email.toLowerCase();
            boolean covered = patterns.stream().anyMatch(p -> RegexUtils.matchesEntirely(p, lower));
            if (!covered) {
                patterns.add(emailToPattern(lower));
            }
        }
    }

    // A regex that fully matches exactly this email. The address is lowercased to mirror how
    // contributor emails are normalized before matching; Pattern.quote makes regex metacharacters
    // (notably '.') literal so e.g. "a.b@x.io" doesn't also match "axb@xyio".
    static String emailToPattern(String email) {
        return java.util.regex.Pattern.quote(email.toLowerCase());
    }

    // Splits a ";"-joined email field into its trimmed, non-blank parts (original casing, order preserved).
    private Set<String> parseEmails(String emailField) {
        Set<String> emails = new LinkedHashSet<>();
        if (StringUtils.isBlank(emailField)) {
            return emails;
        }
        for (String part : emailField.split(EMAIL_SEPARATOR)) {
            String trimmed = part.trim();
            if (StringUtils.isNotBlank(trimmed)) {
                emails.add(trimmed);
            }
        }
        return emails;
    }

    private String userNameKey(String userName) {
        return userName != null ? userName.trim().toLowerCase() : "";
    }
}
