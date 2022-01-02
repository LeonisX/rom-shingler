package md.leonis.crawler.moby;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import md.leonis.crawler.moby.executor.Executor;
import md.leonis.crawler.moby.executor.HttpExecutor;
import md.leonis.shingler.utils.FileUtils;
import org.apache.hc.core5.http.Header;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static md.leonis.crawler.moby.config.ConfigHolder.getPagesDir;
import static md.leonis.crawler.moby.config.ConfigHolder.getSource;
import static md.leonis.shingler.utils.StringUtils.*;

//TODO сохранять и парсить всё, даже если ошибки
public class BrokenLinksVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrokenLinksVerifier.class);

    static Set<String> visited = new HashSet<>();
    static Queue<Rec> recQueue = new LinkedList<>();
    static Queue<Rec> recErrorsQueue = new LinkedList<>();
    static Queue<Rec> visitedQueue = new LinkedList<>();

    static Map<String, String> resolver = new HashMap<>();

    static HttpExecutor executor = new HttpExecutor();

    static Set<String> hosts;

    static boolean useCache = true;

    static int maxLevel = 12;
    static int maxExtLevel = 1;
    static int maxExtMediaLevel = 1;

    public static void main(String[] args) throws Exception {

        Timer timer = new Timer("Timer");
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                System.out.printf("Queue: %s; Errors: %s%n", recQueue.size(), recErrorsQueue.size());
            }
        }, 20 * 1000, 20 * 1000);

        //List<Rec> errors = FileUtils.loadJsonList(Paths.get("."), "errors", Rec.class);

        String uri = "http://ballyalley.com";

        hosts = new HashSet<>();
        hosts.add(new URI(uri).normalize().getHost());
        hosts.add("www." + new URI(uri).normalize().getHost());

        recQueue.add(new Rec(new URI(uri).normalize(), uri, "", new URI("/").normalize(), null, 0));

        while (!recQueue.isEmpty()) {
            Rec rec = recQueue.poll();
            if (rec.getExceptions().size() >= 5) {
                recErrorsQueue.add(rec);
            } else if (rec.isAvailable()) {
                readUri(rec);
            } else {
                recQueue.add(rec);
            }
        }

        timer.cancel();
        FileUtils.saveAsJson(Paths.get("./errors.json"), recErrorsQueue);
        List<String> rawLines = recErrorsQueue.stream().map(e -> String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"", quote(e.getExceptions().stream().distinct()
                        .collect(Collectors.joining(", "))), quote(unescapeUriChars(e.getUri().toString())),
                quote(e.getRawUri()), quote(e.getText()), quote(unescapeUriChars(e.getReferrer().toString())))).distinct().collect(Collectors.toList());
        List<String> filteredRawLines = recErrorsQueue.stream().map(e -> String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"", distinct(quote(e.getExceptions().stream().distinct()
                        .collect(Collectors.joining(", ")))), distinct(quote(unescapeUriChars(e.getUri().toString()))),
                distinct(quote(e.getRawUri())), distinct(quote(e.getText())), distinct(quote(unescapeUriChars(e.getReferrer().toString()))))).distinct().collect(Collectors.toList());
        List<String> lines = new ArrayList<>();
        List<String> filteredLines = new ArrayList<>();
        for (int i = 0; i < rawLines.size(); i++) {
            lines.add(String.format("\"%s\",%s", i, rawLines.get(i)));
        }
        for (int i = 0; i < filteredRawLines.size(); i++) {
            filteredLines.add(String.format("\"%s\",%s", i, filteredRawLines.get(i)));
        }
        lines.add(0, "\"Id\",\"Error\",\"Uri\",\"Raw Uri\",\"Text\",\"Referrer\"");
        filteredLines.add(0, "\"Id\",\"Error\",\"Uri\",\"Raw Uri\",\"Text\",\"Referrer\"");
        Files.write(Paths.get("./errors.csv"), lines);
        Files.write(Paths.get("./errors-distinct.csv"), filteredLines);
    }

    private static String distinct(String str) {
        return str.replace("https://www.", "http://").replace("http://www.", "http://").replace("https://", "http://");
    }

    private static String quote(String str) {
        return str.replace("\"", "\"\"");
    }

    private static void readUri(Rec rec) {

        //System.out.println("readUri: " + rec.getUri() + " <-- " + rec.getReferrer());

        URI uri = rec.getUri();

        try {
            if (isPage(uri)) {
                readPage(rec);
            } else {
                getFile(rec);
            }
        } catch (SSLException e) {
            System.out.println(rec.getRawUri() + " <--- " + rec.getReferrer() + " [" + rec.getLevel() + "]");
            if (e.getMessage().equals("Unsupported or unrecognized SSL message") && uri.getScheme().equals("http")) {
                try {
                    rec.setUri(normalize(new URI(rec.getUri().toString().replace("http", "https"))));
                } catch (URISyntaxException uriSyntaxException) {
                    uriSyntaxException.printStackTrace();
                }
                recQueue.add(rec);
            } else {
                recQueue.add(rec.withException(e));
            }
        } catch (Exception e) {
            System.out.println(rec.getRawUri() + " <--- " + rec.getReferrer() + " [" + rec.getLevel() + "]");
            //TODO здесь надо ретрай делать на самом деле, ошибка может быть временной. Но так же это может указать на мёртвый сайт.
            e.printStackTrace();
            recQueue.add(rec.withException(e));
        }
    }

    private static void readPage(Rec rec) throws Exception {

        URI uri = rec.getUri();
        URI referrer = rec.getReferrer();

        if (uri.toString().contains("300_baud_programs/W&W_software_sales/W&W_software_sales.html")) {
            System.out.println();
        }

        if (visited.contains(uri.toString())) {
            return;
        }

        HttpExecutor.HttpResponse response = getAndSavePage(uri, referrer.toString());

        if (response.getCode() != 200) {
            System.out.println(response.getCode() + ": " + rec.rawUri + " <--- " + rec.referrer + " [" + rec.level + "]");
        }

        if (response.getCode() == 200) {
            visited.add(uri.toString());
            visitedQueue.add(new Rec(uri, rec.getRawUri(), rec.getText(), referrer, response, rec.getLevel()));
        } else {
            recQueue.add(rec.withResponse(response).withException(new RuntimeException(String.valueOf(response.getCode()))));
        }
        Document doc = Jsoup.parse(response.getBody());

        //todo identify extensions
        for (Element a : doc.getElementsByAttribute("href")) {
            addToRecQueue(a.attr("href"), a.text(), rec);
        }

        //TODO alt, title, ...
        for (Element img : doc.getElementsByAttribute("src")) {
            addToRecQueue(img.attr("src"), img.text(), rec);
        }

        //  style="background-image:url('../../t_pic/BallyAlley_r3_c1.gif');
        for (Element el : doc.getElementsByAttribute("style")) {
            String style = el.attr("style");
            for (String s : style.split(";")) {
                String trim = s.trim();
                if (trim.startsWith("background-image:url('")) {
                    String substring = trim.replace("background-image:url('", "");
                    substring = substring.substring(0, substring.length() - 2);
                    addToRecQueue(substring, el.text(), rec);
                } else if (trim.startsWith("background-image:url(")) {
                    String substring = trim.replace("background-image:url(", "");
                    substring = substring.substring(0, substring.length() - 1);
                    addToRecQueue(substring, el.text(), rec);
                }
            }
        }
    }

    private static void addToRecQueue(String href, String text, Rec rec) throws URISyntaxException {

        URI uri = rec.getUri().normalize();

        if (href.equals("")) {
            recErrorsQueue.add(new Rec(new URI(escapeUriChars(href)), "\"\"", "<empty value>", uri, null, -1));
            return;
        }

        // #, #start
        if (href.startsWith("#")) {
            //System.out.println("Ignore: " + href + " <--- " + rec.referrer + " [" + rec.level + "]");
            return;
        }

        if (href.contains("#")) {
            href = href.split("#")[0];
        }

        //TODO
        // javascript:print();
        if (href.startsWith("javascript:")) {
            recErrorsQueue.add(new Rec(new URI(escapeUriChars(href)), href, "Unprocessable entity", uri, null, -1));
            return;
        }

        //TODO
        // data:image/svg+xml,<svg xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22 viewBox%3D%220 0 1024 1024%22 style%3D%22background%3A%23c4b562%22><g><text text-anchor%3D%22middle%22 dy%3D%22.35em%22 x%3D%22512%22 y%3D%22512%22 fill%3D%22%23ffffff%22 font-size%3D%22700%22 font-family%3D%22-apple-system%2C BlinkMacSystemFont%2C Roboto%2C Helvetica%2C Arial%2C sans-serif%22>S<%2Ftext><%2Fg><%2Fsvg>
        // data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" style="background:#c4b562"><g><text text-anchor="middle" dy=".35em" x="512" y="512" fill="#ffffff" font-size="700" font-family="-apple-system, BlinkMacSystemFont, Roboto, Helvetica, Arial, sans-serif">S</text></g></svg>
        // data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7
        // data:image/gif;base64,R0lGODlhAQABAIABAAAAAP///yH5BAEAAAEALAAAAAABAAEAQAICTAEAOw%3D%3D
        // data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAALCAIAAAD5gJpuAAAABGdBTUEAAK/INwWK6QAAABl0RVh0U29mdHdhcmUAQWRvYmUgSW1hZ2VSZWFkeXHJZTwAAAE2SURBVHjaYvz69SsDEvj37x+ERGbAwZ9//wACiAUoysXFBST///8P0QOm//+HU0jgxYsXAAHEAlP0H8HYt+//4SP/f//6b2b238sLrpqRkRFoCUAAsaCrXrv2/8KF///8+f/r9//Dh/8/ffI/OQWiAeJCgABigrseJPT27f/Vq////v3/1y8oWrzk/+PHcEv+/PkDEEBMEM/B3fj/40eo0t9g8suX/w8f/odZAVQMEEAsQAzj/2cQFf3PxARWCrYEaBXQLCkpqB/+/wcqBgggJrjxQPX/hYX/+/v///kLqhpIBgf/l5ODhxiQBAggFriToDoTEv5zcf3ftQuk2s7uf0wM3MdAAPQDQAAxvn37lo+PDy4KZUDcycj4/z9CBojv3r0LEEAgG969eweLSBDEBSCWAAQYACaTbJ/kuok9AAAAAElFTkSuQmCC
        if (href.startsWith("data:")) {
            recErrorsQueue.add(new Rec(new URI(escapeUriChars(href)), href, "Unprocessable entity", uri, null, -1));
            return;
        }

        //TODO
        // android-app://com.google.android.youtube/http/www.youtube.com/channel/UCrzfQpfcDnZhkQs1oO3LVBA
        if (href.startsWith("android-app://")) {
            recErrorsQueue.add(new Rec(new URI(escapeUriChars(href)), href, "Unprocessable entity", uri, null, -1));
            return;
        }

        //TODO
        // ios-app://544007664/vnd.youtube/www.youtube.com/channel/UCrzfQpfcDnZhkQs1oO3LVBA
        if (href.startsWith("ios-app://")) {
            recErrorsQueue.add(new Rec(new URI(escapeUriChars(href)), href, "Unprocessable entity", uri, null, -1));
            return;
        }

        // file:///H:/Astrocade/Ballyalley/public_html/basic/Program%20Title%20and%20Instructions/Program%20Title%20&%20Instructions%20Without%20Using%20Memory%20(1980)(Steven%20Walters).pdf
        if (href.startsWith("file://")) {
            recErrorsQueue.add(new Rec(new URI(escapeUriChars(href)), href, "Unprocessable entity", uri, null, -1));
            return;
        }

        //TODO
        //tel:312-964-5020
        if (href.startsWith("tel:")) {
            recErrorsQueue.add(new Rec(new URI(escapeUriChars(href)), "Unprocessable entity", href, uri, null, -1));
            return;
        }

        // TODO mw-data:TemplateStyles:r999302996
        if (href.startsWith("mw-data:")) {
            recErrorsQueue.add(new Rec(new URI(escapeUriChars(href)), "Unprocessable entity", href, uri, null, -1));
            return;
        }

        //TODO news://rec.games.video.classic
        if (href.startsWith("news://")) {
            recErrorsQueue.add(new Rec(new URI(escapeUriChars(href)), "Unprocessable entity", href, uri, null, -1));
            return;
        }

        if (href.startsWith("mailto:")) {
            return;
        }

        // хак, так делает браузер, хотя, он скорее всего делает это поумнее.
        String hackedHref = href.replace("\\", "/");
        if (!hackedHref.equals(href)) {
            recErrorsQueue.add(new Rec(new URI(escapeUriChars(href)), "Backslash (\\) in href", href, uri, null, -1));
            href = hackedHref;
        }

        URI newUri = processUri(uri, href);

        if (newUri == null) {
            System.out.println();
        }

        int level = getNextLevel(newUri, rec);

        if (isNative(newUri)) {
            if (level > maxLevel) {
                return;
            }
        } else {
/*            if (level < -1) {
                System.out.println();
            }*/
            if (isPage(newUri)) {
                if (level * -1 > maxExtLevel) {
                    return;
                }
            } else {
                if (level * -1 > maxExtMediaLevel) {
                    return;
                }
            }
        }

        if (normalize(newUri).toString().equals("/")) {
            System.out.println();
        }
        if (normalize(new URI(newUri.toString().replace("../", ""))).toString().equals("/")) {
            System.out.println();
        }
        if (normalize(new URI(newUri.toString().trim())).toString().equals("/")) {
            System.out.println();
        }

        recQueue.add(new Rec(normalize(newUri), href, text, uri, null, level));
        if (newUri.toString().contains("../")) {
            recQueue.add(new Rec(normalize(new URI(newUri.toString().replace("../", ""))), href, text, uri, null, level));
            recErrorsQueue.add(new Rec(new URI(escapeUriChars(href)), "Errors in levels (extra ../)", href, uri, null, -1));
        }
        if (!newUri.toString().trim().equals(newUri.toString())) {
            recQueue.add(new Rec(normalize(new URI(newUri.toString().trim())), href, text, uri, null, level));
        }
    }

    public static int getNextLevel(URI uri, Rec rec) {
        if (isNative(rec.getUri())) {
            if (isNative(uri)) {
                return rec.getLevel() + 1;
            } else {
                return -1;
            }
        } else {
            return rec.getLevel() - 1;
        }
    }

    private static boolean isPage(URI uri) {

        if ((uri.toString() == null)) {
            System.out.println();
        }

        if (uri.toString().equals("/")) {
            return true;
        }

        String[] chunks = uri.getPath().split("/");
        String chunk = chunks.length == 0 ? uri.getPath() : chunks[chunks.length - 1];

        return (uri.getPath().isEmpty() || chunk.endsWith(".html") || chunk.endsWith(".htm") || !chunk.contains("."));
    }

    private static boolean isNative(URI uri) {
        return hosts.contains(uri.normalize().getHost());
    }

    private static URI normalize(URI uri) throws URISyntaxException {
        String str = uri.toString();
        if (str.endsWith("/")) {
            return new URI(str.substring(0, str.length() - 1)).normalize();
        } else {
            return uri.normalize();
        }
    }

    private static URI processUri(URI referrer, String link) throws URISyntaxException {

        if (referrer.toString().equals("/")) {
            System.out.println("");
        }

        String href = escapeUriChars(link);
        //System.out.println("Process URI: " + href + " <--- " + referrer);

        URI uri = referrer.getPath().isEmpty() ? new URI(referrer.toString() + '/') : new URI(referrer.toString());
        if (href.startsWith("http")) {
            uri = new URI(href);
        } else if (href.equals("/")) {
            uri = uri;
        } else if (href.startsWith("..") || href.startsWith(".") || href.startsWith("//")) {
            uri = uri.resolve(href);
        } else {
            uri = uri.resolve(href);
        }
        //System.out.println("!!!!! " + uri.normalize());

        resolver.put(uri.toString(), href + " <-- " + uri);

        if (link.contains("\\ads_and_catalogs.html")) {
            String u = uri.toString();
            System.out.println(u);
        }

        if (uri.toString().equals("/")) {
            System.out.println("");
        }
        if (!uri.toString().startsWith("http")) {
            System.out.println("");
        }

        //TODO!!!!!!!!!!!!
        //return new URI(unescapeUriChars(uri.toString()));
        return uri;
    }

    public static Path getFilePath(String uri) {
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }
        uri = escapePathChars(unescapeUriChars(uri));
        return getPagesDir(getSource()).resolve(uri).normalize().toAbsolutePath();
    }

    private static HttpExecutor.HttpResponse getAndSavePage(URI uri, String referrer) throws Exception {

        if (uri.toString().equals("/")) {
            uri = new URI(referrer).resolve(uri);
        }

        //System.out.println("getAndSavePage: " + uri + " <-- " + referrer);
        Path path = getFilePath(uri.toString());

        if (useCache && Files.exists(path)) {
            if (Files.isDirectory(path)) {
                path = path.resolve("default.htm");
            }
            if (Files.exists(path)) {
                //LOGGER.info("Use cached page: " + path.toAbsolutePath());
                return new Executor.HttpResponse(new String(Files.readAllBytes(path)), 200, new Header[0]);
            }
        }

        HttpExecutor.HttpResponse response = executor.getPage(escapeUriChars(uri.toString()), referrer);

        if (response.getCode() == 200) {
            //System.out.println("trying to save page: " + path);
            createDirectories(path);
            Files.write(path, response.getBody().getBytes());
            //System.out.println("saved page: " + path);
        }

        return response;
    }

    private static void getFile(Rec rec) throws Exception {

        URI uri = rec.getUri();
        URI referrer = rec.getReferrer();

        if (visited.contains(uri.toString())) {
            return;
        }

        HttpExecutor.HttpResponse response;

        if (true) { //TODO
            response = headFile(uri, referrer);
            //System.out.println(response);
        } else {
            response = getAndSaveFile(uri, referrer);
            //System.out.println(response);
        }

        if (response.getCode() == 200) {
            visited.add(uri.toString());
            visitedQueue.add(new Rec(uri, rec.getRawUri(), rec.getText(), referrer, response, rec.getLevel()));
        } else {
            System.out.println(response.getCode() + ": " + rec.rawUri + " <--- " + rec.referrer + " [" + rec.level + "]");
            recQueue.add(rec.withResponse(response).withException(new RuntimeException(String.valueOf(response.getCode()))));
        }
    }

    private static HttpExecutor.HttpResponse headFile(URI uri, URI referrer) throws Exception {

        //System.out.println("headFile: " + uri + " <-- " + referrer);
        return executor.doHead(escapeUriChars(uri.toString()), referrer.toString(), executor.getProxy());
    }

    private static HttpExecutor.HttpResponse getAndSaveFile(URI uri, URI referrer) throws Exception {

        //System.out.println("getAndSaveFile: " + uri + " <-- " + referrer);
        Path path = getFilePath(uri.toString());

        if (useCache && Files.exists(path)) {
            //LOGGER.info("Use cached file: " + path.toAbsolutePath());
            return new Executor.HttpResponse(new String(Files.readAllBytes(path)), 200, new Header[0]);
        }

        HttpExecutor.HttpResponse response = executor.getFile(escapeUriChars(uri.toString()), referrer.toString());

        if (response.getCode() == 200) {
            //System.out.println("trying to save file: " + path);
            createDirectories(path);
            Files.write(path, response.getBytes());
            //System.out.println("saved file: " + path);
        }

        return response;
    }

    private static void createDirectories(Path path) throws IOException {

        Path rootPath = getPagesDir(getSource()).normalize().toAbsolutePath();

        //System.out.println("rootPath: " + rootPath);
        //System.out.println("path: " + path);

        Path parent = path.getParent();
        //при записи проверять и если есть такой файл, то копировать в дефолт.
        List<Path> paths = new ArrayList<>();
        //System.out.println("parent: " + parent);
        while (!parent.equals(rootPath)) {
            paths.add(parent);
            parent = parent.getParent();
        }

        for (int i = paths.size() - 1; i >= 0; i--) {
            Path dir = paths.get(i);
            //при записи проверять и если есть такой файл, то копировать в дефолт.
            if (Files.isRegularFile(dir)) {
                Files.move(dir, dir.getParent().resolve("defaultTmp.htm"));
                Files.createDirectories(dir);
                Files.move(dir.getParent().resolve("defaultTmp.htm"), dir.resolve("default.htm"));
            }
        }

        Files.createDirectories(path.getParent());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rec {

        private URI uri;
        private String rawUri;
        private URI referrer;
        private String text;
        @JsonIgnore
        private Executor.HttpResponse httpResponse;
        private List<String> exceptions = new ArrayList<>();
        @JsonIgnore
        private Long retryAfter;
        @JsonIgnore
        private int level = 0;

        public Rec(URI uri, String rawUri, String text, URI referrer, Executor.HttpResponse httpResponse, int level) {
            this.uri = uri;
            this.rawUri = rawUri;
            this.text = text;
            this.referrer = referrer;
            this.httpResponse = httpResponse;
            this.level = level;
        }

        @JsonIgnore
        public boolean isAvailable() {

            if (null == retryAfter) {
                return true;
            } else {
                return new Date().getTime() > retryAfter;
            }
        }

        public Rec withException(Throwable e) {
            exceptions.add(e.getClass().getSimpleName() + ": " + e.getMessage());
            return this;
        }

        public Rec withResponse(Executor.HttpResponse response) {
            this.httpResponse = response;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Rec rec = (Rec) o;
            return Objects.equals(uri, rec.uri);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uri);
        }

        @Override
        public String toString() {
            return "Rec{" +
                    "uri=" + uri +
                    ", referrer=" + referrer + ", " + exceptions.size() +
                    '}';
        }
    }
}


//TODO а что если вместо страницы скачается бинарный файл? Он корректно сохранится? А обработается?