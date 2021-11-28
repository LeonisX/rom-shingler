package md.leonis.crawler.moby.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreditsNode {

    @JsonIgnore
    private CreditsNode parent;

    private String id;
    private String text;
    private String type;

    private List<CreditsNode> nodes;

    public CreditsNode(String type, CreditsNode parent, String text) {
        this.type = type;
        this.setParent(parent);
        this.text = text;
        parent.addNode(this);
    }

    public CreditsNode(String type, String id, String text) {
        this.id = id;
        this.type = type;
        this.text = text;
    }

    public CreditsNode(String type, String text) {
        this.type = type;
        this.text = text;
    }

    public void addNode(CreditsNode node) {
        if (nodes == null) {
            nodes = new ArrayList<>();
        }
        nodes.add(node);
    }


    @Override
    public String toString() {
        switch (type) {
            case "link": {
                return id + "=" + text + nodesString();
            }
            case "text": {
                return text + nodesString();
            }
            case "square": {
                return  '[' + text + nodesString() + ']';
            }
            case "round": {
                return  '(' + text + nodesString() + ')';
            }
            default:
                throw new RuntimeException(type);
        }
    }

    private String nodesString() {

        if (getNodes() != null && !getNodes().isEmpty()) {
            return  " " + nodes.stream().map(CreditsNode::toString).collect(Collectors.joining(" "));
        }
        return "";
    }
}
