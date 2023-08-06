package nl.obren.sokrates.reports.utils;

public class AnimalIcons {

    int size;

    public AnimalIcons(int size) {
        this.size = size;
    }

    public String getAnimalForMainLoc(int linesOfCode) {
        if (linesOfCode <= 1000) return "mouse";
        else if (linesOfCode <= 2000) return "bird";
        else if (linesOfCode <= 5000) return "cat";
        else if (linesOfCode <= 10000) return "dog";
        else if (linesOfCode <= 20000) return "sheep";
        else if (linesOfCode <= 50000) return "donkey";
        else if (linesOfCode <= 100000) return "horse";
        else if (linesOfCode <= 200000) return "hippo";
        else if (linesOfCode <= 500000) return "rhino";
        else if (linesOfCode <= 1000000) return "elephant";
        else return "whale";
    }
    public String getAnimalIconsForMainLoc(int linesOfCode) {
        if (linesOfCode <= 1000) return getIconSvg("mouse");
        else if (linesOfCode <= 2000) return getIconSvg("bird");
        else if (linesOfCode <= 5000) return getIconSvg("cat");
        else if (linesOfCode <= 10000) return getIconSvg("dog");
        else if (linesOfCode <= 20000) return getIconSvg("sheep");
        else if (linesOfCode <= 50000) return getIconSvg("donkey");
        else if (linesOfCode <= 100000) return getIconSvg("horse");
        else if (linesOfCode <= 200000) return getIconSvg("hippo");
        else if (linesOfCode <= 500000) return getIconSvg("rhino");
        else if (linesOfCode <= 1000000) return getIconSvg("elephant");
        else return getIconSvg("whale");
    }

    public String getInfo(int linesOfCode) {
        String animal = getAnimalForMainLoc(linesOfCode);

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(animal.toUpperCase() + "\n\n\n");
        stringBuilder.append("Animal icons graphically illustrate the size (lines of code), based on average weights of animals:\n\n\n");
        stringBuilder.append(" - mouse: &lt; 1000 LOC\n");
        stringBuilder.append(" - bird: 1,000 to 2000 LOC\n");
        stringBuilder.append(" - cat: 2,000 to 5,000 LOC\n");
        stringBuilder.append(" - dog: 5,000 to 10,000 LOC\n");
        stringBuilder.append(" - sheep: 10,000 to 20,000 LOC\n");
        stringBuilder.append(" - donkey: 20,000 to 50,000 LOC\n");
        stringBuilder.append(" - horse: 50,000 to 100,000 LOC\n");
        stringBuilder.append(" - hippo: 100,000 to 200,000 LOC\n");
        stringBuilder.append(" - rhino: 200,000 to 500,000 LOC\n");
        stringBuilder.append(" - elephant: 500,000 to 1,000,000 LOC\n");
        stringBuilder.append(" - whale: &gt; 1,000,000 LOC\n");

        return stringBuilder.toString();
    }

    private String getIconSvg(String icon) {
        String svg = HtmlTemplateUtils.getResource("/icons/" + icon + ".svg");
        svg = svg.replaceAll("height='.*?'", "height='" + size + "px'");
        svg = svg.replaceAll("width='.*?'", "width='" + size + "px'");
        return svg;
    }

}
