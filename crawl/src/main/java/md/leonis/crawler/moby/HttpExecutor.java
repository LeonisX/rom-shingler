package md.leonis.crawler.moby;

import md.leonis.crawler.moby.dto.FileEntry;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsStore;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static md.leonis.crawler.moby.config.ConfigHolder.cacheDir;
import static md.leonis.crawler.moby.config.ConfigHolder.pagesDir;

public class HttpExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpExecutor.class);

    private static final List<Proxy> DIRECT_LIST = Collections.singletonList(new Proxy(1, null, null, 0, null, null));

    public static final Path HTML_CACHE = Paths.get("./cache");

    private static final Random RANDOM = new Random();

    private final List<Proxy> proxies;

    public static boolean validate = false;

    private volatile int index;

    public HttpExecutor(List<Proxy> proxies) {

        this.proxies = proxies;
        this.index = proxies.isEmpty() ? 0 : RANDOM.nextInt(proxies.size());
    }

    // Instance for test purposes
    public static HttpExecutor directInstance() {
        return new HttpExecutor(DIRECT_LIST);
    }

    // Proxies selector

    public synchronized Proxy getProxy() {
        return getNextProxy(nextIndex(index), 0);
    }

    private int nextIndex(int index) {
        return (index == proxies.size() - 1) ? 0 : index + 1;
    }

    private Proxy getNextProxy(int currentIndex, int count) {

        //LOGGER.debug(String.format("Select proxy. Initial index: %s; Current index: %s", initialIndex, currentIndex));

        if (count == proxies.size()) {
            LOGGER.warn("No free proxies!!!");
            throw new RuntimeException("No free proxies!!!");
        }

        Proxy proxy = proxies.get(currentIndex);

        if (proxy.isAvailable()) {
            index = currentIndex;
            //LOGGER.debug("Selected proxy: " + proxy.getHost());
            return proxy;
        } else {
            return getNextProxy(nextIndex(currentIndex), ++count);
        }
    }

    public HttpResponse getPage(String fullUri, String referrer) throws Exception {
        URL url = new URL(fullUri);
        return getPage(url.getProtocol() + "://" + url.getHost(), url.getPath(), referrer, getProxy());
    }

    public HttpResponse getPage(String host, String uri, String referrer, Proxy proxy) throws Exception {
        return doGetPage(host, uri, referrer, proxy, new HttpGet(uri));
    }

    //TODO process exceptions -> retry list
    private HttpResponse doGetPage(String host, String uri, String referrer, Proxy proxy, HttpUriRequestBase httpUriRequestBase) throws Exception {

        try {
            LOGGER.info(host + uri);

            Path path = getPath(uri);

            if (Files.exists(path)) { // TODO if read from cache
                if (Files.isDirectory(path)) {
                    path = path.resolve("default.htm");
                }
                if (Files.exists(path)) {
                    LOGGER.info("Use cached page: " + path.toAbsolutePath());
                    return new HttpResponse(new String(Files.readAllBytes(path)), 200, new Header[0]);
                }
            }

            String[] chunks = host.split("://");
            HttpHost targetHost = new HttpHost(chunks[0], chunks[1]);

            referrer = (referrer == null) ? "https://www.google.com/" : referrer;

            httpUriRequestBase.addHeader(new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"));
            addBrowserHeaders(httpUriRequestBase, referrer);

            //Prepare the CloseableHttpClient
            CloseableHttpClient closeableHttpClient;

            if (null != proxy.getHost()) {
                HttpHost proxyHost = new HttpHost(proxy.getScheme(), proxy.getHost(), proxy.getPort());
                httpUriRequestBase.setConfig(RequestConfig.custom().setProxy(proxyHost).build());

                CredentialsStore credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(new AuthScope(proxy.getHost(), proxy.getPort()),
                        new UsernamePasswordCredentials(proxy.getUserName(), proxy.getPasswordArray()));

                RequestConfig config = RequestConfig.custom()
                        // Determines the timeout until a new connection is fully established. This may also include transport security negotiation exchanges such as SSL or TLS protocol negotiation).
                        .setConnectTimeout(Timeout.ofSeconds(5)).build();

                closeableHttpClient = HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).setDefaultRequestConfig(config).build();
            } else {
                closeableHttpClient = HttpClients.createDefault();
            }

            //Get the result
            CloseableHttpResponse closeableHttpResponse = closeableHttpClient.execute(targetHost, httpUriRequestBase);
            HttpResponse httpResponse = new HttpResponse(closeableHttpResponse);

            //LOGGER.info(httpResponse.getCode() + ": " + httpResponse.getBody());

            EntityUtils.consume(closeableHttpResponse.getEntity());

            updateProxy(proxy, httpResponse);

            if (httpResponse.getCode() != 200) {
                throw new RuntimeException(httpResponse.getCode() + ": " + httpResponse.getBody());
            }

            System.out.println("trying to save: " + path);
            Path dir = path.getParent();
            //при записи проверять и если есть такой файл, то копировать в дефолт.
            if (Files.isRegularFile(dir)) {
                Files.move(dir, dir.getParent().resolve("defaultTmp.htm"));
                Files.createDirectories(dir);
                Files.move(dir.getParent().resolve("defaultTmp.htm"), dir.resolve("default.htm"));
            }

            Files.createDirectories(dir);
            Files.write(path, httpResponse.getBody().getBytes());
            System.out.println("saved: " + path);

            return httpResponse;
        } catch (Exception e) {

            proxy.setStatus(Proxy.ProxyStatus.UNAVAILABLE);
            proxy.setRetryAfterSec(60);
            LOGGER.error("REST request exception. Proxy host: " + proxy.getHost(), e);
            throw e;
        }
    }

    public void saveFile(String platformId, String fullUri, String referrer) throws Exception {
        URL url = new URL(fullUri);
        try {
            saveFile(platformId, url.getProtocol() + "://" + url.getHost(), url.getPath(), referrer, getProxy());
        } catch (Exception e) {
            throw new RuntimeException("Can't save file " + fullUri, e);
        }
    }

    public void saveFile(FileEntry file) throws Exception {
        doSaveFile(file.getPlatformId(), file.getHost(), file.getUri(), file.getReferrer(), getProxy(), new HttpGet(file.getUri()));
    }

    public void saveFile(String platformId, String host, String uri, String referrer) throws Exception {
        doSaveFile(platformId, host, uri, referrer, getProxy(), new HttpGet(uri));
    }




    public void saveFile(String platformId, String host, String uri, String referrer, Proxy proxy) throws Exception {
        doSaveFile(platformId, host, uri, referrer, proxy, new HttpGet(uri));
    }

    //TODO process exceptions -> retry list
    private void doSaveFile(String platformId, String host, String uri, String referrer, Proxy proxy, HttpUriRequestBase httpUriRequestBase) throws Exception {

        try {
            LOGGER.info(host + uri);

            Path path = getPath(platformId, uri);

            if (Files.exists(path)) { // TODO if read from cache

                //TODO remove this validation, use in the specific validation task only
                if (validate) {

                    if (ImagesValidator.isBrokenImage(path)) {
                        LOGGER.info("BrokenImage: " + path.toAbsolutePath());
                        Files.delete(path);
                        //throw new RuntimeException("Invalid image: " + path);
                    } else {
                        LOGGER.info("Already cached: " + path.toAbsolutePath());
                        return;
                    }
                } else {
                    LOGGER.info("Already cached: " + path.toAbsolutePath());
                    return;
                }
            }

            HttpResponse httpResponse = getHttpResponse(host, referrer, proxy, httpUriRequestBase);

            //validate
            if (ImagesValidator.isBrokenImage(host + uri, httpResponse.getBytes())) {
                httpResponse = getHttpResponse(host, referrer, proxy, httpUriRequestBase);
                if (ImagesValidator.isBrokenImage(host + uri, httpResponse.getBytes())) {
                    YbomCrawler.brokenImages.add(host + uri);
                }
            }

            updateProxy(proxy, httpResponse);

            if (httpResponse.getCode() == 404) {
                YbomCrawler.brokenImages.add(host + uri);
            } else if (httpResponse.getCode() != 200) {
                throw new RuntimeException(httpResponse.getCode() + ": " + host + uri);
            }

            //System.out.println("trying to save: " + path);
            Path dir = path.getParent();
            Files.createDirectories(dir);
            Files.write(path, httpResponse.getBytes());

        } catch (Exception e) {

            //TODO тут неверно. ошибка не всегда из-за прокси, надо разбираться
            e.printStackTrace();
            proxy.setStatus(Proxy.ProxyStatus.UNAVAILABLE);
            proxy.setRetryAfterSec(60);
            LOGGER.error("REST request exception. Proxy host: " + proxy.getHost(), e);
            throw e;
        }
    }

    private HttpResponse getHttpResponse(String host, String referrer, Proxy proxy, HttpUriRequestBase httpUriRequestBase) throws Exception {

        String[] chunks = host.split("://");
        HttpHost targetHost = new HttpHost(chunks[0], chunks[1]);

        referrer = (referrer == null) ? "https://www.google.com/" : referrer;

        httpUriRequestBase.addHeader(new BasicHeader("Host", host.replace("http://", "").replace("https://", "")));
        httpUriRequestBase.addHeader(new BasicHeader("Accept", "image/avif,image/webp,*/*"));
        addBrowserHeaders(httpUriRequestBase, referrer);

        //Prepare the CloseableHttpClient
        CloseableHttpClient closeableHttpClient;

        if (null != proxy.getHost()) {
            HttpHost proxyHost = new HttpHost(proxy.getScheme(), proxy.getHost(), proxy.getPort());
            httpUriRequestBase.setConfig(RequestConfig.custom().setProxy(proxyHost).build());

            CredentialsStore credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(proxy.getHost(), proxy.getPort()),
                    new UsernamePasswordCredentials(proxy.getUserName(), proxy.getPasswordArray()));

            RequestConfig config = RequestConfig.custom()
                    // Determines the timeout until a new connection is fully established. This may also include transport security negotiation exchanges such as SSL or TLS protocol negotiation).
                    .setConnectTimeout(Timeout.ofSeconds(5)).build();

            closeableHttpClient = HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).setDefaultRequestConfig(config).build();
        } else {
            closeableHttpClient = HttpClients.createDefault();
        }

        //Get the result
        CloseableHttpResponse closeableHttpResponse = closeableHttpClient.execute(targetHost, httpUriRequestBase);
        byte[] bytes = EntityUtils.toByteArray(closeableHttpResponse.getEntity());
        HttpResponse response = new HttpResponse(bytes, closeableHttpResponse.getCode(), closeableHttpResponse.getHeaders());
        EntityUtils.consume(closeableHttpResponse.getEntity());

        return response;
    }

    private void addBrowserHeaders(HttpUriRequestBase httpUriRequestBase, String referrer) {

        httpUriRequestBase.addHeader(new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:94.0) Gecko/20100101 Firefox/94.0"));
        httpUriRequestBase.addHeader(new BasicHeader("Accept-Language", "en-US;q=0.8,ru-RU,ru;q=0.5,en;q=0.3"));
        //httpUriRequestBase.addHeader(new BasicHeader("Accept-Encoding", "gzip, deflate, br"));
        httpUriRequestBase.addHeader(new BasicHeader("Referer", referrer));
        httpUriRequestBase.addHeader(new BasicHeader("Connection", "keep-alive"));
        httpUriRequestBase.addHeader(new BasicHeader("Cache-Control", "max-age=0"));
    }

    //TODO remove
    public static Path getPath(Path path, String host, String uri) {

        if (host.startsWith("http://")) {
            host = host.replace("http://", "");
        }
        host = host.replace("://", "@");

        return path.toAbsolutePath().resolve(host + uri).normalize();
    }

    // TODO source specific, move to crawler
    public static Path getPath(String uri) {
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }
        return pagesDir.resolve(uri).normalize().toAbsolutePath();
    }

    // TODO source specific, move to crawler
    public static Path getPath(String platformId, String uri) {
        uri = platformId + uri;
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }
        return cacheDir.resolve(uri).normalize().toAbsolutePath();
    }

    private void updateProxy(Proxy proxy, HttpResponse response) {

        // When a 429 is received, it's your obligation as an API to back off and not spam the API.
        //Repeatedly violating rate limits and/or failing to back off after receiving 429s will result in an automated IP ban (HTTP status 418).
        //IP bans are tracked and scale in duration for repeat offenders, from 2 minutes to 3 days.
        //A Retry-After header is sent with a 418 or 429 responses and will give the number of seconds required to wait,
        // in the case of a 429, to prevent a ban, or, in the case of a 418, until the ban is over.
        if (response.getCode() == 200) {//status = Proxy.ProxyStatus.NORMAL;
            //retryAfterSec = null;
        } else {
            LOGGER.warn("Unexpected response code from proxy: " + response.getCode());
            //TODO proxy.setStatus(status);
            //TODO proxy.setRetryAfterSec(retryAfterSec);
        }
    }

    public static class HttpResponse {

        private String body;
        private byte[] bytes;
        private int code;
        private Header[] headers;

        public HttpResponse(String body, int code, Header[] headers) {
            this.body = body;
            this.bytes = new byte[0];
            this.code = code;
            this.headers = headers;
        }

        public HttpResponse(byte[] bytes, int code, Header[] headers) {
            this.body = "";
            this.bytes = bytes;
            this.code = code;
            this.headers = headers;
        }

        public HttpResponse(CloseableHttpResponse httpResponse) throws IOException, ParseException {
            this(EntityUtils.toString(httpResponse.getEntity()), httpResponse.getCode(), httpResponse.getHeaders());
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public void setBytes(byte[] bytes) {
            this.bytes = bytes;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public Header[] getHeaders() {
            return headers;
        }

        public void setHeaders(Header[] headers) {
            this.headers = headers;
        }
    }
}
