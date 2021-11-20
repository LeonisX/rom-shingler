package md.leonis.shingler.gui.crawler.moby;

import java.util.List;

public class PromoImage {

    private String id;
    private String name;
    private String original;
    private String typeName;
    private List<String> sourceDescr;

    public PromoImage(String id, String name, String typeName, List<String> sourceDescr) {
        this.id = id;
        this.name = name;
        this.typeName = typeName;
        this.sourceDescr = sourceDescr;
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

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public List<String> getSourceDescr() {
        return sourceDescr;
    }

    public void setSourceDescr(List<String> sourceDescr) {
        this.sourceDescr = sourceDescr;
    }

    public String getOriginal() {
        return original;
    }

    public void setOriginal(String original) {
        this.original = original;
    }
}
