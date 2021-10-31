package nl.obren.sokrates.common.renderingutils.force3d;

import java.util.ArrayList;
import java.util.List;

public class Force3DObject {
    private List<Force3DNode> nodes = new ArrayList<>();
    private List<Force3DLink> links = new ArrayList<>();

    public List<Force3DNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<Force3DNode> nodes) {
        this.nodes = nodes;
    }

    public List<Force3DLink> getLinks() {
        return links;
    }

    public void setLinks(List<Force3DLink> links) {
        this.links = links;
    }
}
