package nl.obren.sokrates.reports.utils;

import java.util.Arrays;
import java.util.List;

public class AnimalIcons {

    int size;

    public AnimalIcons(int size) {
        this.size = size;
    }

    public List<String> getAnimals() {
        return Arrays.asList(new String[]{"mouse", "bird", "cat", "dog", "sheep", "donkey", "horse", "hippo", "rhino", "elephant", "whale"});
    }
    public List<String> getAnimalsLOCInfo() {
        return Arrays.asList(new String[]{"&lt;1K", "1-2K", "2-5K", "5-10K", "10-20K", "20-50K", "50-100K", "100-200K", "200-500K", "500K-1M", "&gt;1M"});
    }

    private static final int[] LOC_THRESHOLDS = {1000, 2000, 5000, 10000, 20000, 50000, 100000, 200000, 500000, 1000000};
    private static final String[] ANIMALS = {"mouse", "bird", "cat", "dog", "sheep", "donkey", "horse", "hippo", "rhino", "elephant", "whale"};

    /** The animal whose size class the main lines of code fall in: a mouse up to 1,000 lines ... a whale above a million. */
    public String getAnimalForMainLoc(int linesOfCode) {
        for (int i = 0; i < LOC_THRESHOLDS.length; i++) {
            if (linesOfCode <= LOC_THRESHOLDS[i]) {
                return ANIMALS[i];
            }
        }
        return ANIMALS[ANIMALS.length - 1];
    }

    public String getAnimalIconsForMainLoc(int linesOfCode) {
        return getIconSvg(getAnimalForMainLoc(linesOfCode));
    }

    public String getInfo(int linesOfCode) {
        String animal = getAnimalForMainLoc(linesOfCode);

        return getInfoForAnimal(animal);
    }

    public static String getInfoForAnimal(String animal) {
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

    public String getIconSvg(String icon) {
        String svg = HtmlTemplateUtils.getResource("/icons/" + icon + ".svg");
        svg = svg.replaceAll("height='.*?'", "height='" + size + "px'");
        svg = svg.replaceAll("width='.*?'", "width='" + size + "px'");
        return svg;
    }

}
