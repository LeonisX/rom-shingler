package md.leonis.crawler.moby.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
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
}
