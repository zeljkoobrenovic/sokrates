package nl.obren.sokrates.sourcecode.search;

public class CleanedFoundText {
    private FoundText foundText;
    private String cleanedText;

    public CleanedFoundText(FoundText foundText, String cleanedText) {
        this.foundText = foundText;
        this.cleanedText = cleanedText;
    }

    public FoundText getFoundText() {
        return foundText;
    }

    public void setFoundText(FoundText foundText) {
        this.foundText = foundText;
    }

    public String getCleanedText() {
        return cleanedText;
    }

    public void setCleanedText(String cleanedText) {
        this.cleanedText = cleanedText;
    }
}
