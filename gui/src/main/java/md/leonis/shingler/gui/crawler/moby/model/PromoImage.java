package md.leonis.shingler.gui.crawler.moby.model;

import java.util.List;

public class PromoImage {

    private String id;
    private String small;
    private String large;
    private String original;
    private String typeName;
    private List<String> sourceDescr;

    public PromoImage(String id, String small, String typeName, List<String> sourceDescr) {
        this.id = id;
        this.small = small;
        this.typeName = typeName;
        this.sourceDescr = sourceDescr;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSmall() {
        return small;
    }

    public void setSmall(String small) {
        this.small = small;
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

    public String getLarge() {
        return large;
    }

    public void setLarge(String large) {
        this.large = large;
    }
}
