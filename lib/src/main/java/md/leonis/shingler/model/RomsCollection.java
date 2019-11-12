package md.leonis.shingler.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class RomsCollection implements Serializable {

    private static final long serialVersionUID = 424258085469L;

    private String title;
    private String platform;
    private CollectionType type = CollectionType.PLAIN;
    private String romsPath;
    private List<GID> gids = new ArrayList<>();

    public void setGids(Map<String, GID> gids) {
        this.gids = new ArrayList<>(gids.values());
    }

    public Map<String, GID> getGids() {
        return gids.stream().collect(Collectors.toMap(GID::getTitle, Function.identity(), (e1, e2) -> e2, LinkedHashMap::new));
    }
}
