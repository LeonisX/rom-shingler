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

        parseGameMain(mobyEntry);

        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File("companies.json"), companies);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File("sheets.json"), sheets);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File("gameGroups.json"), gameGroups);
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

    private static Element getById(Element element, String id) {
        Element result = element.getElementById(id);
        assert result != null;
        return result;
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

    private static Element select(Element element, String selector) {

        Element result = element.selectFirst(selector);
        assert result != null;
        return result;
    }

    private static void parseGameMain(MobyEntry entry) throws Exception {

        //TODO save raw html + date, next time read offline
        //TODO process exceptions -> retry list
        HttpExecutor.HttpResponse response = executor.getResponse(String.format(GAME_MAIN, entry.platformId(), entry.gameId()));

        Element container = getContainer(response);
        // 1942 (NES)
        // Main  Credits  Screenshots  Reviews  Cover Art  Promo Art  Releases  Trivia  Hints  Specs  Ad Blurb  Rating Systems  Buy/Trade
        Element rightPanelHeader = getByClass(container, "rightPanelHeader");
        Element niceHeaderTitle = getByClass(rightPanelHeader, "niceHeaderTitle");
        List<A> niceHeaderTitleA = getAs(niceHeaderTitle);

        // Здесь так же можно получить полное название игры и платформы
        assert getLastChunk(niceHeaderTitleA.get(0)).equals(entry.gameId());
        assert getLastChunk(niceHeaderTitleA.get(1)).equals(entry.platformId());

        String gameName = getLastChunk(niceHeaderTitleA.get(0).text());
        String platformName = getLastChunk(niceHeaderTitleA.get(1).text());

        assert gameName.equals(games.get(entry.gameId()));
        assert platformName.equals(games.get(entry.platformId()));

        // Main  Credits  Screenshots  Reviews  Cover Art  Promo Art  Releases  Trivia  Hints  Specs  Ad Blurb  Rating Systems  Buy/Trade
        Element ul = select(rightPanelHeader, "ul.nav-tabs");
        Elements lis = ul.getElementsByTag("li");
        assert lis.size() == 14;

        assert lis.get(0).getElementsByTag("a").text().equals("Main");      // https://www.mobygames.com/game/%s/%s
        assert lis.get(1).getElementsByTag("a").text().equals("Credits");   //https://www.mobygames.com/game/%s/%s/credits
        assert lis.get(2).getElementsByTag("a").text().equals("Screenshots"); //https://www.mobygames.com/game/%s/%s/screenshots
        assert lis.get(3).getElementsByTag("a").text().equals("Reviews");   //https://www.mobygames.com/game/%s/%s/mobyrank
        assert lis.get(4).getElementsByTag("a").text().equals("Cover Art"); //https://www.mobygames.com/game/%s/%s/cover-art
        assert lis.get(5).getElementsByTag("a").text().equals("Promo Art"); //https://www.mobygames.com/game/%s/%s/promo
        assert lis.get(6).getElementsByTag("a").text().equals("Releases");  //https://www.mobygames.com/game/%s/%s/release-info
        assert lis.get(7).getElementsByTag("a").text().equals("Trivia");    //https://www.mobygames.com/game/%s/%s/trivia
        assert lis.get(8).getElementsByTag("a").text().equals("Hints");     //https://www.mobygames.com/game/%s/%s/hints
        assert lis.get(9).getElementsByTag("a").text().equals("Specs");     //https://www.mobygames.com/game/%s/%s/techinfo
        assert lis.get(10).getElementsByTag("a").text().equals("Ad Blurb"); //https://www.mobygames.com/game/%s/%s/adblurbs
        assert lis.get(11).getElementsByTag("a").text().equals("Rating Systems");//https://www.mobygames.com/game/%s/%s/rating-systems
        assert lis.get(12).text().equals("");
        assert lis.get(13).getElementsByTag("a").text().equals("Buy/Trade");//https://www.mobygames.com/game/%s/%s/buy-trade

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

        Elements divs = coreGameRelease.getElementsByTag("div");
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
        divs = coreGameGenre.getElementsByTag("div");
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
            node = node.nextSibling();
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
        Element at = select(divColMd8, "h2:contains(Alternate Titles)");
        ul = at.nextElementSibling();
        assert ul != null;
        lis = ul.getElementsByTag("li");

        lis.forEach(li -> entry.getAlternateTitles().add(((TextNode) li.childNode(0)).text() + ((Element) li.childNode(1)).text()));


        // Part of the Following Groups
        Element potfg = select(divColMd8, "h2:contains(Part of the Following Groups)");
        ul = potfg.nextElementSibling();
        assert ul != null;
        lis = ul.getElementsByTag("li");

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
            el = el.nextSibling();
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

    private static Map<String, String> parseGameCredits(String platform, String gameId) throws Exception {

        HttpExecutor.HttpResponse response = executor.getResponse(String.format(GAME_CREDITS, platform, gameId));

        //TODO process exceptions -> retry list

        Document doc = Jsoup.parse(response.getBody());

        //TODO save raw html + date, next time read offline

        Element container = Objects.requireNonNull(doc.getElementById("wrapper")).getElementsByClass("container").first();

        assert container != null;
        // Получаем полное название игры
        // 1942 (NES)
        Element rightPanelHeader = container.getElementsByClass("rightPanelHeader").first();
        assert rightPanelHeader != null;
        Element niceHeaderTitle = rightPanelHeader.getElementsByClass("niceHeaderTitle").first();
        assert niceHeaderTitle != null;
        String gameTitle = niceHeaderTitle.getElementsByTag("a").first().text();


        Element divColMd8 = container.getElementsByClass("col-md-8").first();
        assert divColMd8 != null;
        // Все метаданные по игре + обложка
        Element coreGameInfo = divColMd8.getElementById("floatholder coreGameInfo");
        assert coreGameInfo != null;
        //Title or BR if no credits
        Element h2Title = coreGameInfo.nextElementSibling();

        assert h2Title != null;
        if (h2Title.tagName().equals("h2")) {

            assert Objects.requireNonNull(h2Title).text().equals(gameTitle + " Credits");
            //Div со всеми кредитами
            Element div = h2Title.nextElementSibling();
            // X people
            assert ((TextNode) Objects.requireNonNull(Objects.requireNonNull(div).nextSibling())).text().endsWith(" people");
            // Table
            Element table = div.getElementsByTag("table").first();
            assert Objects.requireNonNull(table).attr("summary").equals("List of Credits");

            Elements trs = table.getElementsByTag("tr");
            assert Objects.requireNonNull(trs.get(0).getElementsByTag("h2").first()).text().equals("Credits");

            trs.forEach(tr -> {
                Elements tds = tr.getElementsByTag("td");
                if (tds.size() == 2) {
                    assert tr.hasClass("crln");

                    String role = tds.get(0).text();
                    Elements as = tds.get(1).getElementsByTag("a");
                    assert as.size() >= 1;

                    as.forEach(a -> {
                        // <a href="https://www.mobygames.com/developer/sheet/view/developerId,45166/">Ayako Mori</a>
                        String link = a.attr("href");
                        int developerId = Integer.parseInt(getLastChunk(link).split(",")[1]);
                        String developerName = a.text();
                        System.out.println(String.format("%s: %s: %s", role, developerName, developerId));
                    });
                }
            });
        } else if (h2Title.tagName().equals("br")) {
            Element p = h2Title.nextElementSibling();
            assert Objects.requireNonNull(p).tagName().equals("p");
            assert p.text().startsWith("here are no credits for the");
            System.out.println(p.text());
        } else {
            throw new RuntimeException(h2Title.outerHtml());
        }


        return null; //TODO
    }

    private static Map<String, String> parseGameScreenshots(String platform, String gameId) throws Exception {

        HttpExecutor.HttpResponse response = executor.getResponse(String.format(GAME_SCREENSHOTS, platform, gameId));

        //TODO process exceptions -> retry list

        Document doc = Jsoup.parse(response.getBody());

        //TODO save raw html + date, next time read offline

        Element container = Objects.requireNonNull(doc.getElementById("wrapper")).getElementsByClass("container").first();

        assert container != null;
        Element niceHeaderTitle = container.getElementsByClass("niceHeaderTitle").first();
        assert niceHeaderTitle != null;
        String gameTitle = niceHeaderTitle.getElementsByTag("a").first().text();


        Element divColMd12 = container.getElementsByClass("col-md-12").first();
        assert divColMd12 != null;
        // Проверим что система верна
        // <ul class="nav nav-pills" style="margin-bottom: 15px;"><li><a href="https://www.mobygames.com/game/1942arcade/screenshots">All</a></li> <li class="active"><a href="https://www.mobygames.com/game/nes/1942arcade/screenshots">NES</a></li>
        Element ulNav = divColMd12.selectFirst("ul.nav-pills");
        assert ulNav != null;
        Element li = ulNav.select("li.active").first();
        assert li != null;
        Element at = li.getElementsByTag("a").first();
        assert Objects.requireNonNull(at).text().equals(gameTitle);

        Element h2 = ulNav.nextElementSibling();
        assert Objects.requireNonNull(h2).text().equals("User Screenshots");
        Element h3 = h2.nextElementSibling();
        assert Objects.requireNonNull(h3).text().equals(gameTitle + " version");
        Element divRow = h3.nextElementSibling();
        assert Objects.requireNonNull(divRow).tagName().equals("div");

        // <div class="thumbnail-image-wrapper">
        // <a href="https://www.mobygames.com/game/nes/1942arcade/screenshots/gameShotId,34513/"
        // title="1942 NES Title screen" class="thumbnail-image"
        // style="background-image:url(/images/shots/s/34513-1942-nes-screenshot-title-screen.jpg);"></a>
        // </div>
        Elements divs = divRow.getElementsByClass("thumbnail");
        divs.forEach(div -> {
            Element divI = div.selectFirst("div.thumbnail-image-wrapper");
            assert divI != null;
            Element a = divI.getElementsByTag("a").first();
            assert a != null;
            // https://www.mobygames.com/game/nes/1942arcade/screenshots/gameShotId,34513/
            // https://www.mobygames.com/images/shots/l/34513-1942-nes-screenshot-title-screen.jpg
            //System.out.println(a.attr("href"));
            String style = a.attr("style");
            style = style.substring(0, style.length() - 1);
            style = style.replace("background-image:url(", "");
            style = "https://www.mobygames.com" + style;
            System.out.println(style);
            System.out.println(style.replace("/images/shots/s/", "/images/shots/l/"));

            Element divS = div.selectFirst("div.thumbnail-caption");
            assert divS != null;
            Element small = divS.getElementsByTag("small").first();
            assert small != null;
            System.out.println(small.text());
        });


        return null; //TODO
    }

    private static Map<String, String> parseGameReviews(String platform, String gameId) throws Exception {

        HttpExecutor.HttpResponse response = executor.getResponse(String.format(GAME_REVIEWS, platform, gameId));

        //TODO process exceptions -> retry list

        Document doc = Jsoup.parse(response.getBody());

        //TODO save raw html + date, next time read offline

        Element container = Objects.requireNonNull(doc.getElementById("wrapper")).getElementsByClass("container").first();

        assert container != null;
        // Получаем полное название игры
        // 1942 (NES)
        Element rightPanelHeader = container.getElementsByClass("rightPanelHeader").first();
        assert rightPanelHeader != null;
        Element niceHeaderTitle = rightPanelHeader.getElementsByClass("niceHeaderTitle").first();
        assert niceHeaderTitle != null;
        String gameTitle = niceHeaderTitle.getElementsByTag("a").first().text();


        Element divColMd8 = container.getElementsByClass("col-md-8").first();
        assert divColMd8 != null;
        // Все метаданные по игре + обложка
        Element coreGameInfo = divColMd8.getElementById("floatholder coreGameInfo");
        assert coreGameInfo != null;
        //Title or BR if no credits
        Element h2Title = coreGameInfo.nextElementSibling();

        assert h2Title != null;
        assert Objects.requireNonNull(h2Title).text().equals("User Reviews");
        // Table либо нет обзоров
        Element table = h2Title.nextElementSibling();
        assert table != null;
        if (table.tagName().equals("table")) {
            Elements trs = table.getElementsByTag("tr");
            trs.forEach(tr -> {
                Elements tds = table.getElementsByTag("td");
                Element a = tds.get(0).getElementsByTag("a").first();
                assert a != null;
                //TODO save/parse this
                String reviewLink = a.attr("href");
                String summary = a.text();
                a = tds.get(1).getElementsByTag("a").first();
                assert a != null;
                String reviewerLink = a.attr("href");
                String reviewer = a.text();
                String note = Objects.requireNonNull(tds.get(2).selectFirst("img")).attr("alt").replace(" Stars", "");

                System.out.println(reviewLink);
                System.out.println(summary);
                System.out.println(reviewer + ": " + reviewerLink);

            });

        } else {
            assert table.text().startsWith("There are no reviews for the ");
        }

        Element next = table.nextElementSibling();
        assert Objects.requireNonNull(next).className().equals("sideBarLinks");
        next = next.nextElementSibling();
        assert Objects.requireNonNull(next).tagName().equals("br");

        h2Title = next.nextElementSibling();

        assert h2Title != null;
        assert Objects.requireNonNull(h2Title).text().equals("Our Users Say");

        // Table либо нет ревью
        Element divFloatholder = h2Title.nextElementSibling();
        assert divFloatholder != null;
        Element table2 = divFloatholder.getElementsByTag("table").first();
        assert table2 != null;
        if (table2.tagName().equals("table")) {
            Elements trs = table.getElementsByTag("tr");
            trs.forEach(tr -> {
                Elements tds = table2.getElementsByTag("td");
                String category = tds.get(0).text();
                String description = tds.get(1).text();
                String userScore = tds.get(2).text();
                System.out.println(category);
                System.out.println(description);
                System.out.println(userScore);
            });
            Elements ths = table2.getElementsByTag("th");
            String overallUserVotes = ths.get(3).text()
                    .replace("Overall User Score (", "")
                    .replace(" votes)", ""); // Overall User Score (52 votes)
            String overallUserScore = ths.get(4).text();
            System.out.println(overallUserVotes);
            System.out.println(overallUserScore);

        } else {
            assert table.text().startsWith("There are no reviews for the ");
        }

        next = table.nextElementSibling();
        assert Objects.requireNonNull(next).className().equals("sideBarLinks");
        next = next.nextElementSibling();
        assert Objects.requireNonNull(next).tagName().equals("br");
        next = next.nextElementSibling();
        assert Objects.requireNonNull(next).tagName().equals("br");

        //critic reviews
        h2Title = next.nextElementSibling();

        assert h2Title != null;
        assert Objects.requireNonNull(h2Title).text().equals("Critic Reviews");

        // Table либо нет ревью
        Elements divRanks = divColMd8.getElementsByClass("floatholder mobyrank scoresource");
        // <div class="floatholder mobyrank scoresource">
        //  <div class="fl scoreBoxMed scoreHi">80</div>
        //  <div class="source scoreBorderHi"><a href="https://www.mobygames.com/mobyrank/source/sourceId,998/">Tilt </a> (Dec, 1987)</div>
        //  <div class="citation">En conclusion, 1942 est, à tous points de vue, un très bon jeu sur console.</div>
        //  <div class="url"><a target="_blank" href="http://download.abandonware.org/magazines/Tilt/tilt_numero049/TILT%20-%20n%B049%20-%20decembre%201987%20-%20page100%20et%20page101.jpg">read review</a></div>
        //</div>
        divRanks.forEach(divRank -> {
            Elements divs = divRank.getElementsByTag("div");
            String score = divs.get(1).text();
            Element a = divs.get(2).getElementsByTag("a").first();
            assert a != null;
            String source = a.text().trim();
            String sourceHref = a.attr("href");
            TextNode textNode = (TextNode) a.nextSibling();
            assert textNode != null;
            String date = textNode.text().trim(); // parse
            String citation = divs.get(3).text();

            System.out.println(score);
            System.out.println(source + ": " + sourceHref);
            System.out.println(date);
            System.out.println(citation);

            if (divs.size() == 5) {
                a = divs.get(4).getElementsByTag("a").first();
                assert a != null;
                String reviewHref = a.attr("href");
                System.out.println(reviewHref); //TODO download/parse/map broken links or read from archive
            }
        });

        return null; //TODO
    }

    private static Map<String, String> parseGameCoverArt(String platform, String gameId) throws Exception {

        HttpExecutor.HttpResponse response = executor.getResponse(String.format(GAME_COVER_ART, platform, gameId));

        //TODO process exceptions -> retry list

        Document doc = Jsoup.parse(response.getBody());

        //TODO save raw html + date, next time read offline

        Element container = Objects.requireNonNull(doc.getElementById("wrapper")).getElementsByClass("container").first();

        assert container != null;
        Element niceHeaderTitle = container.getElementsByClass("niceHeaderTitle").first();
        assert niceHeaderTitle != null;
        String gameTitle = Objects.requireNonNull(niceHeaderTitle.getElementsByTag("a").first()).text();

        Element small = niceHeaderTitle.getElementsByTag("small").first();
        assert small != null;
        String systemName = Objects.requireNonNull(small.getElementsByTag("a").first()).text();

        Element navTabs = niceHeaderTitle.nextElementSibling();
        assert navTabs != null;
        Element navPills = navTabs.nextElementSibling();

        // в цикле читать дивы:
        // coverHeading
        // row
        // sideBarLinks
        assert navPills != null;
        Element current = navPills;

        do {
            current = current.nextElementSibling();
            assert current != null;
            if (!current.className().equals("coverHeading")) {
                break;
            }
            Element h2 = current.child(0);
            assert h2.tagName().equals("h2");
            assert h2.text().equals(systemName);
            Elements trs = current.getElementsByTag("tr");
            // Эти параметры на самом деле разные, надо собрать список и мэппить соответственно.
            for (Element tr : trs) {
                Elements tds = tr.getElementsByTag("td");
                assert tds.size() == 3;
                String property = tds.get(0).text();
                assert tds.get(1).text().equals(" : ");
                Elements spans = tds.get(2).getElementsByTag("span");
                List<String> values = spans.stream()
                        .map(span -> ((TextNode) span.childNode(0)).text()).collect(Collectors.toList());
                System.out.println(property + ": " + String.join(", ", values));
            }

            current = current.nextElementSibling();
            assert Objects.requireNonNull(current).className().equals("row");
            Elements divs = current.getElementsByClass("col-xs-6 col-sm-3 col-md-2");
            divs.forEach(div -> {
                Element a = div.selectFirst("a.thumbnail-cover");
                assert a != null;
                String style = a.attr("style");
                style = style.substring(0, style.length() - 2); // ");:
                style = style.replace("background-image:url(", "");
                style = "https://www.mobygames.com" + style;
                System.out.println(style);
                System.out.println(style.replace("/images/covers/s/", "/images/covers/l/"));

                Element divS = div.selectFirst("div.thumbnail-cover-caption");
                assert divS != null;
                Element p = divS.getElementsByTag("p").first();
                assert p != null;
                System.out.println(p.text());
            });

            current = current.nextElementSibling();
            assert Objects.requireNonNull(current).className().equals("sideBarLinks");


        } while (true);


        return null; //TODO
    }


    private static Map<String, String> parseGamesListToc(String systemId) throws Exception {

        Map<String, String> games = new LinkedHashMap<>();

        HttpExecutor.HttpResponse response = executor.getResponse(String.format(GAMES_PAGES, systemId, 0));

        //TODO process exceptions -> retry list

        Document doc = Jsoup.parse(response.getBody());

        // <td class="mobHeaderPage" width="33%">Viewing Page 1 of 56</td>
        String[] chunks = Objects.requireNonNull(doc.getElementsByClass("mobHeaderPage").first()).text().split(" ");
        int pagesCount = Integer.parseInt(chunks[chunks.length - 1]);
        assert pagesCount > 0;

        System.out.println("Pages count: " + pagesCount);

        // <td class="mobHeaderItems" width="34%">(items 1-25 of 1397)</td>
        chunks = Objects.requireNonNull(doc.getElementsByClass("mobHeaderItems").first()).text().split(" ");
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
        Element table = Objects.requireNonNull(doc.getElementById("mof_object_list")).getElementsByTag("tbody").first();
        assert table != null;
        table.getElementsByTag("tr").forEach(e -> {
            // Game Title	        Year	Publisher	        Genre
            // Bad Street Brawler	1989	Mattel Electronics	Action
            List<Element> a = e.getElementsByTag("td").stream().map(t -> t.getElementsByTag("a").first()).collect(Collectors.toList());
            /*System.out.println("Game Title:" + getLastChunk(a.get(0).attr("href")));
            System.out.println("Game Title:" + a.get(0).text());*/
            games.put(getLastChunk(a.get(0).attr("href")), a.get(0).text());
            /*System.out.println("Game Title:" + a.get(0));
            System.out.println("Year:" + a.get(1));
            System.out.println("Publisher:" + a.get(2));
            System.out.println("Genre:" + a.get(3));*/
        });
    }

    private static Map<String, String> parseSystemsList() throws Exception {

        Map<String, String> systems = new LinkedHashMap<>();

        HttpExecutor.HttpResponse response = executor.getResponse(PLATFORMS);

        //TODO process exceptions -> retry list

        Document doc = Jsoup.parse(response.getBody());
        Objects.requireNonNull(doc.getElementsByClass("browseTable").first()).getElementsByTag("a").forEach(a -> {
            systems.put(getLastChunk(a.attr("href")), a.text());
        });

        return systems;
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
