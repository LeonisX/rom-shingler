package md.leonis.shingler.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Platform {

    private String title;
    private String cpu;
    private String hackMatcher;
    private String badMatcher;
    private String pdMatcher;

    public boolean isHack(String name) {
        return name.matches(hackMatcher);
    }

    public boolean isBad(String name) {
        return name.matches(badMatcher);
    }

    public boolean isPD(String name) {
        return name.matches(pdMatcher);
    }

    public boolean isGood(String name) {
        return !(isBad(name) || isHack(name) || isPD(name));
    }
}