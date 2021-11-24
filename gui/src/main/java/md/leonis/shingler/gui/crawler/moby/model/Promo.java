package md.leonis.shingler.gui.crawler.moby.model;

import java.util.ArrayList;
import java.util.List;

public class Promo {

    private String id;
    private String group;
    private String source;
    private List<PromoImage> images = new ArrayList<>();

    public Promo(String id, String group, String source) {
        this.id = id;
        this.group = group;
        this.source = source;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<PromoImage> getImages() {
        return images;
    }

    public void setImages(List<PromoImage> images) {
        this.images = images;
    }
}
