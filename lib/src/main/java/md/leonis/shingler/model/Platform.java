package md.leonis.shingler.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Platform {

    private String title;
    private String cpu;
    private String hackMatcher;
    private String badMatcher;
    private String pdMatcher;
    private List<String> exts;
    private List<Integer> restrictedShingles;
    private int shingleLevel;

    public boolean isHack(String name) {
        return name.matches(hackMatcher);
    }

    public boolean nonHack(String name) {
        return !isHack(name);
    }

    public boolean isBad(String name) {
        return name.matches(badMatcher);
    }

    public boolean isPD(String name) {
        return name.matches(pdMatcher);
    }

    public boolean nonPD(String name) {
        return !isPD(name);
    }

    public boolean isGood(String name) {
        return !(isBad(name) || isHack(name) || isPD(name));
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
