package md.leonis.shingler.gui.crawler.moby;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class MobyCrawler {

    public static final String PLATFORMS = "https://www.mobygames.com/browse/games/full,1/";
    public static final String GAMES_PAGES = "https://www.mobygames.com/browse/games/%s/offset,%s/so,0a/list-games/";
    public static final String GAME_MAIN = "https://www.mobygames.com/game/%s/%s";
    public static final String GAME_CREDITS = "https://www.mobygames.com/game/%s/%s/credits";
    public static final String GAME_SCREENSHOTS = "https://www.mobygames.com/game/%s/%s/screenshots";
    // About mobyrank: https://www.mobygames.com/info/mobyrank?nof=1
    //TODO тут ссылки на некоторые источники сдохли.
    //Некоторые совсем, некоторые переехали.
    //Нужно уметь мэппить
    public static final String GAME_REVIEWS = "https://www.mobygames.com/game/%s/%s/mobyrank";
    public static final String GAME_COVER_ART = "https://www.mobygames.com/game/%s/%s/cover-art";
    public static final String GAME_PROMO = "https://www.mobygames.com/game/%s/%s/promo";
    public static final String GAME_RELEASES = "https://www.mobygames.com/game/%s/%s/release-info";
    public static final String GAME_TRIVIA = "https://www.mobygames.com/game/%s/%s/trivia";
    public static final String GAME_HINTS = "https://www.mobygames.com/game/%s/%s/hints";
    public static final String GAME_TECH_INFO = "https://www.mobygames.com/game/%s/%s/techinfo";
    public static final String GAME_ADS = "https://www.mobygames.com/game/%s/%s/adblurbs";
    public static final String GAME_RATING_SYSTEMS = "https://www.mobygames.com/game/%s/%s/rating-systems";
    public static final String GAME_BUY_TRADE = "https://www.mobygames.com/game/%s/%s/buy-trade";

    //TODO sources (журналы, сайты, откуда брались обзоры и ревью.
    // ><a href="https://www.mobygames.com/mobyrank/source/sourceId,998/">Tilt </a> (Dec, 1987)</div>
    // там список игр, которые оценивал источник с оценкой и датой


    //TODO proxies list
    public static final HttpExecutor executor = HttpExecutor.directInstance();

    public static Map<String, String> platforms = null;
    public static Map<String, String> games = null;
    public static Map<String, String> companies = new HashMap<>();
    public static Map<String, String> sheets = new HashMap<>();
    public static Map<String, String> gameGroups = new HashMap<>();
    public static Map<String, String> developers = new HashMap<>();
    public static Map<String, String> sources = new HashMap<>();
    public static Map<String, String> users = new HashMap<>();

    public static void main(String[] args) throws Exception {

        /*//TODO get list of systems
        Map<String, String> platforms = parseSystemsList();
        System.out.println(platforms);

        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File("platforms.json"), platforms);

        //TODO in cycle
        Map<String, String> games = parseGamesList("nes");
        System.out.println(games);

        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File("games-nes.json"), games);*/

        platforms = new ObjectMapper().readValue(new File("platforms.json"), new TypeReference<Map<String, String>>() {
        });
        games = new ObjectMapper().readValue(new File("games-nes.json"), new TypeReference<Map<String, String>>() {
        });
        try {
            companies = new ObjectMapper().readValue(new File("companies.json"), new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {

        }
        try {
            sheets = new ObjectMapper().readValue(new File("sheets.json"), new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {

        }
        try {
            gameGroups = new ObjectMapper().readValue(new File("gameGroups.json"), new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {

        }
        try {
            developers = new ObjectMapper().readValue(new File("developers.json"), new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {

        }
        try {
            sources = new ObjectMapper().readValue(new File("sources.json"), new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {

        }
        try {
            users = new ObjectMapper().readValue(new File("users.json"), new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {

        }

        //parseGameMain("nes", "1942arcade");
        //parseGameMain("nes", "super-mario-bros-2");

        //parseGameCredits("nes", "super-mario-bros-2");
        //System.out.println("------------------------");
        //parseGameCredits("nes", "1942arcade");
        //System.out.println("------------------------");
        //parseGameCredits("fm-7", "1942arcade");

        //TODO только если активны (есть)
        //parseGameScreenshots("nes", "super-mario-bros-2");

        //TODO только если активны (есть)
        //parseGameReviews("nes", "1942arcade");

        //TODO только если активны (есть)
        //parseGameCoverArt("nes", "1942arcade");

        MobyEntry mobyEntry = new MobyEntry("nes", "1942arcade");

        //parseGameMain(mobyEntry);
        //parseGameCredits(mobyEntry);
        //parseGameScreenshots(mobyEntry);
        //parseGameReviews(mobyEntry);
        parseGameCoverArt(mobyEntry);

        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File("companies.json"), companies);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File("sheets.json"), sheets);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File("gameGroups.json"), gameGroups);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File("developers.json"), developers);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File("sources.json"), sources);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File("users.json"), users);
    }

    private static void parseGameMain(MobyEntry entry) throws Exception {

        HttpExecutor.HttpResponse response = executor.getResponse(String.format(GAME_MAIN, entry.platformId(), entry.gameId()));

        Element container = getContainer(response);
        // 1942 (NES)
        // Main  Credits  Screenshots  Reviews  Cover Art  Promo Art  Releases  Trivia  Hints  Specs  Ad Blurb  Rating Systems  Buy/Trade
        Element rightPanelHeader = getByClass(container, "rightPanelHeader");
        Element niceHeaderTitle = getByClass(rightPanelHeader, "niceHeaderTitle");
        List<A> niceHeaderTitleA = getAs(niceHeaderTitle);

        // Здесь так же можно получить полное название игры и платформы
        String gameName = getLastChunk(niceHeaderTitleA.get(0).text());
        String platformName = getLastChunk(niceHeaderTitleA.get(1).text());

        assert gameName.equals(games.get(entry.gameId()));
        assert platformName.equals(games.get(entry.platformId()));

        // Main  Credits  Screenshots  Reviews  Cover Art  Promo Art  Releases  Trivia  Hints  Specs  Ad Blurb  Rating Systems  Buy/Trade
        Elements lis = selectUl(rightPanelHeader, "ul.nav-tabs");
        assert lis.size() == 14;

        assert getA(lis.get(0)).text().equals("Main");      // https://www.mobygames.com/game/%s/%s
        assert getA(lis.get(1)).text().equals("Credits");   //https://www.mobygames.com/game/%s/%s/credits
        assert getA(lis.get(2)).text().equals("Screenshots"); //https://www.mobygames.com/game/%s/%s/screenshots
        assert getA(lis.get(3)).text().equals("Reviews");   //https://www.mobygames.com/game/%s/%s/mobyrank
        assert getA(lis.get(4)).text().equals("Cover Art"); //https://www.mobygames.com/game/%s/%s/cover-art
        assert getA(lis.get(5)).text().equals("Promo Art"); //https://www.mobygames.com/game/%s/%s/promo
        assert getA(lis.get(6)).text().equals("Releases");  //https://www.mobygames.com/game/%s/%s/release-info
        assert getA(lis.get(7)).text().equals("Trivia");    //https://www.mobygames.com/game/%s/%s/trivia
        assert getA(lis.get(8)).text().equals("Hints");     //https://www.mobygames.com/game/%s/%s/hints
        assert getA(lis.get(9)).text().equals("Specs");     //https://www.mobygames.com/game/%s/%s/techinfo
        assert getA(lis.get(10)).text().equals("Ad Blurb"); //https://www.mobygames.com/game/%s/%s/adblurbs
        assert getA(lis.get(11)).text().equals("Rating Systems");//https://www.mobygames.com/game/%s/%s/rating-systems
        assert lis.get(12).text().isEmpty();
        assert getA(lis.get(13)).text().equals("Buy/Trade");//https://www.mobygames.com/game/%s/%s/buy-trade

        entry.setHasCredits(!lis.get(1).hasClass("disabled"));
        entry.setHasScreenshots(!lis.get(2).hasClass("disabled"));
        entry.setHasReviews(!lis.get(3).hasClass("disabled"));
        entry.setHasCoverArt(!lis.get(4).hasClass("disabled"));
        entry.setHasPromoArt(!lis.get(5).hasClass("disabled"));
        entry.setHasReleases(!lis.get(6).hasClass("disabled"));
        entry.setHasTrivia(!lis.get(7).hasClass("disabled"));
        entry.setHasHints(!lis.get(8).hasClass("disabled"));
        entry.setHasSpecs(!lis.get(9).hasClass("disabled"));
        entry.setHasAdBlurb(!lis.get(10).hasClass("disabled"));
        entry.setHasRatings(!lis.get(11).hasClass("disabled"));

        Element divColMd8 = getByClass(container, "col-md-8 col-lg-8");
        // Все метаданные по игре + обложка
        Element coreGameInfo = getById(divColMd8, "floatholder coreGameInfo");
        // Обложка + кол-во картинок
        Element coreGameCover = getById(divColMd8, "coreGameCover");
        // Не хочу это парсить, поскольку всё то же самое есть на странице Cover Art.

        // Second column
        Element coreGameRelease = getById(coreGameInfo, "coreGameRelease");

        Elements divs = getDivs(coreGameRelease);
        assert divs.size() == 8;
        assert divs.get(1).text().equals("Published by");
        assert divs.get(3).text().equals("Developed by");
        assert divs.get(5).text().equals("Released");
        assert divs.get(9).text().equals("Also For");

        //TODO read all a's, parse
        // <a href="https://www.mobygames.com/company/capcom-usa-inc">Capcom&nbsp;U.S.A.,&nbsp;Inc.</a>
        getAs(divs.get(2)).forEach(a -> {
            String chunk = getLastChunk(a);
            entry.getPublishers().add(chunk);
            companies.put(chunk, a.text());
        });
        // <a href="https://www.mobygames.com/company/capcom-co-ltd">Capcom&nbsp;Co.,&nbsp;Ltd.</a>
        getAs(divs.get(4)).forEach(a -> {
            String chunk = getLastChunk(a);
            entry.getDevelopers().add(chunk);
            companies.put(chunk, a.text());
        });
        //TODO may be use Releases tab instead this info
        // <a href="https://www.mobygames.com/game/%s/%s/release-info">Nov, 1986</a>
        getAs(divs.get(6)).forEach(a -> entry.getReleases().add(a.text()));
        // <a href="https://www.mobygames.com/game/cpc/1942arcade">Amstrad CPC</a>
        // ,
        // <a href="https://www.mobygames.com/game/android/1942arcade">Android</a>
        // ,
        // ...
        // |
        // <a href="https://www.mobygames.com/game/1942arcade">Combined&nbsp;View</a>
        List<A> alsoFor = getAs(divs.get(8));
        alsoFor.subList(0, alsoFor.size() - 1).forEach(a -> entry.getAlsoFor().add(getPreLastChunk(a.href())));

        // Third column
        Element coreGameGenre = getById(coreGameInfo, "coreGameGenre");
        divs = getDivs(coreGameGenre);
        assert divs.size() == 10;
        assert divs.get(2).text().equals("Genre");
        assert divs.get(4).text().equals("Perspective");
        assert divs.get(6).text().equals("Visual");
        assert divs.get(8).text().equals("Gameplay");
        assert divs.get(10).text().equals("Setting");

        //TODO read all a's, parse
        // <a href="https://www.mobygames.com/genre/sheet/action/">Action</a>
        getAs(divs.get(3)).forEach(a -> {
            String sheet = getLastChunk(a);
            entry.getGenres().add(sheet);
            sheets.put(sheet, a.text());
        });
        //TODO replace in all string fields &nbsp; with spaces!!!!
        // <a href="https://www.mobygames.com/genre/sheet/top-down/">Top-down</a>
        getAs(divs.get(5)).forEach(a -> {
            String sheet = getLastChunk(a);
            entry.getPerspectives().add(sheet);
            sheets.put(sheet, a.text());
        });
        // <a href="https://www.mobygames.com/genre/sheet/2d-scrolling/">2D&nbsp;scrolling</a>
        getAs(divs.get(7)).forEach(a -> {
            String sheet = getLastChunk(a);
            entry.getVisuals().add(sheet);
            sheets.put(sheet, a.text());
        });
        // <a href="https://www.mobygames.com/genre/sheet/arcade_/">Arcade</a>,
        // <a href="https://www.mobygames.com/genre/sheet/shooter/">Shooter</a>
        getAs(divs.get(9)).forEach(a -> {
            String sheet = getLastChunk(a);
            entry.getGameplays().add(sheet);
            sheets.put(sheet, a.text());
        });
        // <a href="https://www.mobygames.com/genre/sheet/world-war-ii/">World&nbsp;War&nbsp;II</a>
        getAs(divs.get(11)).forEach(a -> {
            String sheet = getLastChunk(a);
            entry.getSettings().add(sheet);
            sheets.put(sheet, a.text());
        });

        // Description
        Element h2description = select(divColMd8, "h2:contains(Description)");

        // Читать ноды, пока не встретится класс sideBarLinks
        List<String> description = new ArrayList<>();
        Node node = h2description;

        do {
            node = getNextNode(node);
            if (node instanceof TextNode) {
                description.add(((TextNode) node).text());
            } else {
                Element element = (Element) node;
                assert element != null;
                if (element.className().equals("sideBarLinks")) {
                    break;
                }
                description.add(element.outerHtml());
            }
        } while (true);

        entry.setDescription(description);

        // Skip screenshots, promo images, trailer

        // Alternate Titles
        lis = selectNextUl(divColMd8, "h2:contains(Alternate Titles)");
        lis.forEach(li -> entry.getAlternateTitles().add(((TextNode) li.childNode(0)).text() + ((Element) li.childNode(1)).text()));


        // Part of the Following Groups
        lis = selectNextUl(divColMd8, "h2:contains(Part of the Following Groups)");
        lis.forEach(li -> {
            A a = getA(li.child(0));
            // https://www.mobygames.com/game-group/mario-games
            gameGroups.put(getLastChunk(a), a.text());
            entry.getGameGroup().add(getLastChunk(a));
        });

        // Trivia
        Node el = select(divColMd8, "h2:contains(Trivia)");
        String key = null;
        List<String> values = new ArrayList<>();
        while (true) {
            el = getNextNode(el);
            assert el != null;
            if (el instanceof TextNode) {
                if (!((TextNode) el).text().trim().isEmpty()) {
                    values.add(((TextNode) el).text());
                }
            } else {
                Element ell = (Element) el;
                if (ell.tagName().equals("h3")) {
                    if (key != null) {
                        entry.getTrivia().put(key, values);
                    }
                    key = ell.text();
                    values = new ArrayList<>();
                } else if (ell.tagName().equals("small")) {
                    break;
                } else if (ell.className().equals("sideBarLinks")) {
                    break;
                } else {
                    values.add(ell.outerHtml());
                }
            }
        }
        entry.getTrivia().put(key, values);
    }

    private static void parseGameCredits(MobyEntry entry) throws Exception {

        HttpExecutor.HttpResponse response = executor.getResponse(String.format(GAME_CREDITS, entry.platformId(), entry.gameId()));
        Element container = getContainer(response);
        Element divColMd8 = getByClass(container, "col-md-8");
        // Все метаданные по игре + обложка
        Element coreGameInfo = getById(divColMd8, "floatholder coreGameInfo");
        //Title or BR if no credits
        Element h2Title = getNext(coreGameInfo);

        if (h2Title.tagName().equals("h2")) {
            assert h2Title.text().equals(games.get(entry.gameId()) + " Credits");
            //Div со всеми кредитами
            Element div = getNext(h2Title);
            // X people
            assert ((TextNode) getNextNode(div)).text().endsWith(" people");
            // Table
            Element table = getTable(div);
            assert table.attr("summary").equals("List of Credits");

            Elements trs = getTrs(table);
            assert getH2(trs.get(0)).text().equals("Credits");

            trs.forEach(tr -> {
                Elements tds = getTds(tr);
                if (tds.size() == 2) {
                    assert tr.hasClass("crln");

                    String role = tds.get(0).text();
                    List<A> as = getAs(tds.get(1));
                    assert as.size() >= 1;

                    List<String> ids = as.stream().map(a -> {
                        // <a href="https://www.mobygames.com/developer/sheet/view/developerId,45166/">Ayako Mori</a>
                        String developerId = getLastChunk(a).split(",")[1];
                        developers.put(developerId, a.text());
                        return developerId;
                    }).collect(Collectors.toList());
                    entry.getCredits().put(role, ids);
                }
            });
        } else if (h2Title.tagName().equals("br")) {
            Element p = getNext(h2Title);
            assert p.tagName().equals("p");
            assert p.text().startsWith("here are no credits for the");
            System.out.println(p.text());
        } else {
            throw new RuntimeException(h2Title.outerHtml());
        }
    }

    private static void parseGameScreenshots(MobyEntry entry) throws Exception {

        HttpExecutor.HttpResponse response = executor.getResponse(String.format(GAME_SCREENSHOTS, entry.platformId(), entry.gameId()));
        Element container = getContainer(response);
        Element divColMd12 = getByClass(container, "col-md-12");
        // Проверим что система верна
        // <ul class="nav nav-pills" style="margin-bottom: 15px;"><li><a href="https://www.mobygames.com/game/1942arcade/screenshots">All</a></li> <li class="active"><a href="https://www.mobygames.com/game/nes/1942arcade/screenshots">NES</a></li>
        Element ulNav = select(divColMd12, "ul.nav-pills");
        Element li = select(ulNav, "li.active");
        A at = getA(li);
        assert at.text().equals(games.get(entry.gameId()));

        Element h2 = getNext(ulNav);
        assert h2.text().equals("User Screenshots");
        Element h3 = getNext(h2);
        assert h3.text().equals(games.get(entry.gameId()) + " version");
        Element divRow = getNext(h3);
        assert divRow.tagName().equals("div");

        // <div class="thumbnail-image-wrapper">
        // <a href="https://www.mobygames.com/game/nes/1942arcade/screenshots/gameShotId,34513/"
        // title="1942 NES Title screen" class="thumbnail-image"
        // style="background-image:url(/images/shots/s/34513-1942-nes-screenshot-title-screen.jpg);"></a>
        // </div>
        Elements divs = getAllByClass(divRow, "thumbnail");
        divs.forEach(div -> {
            Element divI = select(div, "div.thumbnail-image-wrapper");
            A a = getA(divI);
            // https://www.mobygames.com/game/nes/1942arcade/screenshots/gameShotId,34513/
            // https://www.mobygames.com/images/shots/l/34513-1942-nes-screenshot-title-screen.jpg
            //System.out.println(a.attr("href"));
            String style = a.attr("style");
            style = style.substring(0, style.length() - 2);
            style = style.replace("background-image:url(", "");
            style = "https://www.mobygames.com" + style;
            //System.out.println(style);
            //System.out.println(style.replace("/images/shots/s/", "/images/shots/l/"));

            String image = getLastChunk(style);

            Element divS = select(div, "div.thumbnail-caption");
            Element small = getByTag(divS, "small");
            String description = small.text();
            entry.getScreens().put(image, description);
        });
    }

    private static void parseGameReviews(MobyEntry entry) throws Exception {

        HttpExecutor.HttpResponse response = executor.getResponse(String.format(GAME_REVIEWS, entry.platformId(), entry.gameId()));
        Element container = getContainer(response);
        Element divColMd8 = getByClass(container, "col-md-8");
        // Все метаданные по игре + обложка
        Element coreGameInfo = getById(divColMd8, "floatholder coreGameInfo");
        //Title or BR if no credits
        Element h2Title = getNext(coreGameInfo);
        assert h2Title.text().equals("User Reviews");
        // Table либо нет обзоров
        Element table = getNext(h2Title);
        if (table.tagName().equals("table")) {
            Elements trs = getTrs(table);
            trs.forEach(tr -> {
                Elements tds = getTds(tr);
                A a = getA(tds.get(0));
                // https://www.mobygames.com/game/nes/1942arcade/reviews/reviewerId,1717/
                //String reviewLink = a.href();
                String summary = a.text();
                a = getA(tds.get(1));
                // https://www.mobygames.com/user/sheet/userSheetId,1717/
                String reviewerLink = a.href();
                String reviewerId = getLastChunk(reviewerLink).split(",")[1];
                String reviewer = a.text();
                String note = getByTag(tds.get(2), "img").attr("alt").replace(" Stars", "");

                users.put(reviewerId, reviewer);
                entry.getUserReviews().add(new UserReview(note, reviewerId, summary));
            });
        } else {
            assert table.text().startsWith("There are no reviews for the ");
        }

        Element next = getNext(table);
        assert next.className().equals("sideBarLinks");
        next = getNext(next);
        assert next.tagName().equals("br");

        h2Title = getNext(next);
        assert h2Title.text().equals("Our Users Say");

        // Table либо нет ревью
        Element divFloatholder = getNext(h2Title);
        Element table2 = getTable(divFloatholder);
        if (table2.tagName().equals("table")) {
            Elements trs = getTrs(table2);
            trs.subList(1, trs.size() - 1).forEach(tr -> {
                Elements tds = getTds(tr);
                String category = tds.get(0).text();
                //String description = tds.get(1).text(); // How well the game mechanics work and the game plays., ...
                String userScore = tds.get(2).text();
                entry.getNotes().put(category, userScore);
            });
            Elements ths = getThs(table2);
            String overallUserVotes = ths.get(3).text(); // Overall User Score (52 votes)
            String overallUserScore = ths.get(4).text();
            entry.getNotes().put(overallUserVotes, overallUserScore);

        } else {
            assert table.text().startsWith("There are no reviews for the ");
        }

        next = getNext(table);
        assert next.className().equals("sideBarLinks");
        next = getNext(next);
        assert next.tagName().equals("br");
        next = getNext(next);
        assert next.tagName().equals("br");

        //critic reviews
        h2Title = getNext(next);
        assert h2Title.text().equals("Critic Reviews");

        // Table либо нет ревью
        Elements divRanks = getAllByClass(divColMd8, "floatholder mobyrank scoresource");
        // <div class="floatholder mobyrank scoresource">
        //  <div class="fl scoreBoxMed scoreHi">80</div>
        //  <div class="source scoreBorderHi"><a href="https://www.mobygames.com/mobyrank/source/sourceId,998/">Tilt </a> (Dec, 1987)</div>
        //  <div class="citation">En conclusion, 1942 est, à tous points de vue, un très bon jeu sur console.</div>
        //  <div class="url"><a target="_blank" href="http://download.abandonware.org/magazines/Tilt/tilt_numero049/TILT%20-%20n%B049%20-%20decembre%201987%20-%20page100%20et%20page101.jpg">read review</a></div>
        //</div>
        divRanks.forEach(divRank -> {
            Elements divs = getDivs(divRank);
            int score = Integer.parseInt(divs.get(1).text());
            TextNode textNode = (TextNode) divs.get(2).childNode(1);
            A a = getA(divs.get(2));
            String source = a.text().trim();
            // https://www.mobygames.com/mobyrank/source/sourceId,998/
            String sourceId = getLastChunk(a).split(",")[1];
            String date = textNode.text().trim(); // todo parse
            String citation = divs.get(3).text();

            sources.put(sourceId, source);
            Review review = new Review(score, sourceId, date.substring(1, date.length() - 1), citation);
            entry.getReviews().add(review);

            if (divs.size() == 5) {
                review.setSourceUrl(getA(divs.get(4)).href()); //TODO download/parse/map broken links or read from archive
            }
        });
    }

    private static void parseGameCoverArt(MobyEntry entry) throws Exception {

        HttpExecutor.HttpResponse response = executor.getResponse(String.format(GAME_COVER_ART, entry.platformId(), entry.gameId()));
        Element container = getContainer(response);

        // в цикле читать дивы:
        // coverHeading
        // row
        // sideBarLinks
        Element current = getByClass(container, "nav nav-pills");
        Covers covers = new Covers();

        do {
            current = getNext(current);
            if (!current.className().equals("coverHeading")) {
                break;
            }
            Element h2 = current.child(0);
            assert h2.tagName().equals("h2");
            assert h2.text().equals(platforms.get(entry.platformId()));
            Elements trs = getTrs(current);
            // Эти параметры на самом деле разные, надо собрать список и мэппить соответственно.
            for (Element tr : trs) {
                Elements tds = getTds(tr);
                assert tds.size() == 3;
                String property = tds.get(0).text();
                assert tds.get(1).text().equals(" : ");
                Elements spans = getAllByTag(tds.get(2), "span");
                List<String> values = spans.stream()
                        .map(span -> ((TextNode) span.childNode(0)).text()).collect(Collectors.toList());
                //System.out.println(property + ": " + String.join(", ", values));
                covers.getProps().put(property, values);
            }

            current = getNext(current);
            assert current.className().equals("row");
            Elements divs = getAllByClass(current, "col-xs-6 col-sm-3 col-md-2");
            for (Element div : divs) {
                Element a = select(div, "a.thumbnail-cover");
                // https://www.mobygames.com/images/covers/s/17048-1942-nes-front-cover.jpg
                // https://www.mobygames.com/images/covers/l/17048-1942-nes-front-cover.jpg
                String style = a.attr("style");
                style = style.substring(0, style.length() - 2); // ");:
                style = style.replace("background-image:url(", "");
                String imageId = getLastChunk(style);
                //style = "https://www.mobygames.com" + style;
                //System.out.println(style);
                //System.out.println(style.replace("/images/covers/s/", "/images/covers/l/"));

                Element divS = select(div, "div.thumbnail-cover-caption");
                Element p = getByTag(divS, "p");
                //System.out.println(p.text());
                covers.getImages().put(imageId, p.text());
            }

            entry.getCovers().add(covers);
            covers = new Covers();
            current = getNext(current);
            assert current.className().equals("sideBarLinks");
        } while (true);
    }

    private static Map<String, String> parseGamesListToc(String systemId) throws Exception {

        Map<String, String> games = new LinkedHashMap<>();

        HttpExecutor.HttpResponse response = executor.getResponse(String.format(GAMES_PAGES, systemId, 0));
        Document doc = Jsoup.parse(response.getBody());

        // <td class="mobHeaderPage" width="33%">Viewing Page 1 of 56</td>
        String[] chunks = getByClass(doc, "mobHeaderPage").text().split(" ");
        int pagesCount = Integer.parseInt(chunks[chunks.length - 1]);
        assert pagesCount > 0;

        System.out.println("Pages count: " + pagesCount);

        // <td class="mobHeaderItems" width="34%">(items 1-25 of 1397)</td>
        chunks = getByClass(doc, "mobHeaderItems").text().split(" ");
        String last = chunks[chunks.length - 1];
        int gamesCount = Integer.parseInt(last.substring(0, last.length() - 1));
        assert gamesCount > 0;

        System.out.println("Games count: " + gamesCount);

        /*Objects.requireNonNull(doc.getElementsByClass("browseTable").first()).getElementsByTag("a").forEach(a -> {
            games.put(parseSystemUrl(a.attr("href")), a.text());
        });*/

        //TODO parse #0 page
        parseGamesList(games, response.getBody());

        //TODO parse in cycle
        // 1 - 0
        // 2 - 25
        // 3 - 50
        for (int i = 1; i < pagesCount; i++) {
            response = executor.getResponse(String.format(GAMES_PAGES, systemId, i * 25));
            parseGamesList(games, response.getBody());
        }

        assert games.size() == gamesCount;

        return games;
    }

    private static void parseGamesList(Map<String, String> games, String html) {

        Document doc = Jsoup.parse(html);

        // <td class="mobHeaderPage" width="33%">Viewing Page 1 of 56</td>
        Element table = getByTag(getById(doc, "mof_object_list"), "tbody");
        getTrs(table).forEach(tr -> {
            // Game Title	        Year	Publisher	        Genre
            // Bad Street Brawler	1989	Mattel Electronics	Action
            List<Element> as = getTds(tr).stream().map(MobyCrawler::getA).collect(Collectors.toList());
            games.put(getLastChunk(as.get(0).attr("href")), as.get(0).text());
        });
    }

    private static Map<String, String> parseSystemsList() throws Exception {

        Map<String, String> systems = new LinkedHashMap<>();

        HttpExecutor.HttpResponse response = executor.getResponse(PLATFORMS);
        Document doc = Jsoup.parse(response.getBody());
        getAs(getByClass(doc, "browseTable")).forEach(a -> systems.put(getLastChunk(a), a.text()));

        return systems;
    }


    private static Element getContainer(HttpExecutor.HttpResponse response) {

        Document doc = Jsoup.parse(response.getBody());
        Element container = Objects.requireNonNull(doc.getElementById("wrapper")).getElementsByClass("container").first();
        assert container != null;
        return container;
    }

    private static Element getByClass(Element element, String className) {

        Element child = element.getElementsByClass(className).first();
        assert child != null;
        return child;
    }

    private static Elements getAllByClass(Element element, String className) {
        return element.getElementsByClass(className);
    }

    private static Element getById(Element element, String id) {
        Element result = element.getElementById(id);
        assert result != null;
        return result;
    }

    private static Element getByTag(Element element, String tag) {
        Element result = element.getElementsByTag(tag).first();
        assert result != null;
        return result;
    }

    private static Elements getAllByTag(Element element, String className) {
        return element.getElementsByTag(className);
    }

    private static class A extends Element {

        private String href = "";

        public A(Element a) {
            super(a.tag(), a.baseUri(), a.attributes());
            this.href = a.attr("href");
            this.addChildren(a.childNodes().toArray(new Node[0]));
        }

        public A(String tag) {
            super(tag);
        }

        public A(Tag tag, String baseUri, Attributes attributes) {
            super(tag, baseUri, attributes);
        }

        public A(Tag tag, String baseUri) {
            super(tag, baseUri);
        }

        public String href() {
            return href;
        }

        public void setHref(String href) {
            this.href = href;
        }
    }

    private static List<A> getAs(Element element) {
        return element.getElementsByTag("a").stream().map(A::new).collect(Collectors.toList());
    }

    private static A getA(Element element) {
        return new A(Objects.requireNonNull(element.getElementsByTag("a").first()));
    }

    private static Elements getDivs(Element element) {
        return element.getElementsByTag("div");
    }

    private static Element getH2(Element element) {
        return Objects.requireNonNull(element.getElementsByTag("h2").first());
    }

    private static Element getTable(Element element) {
        return Objects.requireNonNull(element.getElementsByTag("table").first());
    }

    private static Elements getTrs(Element element) {
        return element.getElementsByTag("tr");
    }

    private static Elements getTds(Element element) {
        return element.getElementsByTag("td");
    }

    private static Elements getThs(Element element) {
        return element.getElementsByTag("th");
    }

    private static Element getNext(Element element) {
        Element el = element.nextElementSibling();
        assert el != null;
        return el;
    }

    private static Node getNextNode(Node element) {
        Node node = element.nextSibling();
        assert node != null;
        return node;
    }

    private static Element select(Element element, String selector) {
        Element result = element.selectFirst(selector);
        assert result != null;
        return result;
    }

    private static Elements selectUl(Element element, String selector) {
        Element ul = select(element, selector);
        return ul.getElementsByTag("li");
    }

    private static Elements selectNextUl(Element element, String selector) {
        Element ul = select(element, selector).nextElementSibling();
        assert ul != null;
        return ul.getElementsByTag("li");
    }


    private static String getLastChunk(A a) {
        return getLastChunk(a.href());
    }

    private static String getPreLastChunk(A a) {
        return getPreLastChunk(a.href());
    }

    private static String getLastChunk(String url) {
        String[] chunks = url.split("/");
        return chunks[chunks.length - 1];
    }

    private static String getPreLastChunk(String url) {
        String[] chunks = url.split("/");
        return chunks[chunks.length - 2];
    }
}
