package md.leonis.shingler.gui.crawler.moby.model;

import md.leonis.shingler.gui.crawler.moby.model.credits.CreditsNode;

// []
public class SNode extends CreditsNode {

    private String text;

    public SNode(CreditsNode parent, String text) {
        this.setParent(parent);
        this.text = text;
        parent.addNode(this);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        String result = '[' + text;
        if (getNodes() != null && !getNodes().isEmpty()) {
            result = result + " " + super.toString();
        }
        return result + ']';
    }
}
