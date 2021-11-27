package md.leonis.shingler.gui.crawler.moby.model.credits;

// ()
public class PNode extends CreditsNode {

    private String text;

    public PNode(CreditsNode parent, String text) {
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
        String result = '(' + text;
        if (getNodes() != null && !getNodes().isEmpty()) {
            result = result + " " + super.toString();
        }
        return result + ')';
    }
}
