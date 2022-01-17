package md.leonis.crawler.moby;

import md.leonis.crawler.moby.executor.Executor;
import md.leonis.crawler.moby.executor.HttpExecutor;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static md.leonis.crawler.moby.config.ConfigHolder.getSource;
import static md.leonis.shingler.utils.StringUtils.*;

class BrokenLinksVerifierTest {

    public static Path getPagesDir(String source) {
        return Paths.get("..").resolve(source).resolve("pages");
    }

    public static Path getFilePath(String uri) {
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }
        uri = escapePathChars(unescapeUriChars(uri));
        return getPagesDir(getSource()).resolve(uri).normalize().toAbsolutePath();
    }

    @Test
    void test() throws Exception {

        // https://groups.io/g/ballyalley/topic/54297463#10920

        Executor.HttpResponse response = new HttpExecutor().getPage("http://youtube.com/watch?v=HohmJVb7wm0", "");


        String href = "https://groups.io/g/ballyalley/topic/uploaded_paul_zibits_tape/86219665";

        URI uri = new URI(href);
        URL url = uri.toURL();

        //TODO port
        Path rootPath = getFilePath(uri.getScheme() + "@" + uri.getHost());
        if (!Files.exists(rootPath)) {
            Files.createDirectories(rootPath);
        }

        Path path = getFilePath(url.toString());

        Path dir = path.getParent();

        List<Path> paths = new ArrayList<>();
        while (!dir.equals(rootPath)) {
            paths.add(dir);
            dir = dir.getParent();
        }

        for (int i = paths.size() - 1; i >= 0; i--) {

            dir = paths.get(i);
            //при записи проверять и если есть такой файл, то копировать в дефолт.
            if (Files.isRegularFile(dir)) {
                Files.move(dir, dir.getParent().resolve("defaultTmp.htm"));
                Files.createDirectories(dir);
                Files.move(dir.getParent().resolve("defaultTmp.htm"), dir.resolve("default.htm"));
            }
        }

        System.out.println(new URI("http://ballyalley.com/ads_and_catalogs/ads_and_catalogs.html").resolve("bally/bally.html"));

        System.out.println("");

        /*List<BrokenLinksVerifier.Rec> recErrorsQueue = FileUtils.loadJsonList(Paths.get(".."), "errors", BrokenLinksVerifier.Rec.class);
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
        Files.write(Paths.get("../errors.csv"), lines);
        Files.write(Paths.get("../errors-distinct.csv"), filteredLines);*/


        System.out.println("\" 123213 \"\" dsfsdf \"".replace("\"", "\"\""));

        /*URI uri = new URI("http://en.wikipedia.org/wiki/Lights_Out_%28game%29");

        String[] chunks = uri.getPath().split("/");
        String chunk = chunks.length == 0 ? uri.getPath() : chunks[chunks.length - 1];*/

        //new HttpExecutor().getPage("https://patft.uspto.gov/netacgi/nph-Parser?Sect1=PTO1&Sect2=HITOFF&d=PALL&p=1&u=%2Fnetahtml%2FPTO%2Fsrchnum.htm&r=1&f=G&l=50&s1=4301503.PN.&OS=PN/4301503&RS=PN/4301503", "");

//file:///H:/Astrocade/Ballyalley/public_html/basic/Program%20Title%20and%20Instructions/Program%20Title%20&%20Instructions%20Without%20Using%20Memory%20(1980)(Steven%20Walters)[Outdated].pdf


        new HttpExecutor().doHead(escapeUriChars("http://ia800700.us.archive.org/zip_dir.php?path=/15/items/46PatentsCitedbytheBallyArcadeandAstrocadePatents.zip&formats=ABBYY GZ%26file=/46PatentsCitedbytheBallyArcadeandAstrocadePatents.zip"), "", new HttpExecutor().getProxy());
//http://ia800700.us.archive.org/zip_dir.php?path=/15/items/46PatentsCitedbytheBallyArcadeandAstrocadePatents.zip&formats=ABBYY GZ%26file=/46PatentsCitedbytheBallyArcadeandAstrocadePatents.zip
        //System.out.println(Integer.toHexString(':'));

        System.out.println(new URI("https://ballyalley.com/ml/ml_source/ml_source.html").resolve("/"));

        String str = "test";

        System.out.println(str.substring(0, Math.min(str.length(), 180)));


        String style = "background-image:url('../../t_pic/BallyAlley_r3_c1.gif')";
        for (String s : style.split(";")) {
            String trim = s.trim();
            if (trim.startsWith("background-image:url")) {
                String substring = trim.replace("background-image:url('", "");
                substring = substring.substring(0, substring.length() - 2);
                System.out.println(substring);
            }
        }


        //System.out.println("https://ballyalley.com/../../index.html".replace("../", ""));

        //System.out.println(new URI("https://ballyalley.com/../../index.html").normalize().toString());

        //System.out.println(new URI("https://ballyalley.com/ml/ml_source/ml_source.html").resolve("../../documentation/BallyCheck/BallyCheck.html"));

        //System.out.println(new URI("https://ballyalley.com/ml/ml_source/ml_source.html").resolve("../documentation/BallyCheck/BallyCheck.html"));

        //System.out.println(new URI("http://ballyalley.com/pics/cassette_pics/Paul_Zibits/Paul_Zibits.html").resolve("/" + "XY_Tutorial_(Seebrees_Computing)_Tape_Insert_Outside_tn.jpg"));

        //System.out.println(new URI("http://ballyalley.com/").resolve("t_pic/BallyAlley_r4_c4.gif"));

        //System.out.println(new URI("http://ballyalleyastrocast.libsyn.com/podcast").getScheme());


        //System.out.println(new URI("http://ballyalley.com/basic/basic.html").resolve("Marc%20Calson/TV%20Output%20Notes%20(Marc%20Calson)(1979).pdf"));

        // http@web.archive.org/web/20160608133602/https:/www.evl.uic.edu/application-research

        //"http@patft.uspto.gov/netacgi/nph-Parser?Sect1=PTO1%26Sect2=HITOFF%26d=PALL%26p=1%26u=%2Fnetahtml%2FPTO%2Fsrchnum.htm%26r=1%26f=G%26l=50%26s1=4301503.PN.%26OS=PN/4301503%26RS=PN/4301503"

        //new HttpExecutor().getPage("https://groups.io/g/ballyalley", "");
    }

    private static String quote(String str) {
        return str.replace("\"", "\"\"");
    }


    private static String distinct(String str) {
        return str.replace("https://www.", "http://").replace("http://www.", "http://").replace("https://", "http://");
    }

}