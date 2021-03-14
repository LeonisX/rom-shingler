package md.leonis.shingler.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tribe {

    private String id = UUID.randomUUID().toString();

    private String name;
    private Name father;
    private List<Family> families = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tribe tribe = (Tribe) o;
        return Objects.equals(id, tribe.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
