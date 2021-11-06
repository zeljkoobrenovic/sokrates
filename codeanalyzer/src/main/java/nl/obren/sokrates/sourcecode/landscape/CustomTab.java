package nl.obren.sokrates.sourcecode.landscape;

import java.util.ArrayList;
import java.util.List;

public class CustomTab {
    // A tab name (label)
    private String name = "More";

    // A list of iFrames to be included in the tab
    private List<WebFrameLink> iFrames = new ArrayList<>();

    public CustomTab() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<WebFrameLink> getiFrames() {
        return iFrames;
    }

    public void setiFrames(List<WebFrameLink> iFrames) {
        this.iFrames = iFrames;
    }
}
