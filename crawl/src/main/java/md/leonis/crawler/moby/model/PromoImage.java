package md.leonis.crawler.moby.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
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
}
