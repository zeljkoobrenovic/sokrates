/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.analysis;

import org.apache.commons.lang3.StringUtils;

public class Finding {

    public enum Assessment {
        POSITIVE("+"), NEGATIVE("-"), NEUTRAL("±"), NONE("");
        private String displayText;

        Assessment(String displayText) {
            this.displayText = displayText;
        }

        public String getDisplayText() {
            return displayText;
        }
    }

    private Assessment assessment = Assessment.NEUTRAL;
    private String shortDescription = "";
    private String detailedDescription = "";
    private String type = "";
    private String searchPhrase = "";

    public Finding() {
    }

    public Finding(Assessment assessment, String shortDescription) {
        this.assessment = assessment;
        this.shortDescription = shortDescription;
    }

    public static Finding newPositiveFinding(String description) {
        return new Finding(Assessment.POSITIVE, description);
    }

    public static Finding newFindingWithoutOpinion(String description) {
        return new Finding(Assessment.NONE, description);
    }

    public Finding detailedDescription(String details) {
        this.detailedDescription = details;
        return this;
    }

    public static Finding newNegativeFinding(String description) {
        return new Finding(Assessment.NEGATIVE, description);
    }

    public static Finding newNeutralFinding(String description) {
        return new Finding(Assessment.NEUTRAL, description);
    }

    public Assessment getAssessment() {
        return assessment;
    }

    public void setAssessment(Assessment assessment) {
        this.assessment = assessment;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getType() {
        return type;
    }

    public Finding setType(String type) {
        this.type = type;
        return this;
    }

    public String getSearchPhrase() {
        return searchPhrase;
    }

    public Finding setSearchPhrase(String searchPhrase) {
        this.searchPhrase = searchPhrase;
        return this;
    }

    public String getDetailedDescription() {
        return detailedDescription;
    }

    public void setDetailedDescription(String detailedDescription) {
        this.detailedDescription = detailedDescription;
    }

    @Override
    public String toString() {
        String typeString = StringUtils.isNotBlank(type) ? " (" + type + ")" : "";
        return assessment.getDisplayText() + typeString + " " + shortDescription;
    }
}
