package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import nl.obren.sokrates.sourcecode.landscape.PersonConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds/updates a {@link PeopleConfig} (config-people.json) by grouping contributor emails under
 * a shared display name (userName).
 *
 * <p>For each distinct userName, all emails that committed under that name are collected. The
 * {@code emailPatterns} list accumulates a literal regex per email (so the entry matches every one of
 * the person's addresses during analysis). The {@code email} field, however, holds a SINGLE address —
 * the latest-used email — and is only set when it is currently blank; an existing non-blank
 * {@code email} is left untouched. This is purely additive: it never removes emails, patterns, or
 * entries.
 */
public class PeopleConfigByUserNameUpdater {

    /**
     * One (email, userName) pair observed in the git history of some repository, with the commit date
     * (a {@code yyyy-MM-dd...} string, lexicographically comparable) used to pick the latest email.
     */
    public static class ContributorIdentity {
        private final String email;
        private final String userName;
        private final String date;

        public ContributorIdentity(String email, String userName) {
            this(email, userName, "");
        }

        public ContributorIdentity(String email, String userName, String date) {
            this.email = email != null ? email.trim() : "";
            this.userName = userName != null ? userName.trim() : "";
            this.date = date != null ? date.trim() : "";
        }

        public String getEmail() {
            return email;
        }

        public String getUserName() {
            return userName;
        }

        public String getDate() {
            return date;
        }
    }

    /**
     * Merges the observed identities into {@code peopleConfig}, grouping emails by userName. Mutates
     * and returns the same {@code peopleConfig} instance.
     *
     * @return the number of email patterns added (new entries + newly covered emails on existing ones).
     */
    public int update(PeopleConfig peopleConfig, List<ContributorIdentity> identities) {
        // userName -> existing PersonConfig (case-insensitive match on userName). Built from the
        // current config so additions land on the right entry.
        Map<String, PersonConfig> byUserName = new LinkedHashMap<>();

        peopleConfig.getPeople().forEach(person -> {
            String key = userNameKey(person.getUserName());
            if (StringUtils.isNotBlank(key)) {
                byUserName.putIfAbsent(key, person);
            }
        });

        // Group observed emails by userName (preserving first-seen order), and track the latest-used
        // email per userName by commit date (ties: the first one seen wins).
        Map<String, String> displayNameByKey = new LinkedHashMap<>();
        Map<String, Set<String>> emailsByUserName = new LinkedHashMap<>();
        Map<String, String> latestEmailByKey = new LinkedHashMap<>();
        Map<String, String> latestDateByKey = new LinkedHashMap<>();
        identities.forEach(identity -> {
            String userName = identity.getUserName();
            String email = identity.getEmail();
            if (StringUtils.isBlank(userName) || StringUtils.isBlank(email)) {
                return;
            }
            String key = userNameKey(userName);
            displayNameByKey.putIfAbsent(key, userName);
            emailsByUserName.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(email);

            String date = identity.getDate();
            if (!latestEmailByKey.containsKey(key) || date.compareTo(latestDateByKey.get(key)) > 0) {
                latestEmailByKey.put(key, email);
                latestDateByKey.put(key, date);
            }
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
            }

            // The email field holds a SINGLE address — the latest-used email — and is only filled when
            // currently blank; an existing non-blank email is never overwritten.
            if (StringUtils.isBlank(person.getEmail())) {
                person.setEmail(latestEmailByKey.get(key));
            }

            // emailPatterns accumulates a literal regex per email (so the entry matches every one of
            // the person's addresses). matchesAnyPattern is a full, case-sensitive match against the
            // lowercased contributor email; add a pattern for any email not already covered.
            addedCount += addEmailPatterns(peopleConfig, person, observedEmails);
        }

        return addedCount;
    }

    // Ensures person.emailPatterns covers each of `emails` (full-match, case-insensitive). Adds a
    // quoted literal regex for any email not already matched by a current pattern; never removes.
    // The pattern to add is skipped when that exact pattern string already exists on ANY OTHER person,
    // so a pattern is never duplicated across userNames (existing cross-person duplication is left
    // as-is). Returns the number of patterns added.
    private int addEmailPatterns(PeopleConfig peopleConfig, PersonConfig person, Set<String> emails) {
        // The set of pattern strings already used by every OTHER person.
        Set<String> patternsUsedByOthers = new HashSet<>();
        peopleConfig.getPeople().stream()
                .filter(other -> other != person)
                .forEach(other -> patternsUsedByOthers.addAll(other.getEmailPatterns()));

        List<String> patterns = person.getEmailPatterns();
        int added = 0;
        for (String email : emails) {
            String lower = email.toLowerCase();
            boolean covered = patterns.stream().anyMatch(p -> RegexUtils.matchesEntirely(p, lower));
            if (covered) {
                continue;
            }
            String pattern = emailToPattern(lower);
            // Do not add a pattern string that another userName already has.
            if (patternsUsedByOthers.contains(pattern)) {
                continue;
            }
            patterns.add(pattern);
            patternsUsedByOthers.add(pattern); // guard against re-adding within this same call
            added++;
        }
        return added;
    }

    // A regex that fully matches exactly this email. The address is lowercased to mirror how
    // contributor emails are normalized before matching; Pattern.quote makes regex metacharacters
    // (notably '.') literal so e.g. "a.b@x.io" doesn't also match "axb@xyio".
    static String emailToPattern(String email) {
        return java.util.regex.Pattern.quote(email.toLowerCase());
    }

    private String userNameKey(String userName) {
        return userName != null ? userName.trim().toLowerCase() : "";
    }
}
