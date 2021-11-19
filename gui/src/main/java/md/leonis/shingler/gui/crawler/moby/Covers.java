package md.leonis.shingler.gui.crawler.moby;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Covers {

    private Map<String, List<String>> props = new LinkedHashMap<>();
    private Map<String, String> images = new LinkedHashMap<>();

    public Covers() {
    }

    public Map<String, List<String>> getProps() {
        return props;
    }

    public void setProps(Map<String, List<String>> props) {
        this.props = props;
    }

    public Map<String, String> getImages() {
        return images;
    }

    public void setImages(Map<String, String> images) {
        this.images = images;
    }
}
