package md.leonis.crawler.moby.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class GameEntry {

    private String platformId;
    private String gameId;
    private String title;

    private List<String> publishers = new ArrayList<>();
    private List<String> developers = new ArrayList<>();
    private List<String> dates = new ArrayList<>();
    private List<String> officialSites = new ArrayList<>();
    private List<String> alsoFor = new ArrayList<>();

    private List<String> esbrRatings = new ArrayList<>();
    private List<String> genres = new ArrayList<>();
    private List<String> perspectives = new ArrayList<>();
    private List<String> visuals = new ArrayList<>();
    private List<String> pacings = new ArrayList<>();
    private List<String> arts = new ArrayList<>();
    private List<String> gameplays = new ArrayList<>();
    private List<String> educationals = new ArrayList<>();
    private List<String> interfaces = new ArrayList<>();
    private List<String> vehiculars = new ArrayList<>();
    private List<String> settings = new ArrayList<>();
    private List<String> sports = new ArrayList<>();
    private List<String> narratives = new ArrayList<>();
    private List<String> specialEditions = new ArrayList<>();
    private List<String> addons = new ArrayList<>();
    private List<String> miscs = new ArrayList<>();

    private List<String> description = new ArrayList<>();
    private List<String> alternateTitles = new ArrayList<>();
    private List<String> gameGroup = new ArrayList<>();

    private Map<String, Map<String, List<CreditsNode>>> credits = new LinkedHashMap<>();

    private List<MobyImage> screens = new ArrayList<>();

    private List<UserReview> userReviews = new ArrayList<>();
    private Map<String, String> notes = new LinkedHashMap<>();
    private List<Review> reviews = new ArrayList<>();

    private List<Covers> covers = new ArrayList<>();

    private List<Promo> promos = new ArrayList<>();

    List<Map<String, String>> releases = new ArrayList<>();

    private Map<String, List<String>> trivia = new LinkedHashMap<>();

    private Map<String, Map<String, List<String>>> hints = new LinkedHashMap<>();

    private Map<String, List<String>> specs = new LinkedHashMap<>();

    Map<String, String> adBlurbs = new LinkedHashMap<>();

    Map<String, List<String>> ratingSystems = new LinkedHashMap<>();

    private boolean hasCredits;
    private boolean hasScreenshots;
    private boolean hasReviews;
    private boolean hasCoverArt;
    private boolean hasPromoArt;
    private boolean hasReleases;
    private boolean hasTrivia;
    private boolean hasHints;
    private boolean hasSpecs;
    private boolean hasAdBlurb;
    private boolean hasRatings;

    private boolean completed;

    public GameEntry(String platformId, String gameId, String title) {
        this.platformId = platformId;
        this.gameId = gameId;
        this.title = title;
    }
}
