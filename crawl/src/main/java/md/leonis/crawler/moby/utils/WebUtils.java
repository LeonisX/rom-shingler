package md.leonis.crawler.moby.utils;

import md.leonis.crawler.moby.config.ConfigHolder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebUtils {

    public static String readFromUrl(String urlAddress) throws IOException {
        URL url = new URL(urlAddress);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("AuthToken", ConfigHolder.serverSecret);
        conn.setRequestProperty("User-Agent", "TiVi's admin client");
        return MultipartUtility.readResponse(conn);
    }

    public static void openWebPage(String url) {
        String os = System.getProperty("os.name").toLowerCase();
        Runtime rt = Runtime.getRuntime();

        try {
            if (os.contains("win")) {
                // this doesn't support showing urls in the form of "page.html#nameLink"
                rt.exec("rundll32 url.dll,FileProtocolHandler " + url);

            } else if (os.contains("mac")) {
                rt.exec("open " + url);

            } else if (os.contains("nix") || os.contains("nux")) {
                // Do a best guess on unix until we get a platform independent way
                // Build a list of browsers to try, in this order.
                String[] browsers = {"epiphany", "firefox", "mozilla", "chromium", "chrome", "konqueror",
                        "netscape", "opera", "safari", "links", "lynx", "midori"};

                // Build a command string which looks like "browser1 "url" || browser2 "url" ||..."
                StringBuilder cmd = new StringBuilder();
                for (int i = 0; i < browsers.length; i++)
                    cmd.append(i == 0 ? "" : " || ").append(browsers[i]).append(" \"").append(url).append("\" ");

                rt.exec(new String[]{"sh", "-c", cmd.toString()});

            }
        } catch (Exception ignored) {
        }
    }
}
