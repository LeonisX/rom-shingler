package md.leonis.crawler.moby.crawler;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

class JsDosCrawlerTest {

    @Test
    void uri() throws URISyntaxException {
        URI uri = new URI("https://cdn.dos.zone/original/2X/f/f9ddfcf94f9412ca549ff26b48f2c000d8ff7d24.png");
        System.out.println(uri.getHost());
        System.out.println(uri.getPath());
        System.out.println(uri.getScheme() +  "://" + uri.getHost());
    }

}