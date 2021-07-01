package nl.obren.sokrates.sourcecode.landscape;

import java.util.ArrayList;
import java.util.List;

public class CustomTab {
    private String name = "More";
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
