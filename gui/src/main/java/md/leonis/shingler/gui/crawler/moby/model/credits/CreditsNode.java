package md.leonis.shingler.gui.crawler.moby.model.credits;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CreditsNode {

    @JsonIgnore
    private CreditsNode parent;

    @JsonIgnore
    private List<CreditsNode> nodes;

    public void addNode(CreditsNode node) {
        if (nodes == null) {
            nodes = new ArrayList<>();
        }
        nodes.add(node);
    }

    public List<CreditsNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<CreditsNode> nodes) {
        this.nodes = nodes;
    }

    public CreditsNode getParent() {
        return parent;
    }

    public void setParent(CreditsNode parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        if (nodes == null) {
            return "";
        } else {
            return nodes.stream().map(CreditsNode::toString).collect(Collectors.joining(" "));
        }
    }
}
