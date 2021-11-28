package md.leonis.crawler.moby.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class MobyArtImage extends MobyImage {

    private Map<String, String> summary = new LinkedHashMap<>();

    public MobyArtImage(String id, String small, String description) {
        super(id, small, description);
    }

    public Map<String, String> getSummary() {
        return summary;
    }

    public void setSummary(Map<String, String> summary) {
        this.summary = summary;
    }
}
