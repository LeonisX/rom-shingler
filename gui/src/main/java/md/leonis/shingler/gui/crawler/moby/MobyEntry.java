package md.leonis.shingler.gui.crawler.moby;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MobyEntry {

    private String platformId;
    private String gameId;

    private List<String> publishers = new ArrayList<>();
    private List<String> developers = new ArrayList<>();
    private List<String> releases = new ArrayList<>();
    private List<String> alsoFor = new ArrayList<>();

    private List<String> genres = new ArrayList<>();
    private List<String> perspectives = new ArrayList<>();
    private List<String> visuals = new ArrayList<>();
    private List<String> gameplays = new ArrayList<>();
    private List<String> settings = new ArrayList<>();

    private List<String> description = new ArrayList<>();
    private List<String> alternateTitles = new ArrayList<>();
    private List<String> gameGroup = new ArrayList<>();

    private Map<String, List<String>> trivia = new LinkedHashMap<>();

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

    public MobyEntry(String platformId, String gameId) {
        this.platformId = platformId;
        this.gameId = gameId;
    }

    public String platformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String gameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getPlatformId() {
        return platformId;
    }

    public String getGameId() {
        return gameId;
    }

    public boolean hasCredits() {
        return hasCredits;
    }

    public void setHasCredits(boolean hasCredits) {
        this.hasCredits = hasCredits;
    }

    public boolean hasScreenshots() {
        return hasScreenshots;
    }

    public void setHasScreenshots(boolean hasScreenshots) {
        this.hasScreenshots = hasScreenshots;
    }

    public boolean hasReviews() {
        return hasReviews;
    }

    public void setHasReviews(boolean hasReviews) {
        this.hasReviews = hasReviews;
    }

    public boolean hasCoverArt() {
        return hasCoverArt;
    }

    public void setHasCoverArt(boolean hasCoverArt) {
        this.hasCoverArt = hasCoverArt;
    }

    public boolean hasPromoArt() {
        return hasPromoArt;
    }

    public void setHasPromoArt(boolean hasPromoArt) {
        this.hasPromoArt = hasPromoArt;
    }

    public boolean hasReleases() {
        return hasReleases;
    }

    public void setHasReleases(boolean hasReleases) {
        this.hasReleases = hasReleases;
    }

    public boolean hasTrivia() {
        return hasTrivia;
    }

    public void setHasTrivia(boolean hasTrivia) {
        this.hasTrivia = hasTrivia;
    }

    public boolean hasHints() {
        return hasHints;
    }

    public void setHasHints(boolean hasHints) {
        this.hasHints = hasHints;
    }

    public boolean hasSpecs() {
        return hasSpecs;
    }

    public void setHasSpecs(boolean hasSpecs) {
        this.hasSpecs = hasSpecs;
    }

    public boolean hasAdBlurb() {
        return hasAdBlurb;
    }

    public void setHasAdBlurb(boolean hasAdBlurb) {
        this.hasAdBlurb = hasAdBlurb;
    }

    public boolean hasRatings() {
        return hasRatings;
    }

    public void setHasRatings(boolean hasRatings) {
        this.hasRatings = hasRatings;
    }

    public List<String> getPublishers() {
        return publishers;
    }

    public void setPublishers(List<String> publishers) {
        this.publishers = publishers;
    }

    public List<String> getDevelopers() {
        return developers;
    }

    public void setDevelopers(List<String> developers) {
        this.developers = developers;
    }

    public List<String> getReleases() {
        return releases;
    }

    public void setReleases(List<String> releases) {
        this.releases = releases;
    }

    public List<String> getAlsoFor() {
        return alsoFor;
    }

    public void setAlsoFor(List<String> alsoFor) {
        this.alsoFor = alsoFor;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public List<String> getPerspectives() {
        return perspectives;
    }

    public void setPerspectives(List<String> perspectives) {
        this.perspectives = perspectives;
    }

    public List<String> getVisuals() {
        return visuals;
    }

    public void setVisuals(List<String> visuals) {
        this.visuals = visuals;
    }

    public List<String> getGameplays() {
        return gameplays;
    }

    public void setGameplays(List<String> gameplays) {
        this.gameplays = gameplays;
    }

    public List<String> getSettings() {
        return settings;
    }

    public void setSettings(List<String> settings) {
        this.settings = settings;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public List<String> getAlternateTitles() {
        return alternateTitles;
    }

    public void setAlternateTitles(List<String> alternateTitles) {
        this.alternateTitles = alternateTitles;
    }

    public List<String> getGameGroup() {
        return gameGroup;
    }

    public void setGameGroup(List<String> gameGroup) {
        this.gameGroup = gameGroup;
    }

    public Map<String, List<String>> getTrivia() {
        return trivia;
    }

    public void setTrivia(Map<String, List<String>> trivia) {
        this.trivia = trivia;
    }
}
