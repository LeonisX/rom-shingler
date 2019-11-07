package md.leonis.shingler.gui.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;

//TODO far future - keep in DB as language settings
@Getter
@AllArgsConstructor
public enum LanguageLevel {

    A0("Beginner (< A1)", 0, 426),
    A1("Beginner (A1)", 500, 1000),
    A2("Elementary (A2)", 1000, 1664),
    A2P("Elementary (A2+)", 1500, 2403),
    B1("Pre-Intermediate (B1)", 2000, 3719),
    B1P("Pre-Intermediate (B1+)", 3000, 6826),
    B2("Intermediate (B2)", 4000, 11571),
    B2P("Intermediate (B2+)", 6000, 20294),
    C1("Upper-Intermediate (C1)", 8000, 33279),
    C2("Advanced (C2)", 16000, 34911),
    C2P("Near Native (C2+)", 32000, 200_000), // Who is bigger?

    UNK("Unknown :(", -1, -1);

    private String title;
    private long minCount;
    private long maxCount;

    @SuppressWarnings("all")
    public static String getTitle(long minCount) {
        return Arrays.stream(LanguageLevel.values())
                .filter(l -> l.minCount <= minCount)
                .max(Comparator.comparing(LanguageLevel::getMinCount)).get()
                .getTitle();
    }
}
