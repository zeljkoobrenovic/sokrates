/*
 * Copyright (c) 2026 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.MaintainabilityScoresConfig;
import nl.obren.sokrates.sourcecode.core.ScoreFrameworkConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static nl.obren.sokrates.sourcecode.analysis.scores.MaintainabilityScoresAnalyzer.*;

/**
 * Why each maintainability sub-score matters for people and for AI coding agents, shown when a sub-score row of the
 * Highlights breakdown is opened. Built-in texts per sub-score key; a custom framework's {@code whyHuman}/{@code whyAi}
 * replace them (and are the only texts for its metric sub-scores).
 */
public class SubScoreExplanations {
    /** Why a sub-score matters: {@code human} and {@code ai} (either may be empty). */
    public static class Why {
        private final String human;
        private final String ai;

        public Why(String human, String ai) {
            this.human = human != null ? human : "";
            this.ai = ai != null ? ai : "";
        }

        public String getHuman() {
            return human;
        }

        public String getAi() {
            return ai;
        }

        public boolean isEmpty() {
            return human.isBlank() && ai.isBlank();
        }
    }

    static final Map<String, Why> BUILT_IN;

    static {
        Map<String, Why> why = new LinkedHashMap<>();
        why.put(VOLUME, new Why(
                "More code is more to learn, search and keep in mind; onboarding and judging a change's impact slow down as it grows.",
                "An agent sees only a slice at a time: the bigger the code base, the more it searches, and the likelier it misses relevant code or rewrites an existing helper."));
        why.put(DUPLICATION, new Why(
                "A fix in one copy must be repeated in all the others; a missed copy becomes a bug.",
                "Agents copy the patterns they find, so duplicates multiply; a change to one copy misses the others, and every copy costs context."));
        why.put(UNIT_SIZE, new Why(
                "Long functions do many things at once: they are hard to name, read, test and reuse.",
                "To change one line, an agent reads and often rewrites the whole function: bigger edits, more tokens, more breakage."));
        why.put(UNIT_COMPLEXITY, new Why(
                "Every branch is another path to understand and test; complex logic is where mistakes are made and missed in review.",
                "Nested conditions are where generated changes break edge cases; complex units need more tests and review before an agent's change can be trusted."));
        why.put(FILE_SIZE, new Why(
                "Large files are hard to navigate, mix responsibilities and attract merge conflicts.",
                "Agents read whole files into their context: every change to a large file costs many tokens and crowds out other relevant code."));
        why.put(FILE_COMPLEXITY, new Why(
                "Complex files concentrate risk: understanding one means keeping many paths in mind.",
                "Much to read and much logic to get right in one place: the costliest combination for an agent."));
        why.put(TEST_CODE, new Why(
                "Tests document the intended behaviour and make changes safe.",
                "Tests let an agent check its own work; without them its mistakes surface later, in review or in production."));
        why.put(CHANGE_ENTROPY, new Why(
                "When a typical change touches many components, every task needs knowledge of many parts, and the boundaries do not match how the code changes.",
                "Scattered changes make an agent find and load many places per task; each extra component adds context and another place to miss."));
        why.put(CONTEXT_PER_CHANGE, new Why(
                "The code people look at around a change: the more there is, the slower the change and its review.",
                "Lines read per change drive an agent's token cost and accuracy: the more it reads, the costlier and more error-prone each task."));
        why.put(KNOWLEDGE, new Why(
                "When a few people make most changes, the knowledge sits with them: their absence slows everyone, and reviews and onboarding depend on them.",
                "Not in the AI rating by default: an agent reads the code, not people's memory, though it gains when that knowledge is written down."));
        BUILT_IN = Collections.unmodifiableMap(why);
    }

    /** The texts per sub-score key for this analysis: built-in, replaced by a custom framework's own texts. */
    public static Map<String, Why> of(CodeAnalysisResults results) {
        Map<String, Why> explanations = new HashMap<>(BUILT_IN);
        MaintainabilityScoresConfig config = results != null && results.getCodeConfiguration() != null
                ? results.getCodeConfiguration().getAnalysis().getMaintainabilityScores() : null;
        if (config != null && config.isUseCustomFramework()) {
            for (ScoreFrameworkConfig.SubScoreConfig subScore : config.getCustomFramework().getSubScores()) {
                String key = subScore.getKey().trim();
                Why builtIn = subScore.getMetric().isBlank() ? BUILT_IN.get(key) : null;
                String human = !subScore.getWhyHuman().isBlank() ? subScore.getWhyHuman() : builtIn != null ? builtIn.getHuman() : "";
                String ai = !subScore.getWhyAi().isBlank() ? subScore.getWhyAi() : builtIn != null ? builtIn.getAi() : "";
                explanations.put(key, new Why(human, ai));
            }
        }
        return explanations;
    }
}
