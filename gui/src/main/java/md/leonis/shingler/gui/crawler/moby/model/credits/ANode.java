package md.leonis.shingler.gui.crawler.moby.model.credits;

// <a href=...
public class ANode extends CreditsNode {

    private String id;
    private String name;

    public ANode(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        String result = id + "=" + name;
        if (getNodes() != null && !getNodes().isEmpty()) {
            result = result + " " + super.toString();
        }
        return result;
    }
}
