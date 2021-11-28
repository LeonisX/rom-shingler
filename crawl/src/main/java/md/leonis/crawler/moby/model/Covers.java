package md.leonis.crawler.moby.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Covers {

    private Map<String, List<String>> props = new LinkedHashMap<>();
    private List<MobyArtImage> images = new ArrayList<>();

    public Covers() {
    }

    public Map<String, List<String>> getProps() {
        return props;
    }

    public void setProps(Map<String, List<String>> props) {
        this.props = props;
    }

    public List<MobyArtImage> getImages() {
        return images;
    }

    public void setImages(List<MobyArtImage> images) {
        this.images = images;
    }
}
