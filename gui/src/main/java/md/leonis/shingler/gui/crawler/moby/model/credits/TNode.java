package md.leonis.shingler.gui.crawler.moby.model.credits;

// Text
public class TNode extends CreditsNode {

    private String text;

    public TNode(String text) {
        this.text = text;
    }

    public TNode(CreditsNode parent, String text) {
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
        String result = text;
        if (getNodes() != null && !getNodes().isEmpty()) {
            result = result + " " + super.toString();
        }
        return result;
    }
}
