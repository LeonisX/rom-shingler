package md.leonis.crawler.moby.model;

import md.leonis.crawler.moby.model.credits.CreditsNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MobyEntry {

    private String platformId;
    private String gameId;

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

    public List<String> getDates() {
        return dates;
    }

    public void setDates(List<String> dates) {
        this.dates = dates;
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

    public Map<String, Map<String, List<CreditsNode>>> getCredits() {
        return credits;
    }

    public void setCredits(Map<String, Map<String, List<CreditsNode>>> credits) {
        this.credits = credits;
    }

    public List<MobyImage> getScreens() {
        return screens;
    }

    public void setScreens(List<MobyImage> screens) {
        this.screens = screens;
    }

    public List<UserReview> getUserReviews() {
        return userReviews;
    }

    public void setUserReviews(List<UserReview> userReviews) {
        this.userReviews = userReviews;
    }

    public Map<String, String> getNotes() {
        return notes;
    }

    public void setNotes(Map<String, String> notes) {
        this.notes = notes;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    public List<Covers> getCovers() {
        return covers;
    }

    public void setCovers(List<Covers> covers) {
        this.covers = covers;
    }

    public List<Promo> getPromos() {
        return promos;
    }

    public void setPromos(List<Promo> promos) {
        this.promos = promos;
    }

    public List<Map<String, String>> getReleases() {
        return releases;
    }

    public void setReleases(List<Map<String, String>> releases) {
        this.releases = releases;
    }

    public Map<String, Map<String, List<String>>> getHints() {
        return hints;
    }

    public void setHints(Map<String, Map<String, List<String>>> hints) {
        this.hints = hints;
    }

    public Map<String, List<String>> getSpecs() {
        return specs;
    }

    public void setSpecs(Map<String, List<String>> specs) {
        this.specs = specs;
    }

    public Map<String, String> getAdBlurbs() {
        return adBlurbs;
    }

    public void setAdBlurbs(Map<String, String> adBlurbs) {
        this.adBlurbs = adBlurbs;
    }

    public List<String> getPacings() {
        return pacings;
    }

    public void setPacings(List<String> pacings) {
        this.pacings = pacings;
    }

    public List<String> getEsbrRatings() {
        return esbrRatings;
    }

    public void setEsbrRatings(List<String> esbrRatings) {
        this.esbrRatings = esbrRatings;
    }

    public List<String> getMiscs() {
        return miscs;
    }

    public void setMiscs(List<String> miscs) {
        this.miscs = miscs;
    }

    public Map<String, List<String>> getRatingSystems() {
        return ratingSystems;
    }

    public void setRatingSystems(Map<String, List<String>> ratingSystems) {
        this.ratingSystems = ratingSystems;
    }

    public List<String> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(List<String> interfaces) {
        this.interfaces = interfaces;
    }

    public List<String> getSports() {
        return sports;
    }

    public void setSports(List<String> sports) {
        this.sports = sports;
    }

    public List<String> getArts() {
        return arts;
    }

    public void setArts(List<String> arts) {
        this.arts = arts;
    }

    public List<String> getNarratives() {
        return narratives;
    }

    public void setNarratives(List<String> narratives) {
        this.narratives = narratives;
    }

    public List<String> getVehiculars() {
        return vehiculars;
    }

    public void setVehiculars(List<String> vehiculars) {
        this.vehiculars = vehiculars;
    }

    public List<String> getEducationals() {
        return educationals;
    }

    public void setEducationals(List<String> educationals) {
        this.educationals = educationals;
    }

    public List<String> getOfficialSites() {
        return officialSites;
    }

    public void setOfficialSites(List<String> officialSites) {
        this.officialSites = officialSites;
    }

    public List<String> getSpecialEditions() {
        return specialEditions;
    }

    public void setSpecialEditions(List<String> specialEditions) {
        this.specialEditions = specialEditions;
    }

    public List<String> getAddons() {
        return addons;
    }

    public void setAddons(List<String> addons) {
        this.addons = addons;
    }
}
