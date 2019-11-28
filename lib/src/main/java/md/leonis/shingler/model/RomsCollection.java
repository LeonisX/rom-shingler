package md.leonis.shingler.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class RomsCollection {

    private String title;
    private String platform;
    private CollectionType type = CollectionType.PLAIN;
    private String romsPathString;
    private List<GID> gids;

    public RomsCollection() {
        gids = new ArrayList<>();
    }

    @JsonIgnore
    public Path getRomsPath() {
        return romsPathString == null ? null : Paths.get(romsPathString);
    }

    @JsonIgnore
    public void setGidsMap(Map<String, GID> gids) {
        this.gids = new ArrayList<>(gids.values());
    }

    @JsonIgnore
    public Map<String, GID> getGidsMap() {
        return gids.stream().collect(Collectors.toMap(GID::getTitle, Function.identity(), (e1, e2) -> e2, LinkedHashMap::new));
    }
}
