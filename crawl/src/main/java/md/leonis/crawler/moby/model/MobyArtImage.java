package md.leonis.crawler.moby.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class MobyArtImage extends MobyImage {

    private Map<String, String> summary = new LinkedHashMap<>();

    public MobyArtImage(String id, String host, String small, String description) {
        super(id, host, small, description);
    }
}
