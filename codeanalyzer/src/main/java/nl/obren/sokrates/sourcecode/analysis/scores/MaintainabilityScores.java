/*
 * Copyright (c) 2026 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.scores;

/**
 * How easy a repository is to understand and change, for people ({@link #human}) and for AI coding agents
 * ({@link #ai}): the same measured sub-scores, weighted differently. {@link #contextLinesPerChange} is the AI
 * score's measure in physical units — the current lines of the main files an average change of the past year
 * touched, i.e. what an agent reads to make it (0 without history). Serialized in {@code analysisResults.json},
 * so landscapes read it; null when the scores are disabled or there is no main code.
 */
public class MaintainabilityScores {
    private MaintainabilityScore human;
    private MaintainabilityScore ai;
    private int contextLinesPerChange;
    private int changesMeasured;

    public MaintainabilityScore getHuman() {
        return human;
    }

    public void setHuman(MaintainabilityScore human) {
        this.human = human;
    }

    public MaintainabilityScore getAi() {
        return ai;
    }

    public void setAi(MaintainabilityScore ai) {
        this.ai = ai;
    }

    public int getContextLinesPerChange() {
        return contextLinesPerChange;
    }

    public void setContextLinesPerChange(int contextLinesPerChange) {
        this.contextLinesPerChange = contextLinesPerChange;
    }

    public int getChangesMeasured() {
        return changesMeasured;
    }

    public void setChangesMeasured(int changesMeasured) {
        this.changesMeasured = changesMeasured;
    }
}
