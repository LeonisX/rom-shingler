package md.leonis.crawler.moby.executor;

import md.leonis.crawler.moby.Proxy;
import md.leonis.crawler.moby.dto.FileEntry;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsStore;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class HttpExecutor implements Executor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpExecutor.class);

    private static final List<Proxy> DIRECT_LIST = Collections.singletonList(new Proxy(1, null, null, 0, null, null));

    private static final Random RANDOM = new Random();

    private final List<Proxy> proxies;

    private volatile int index;

    public HttpExecutor() {

        this.proxies = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            proxies.add(DIRECT_LIST.get(0));
        }
        this.index = proxies.isEmpty() ? 0 : RANDOM.nextInt(proxies.size());
    }

    public HttpExecutor(List<Proxy> proxies) {

        this.proxies = proxies;
        this.index = proxies.isEmpty() ? 0 : RANDOM.nextInt(proxies.size());
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

    public Executor.HttpResponse getPage(String fullUri, String referrer) throws Exception {
        //System.out.println("getPage: " + fullUri + " <-- " + referrer);
        URL url = new URL(fullUri);
        return getPage(url.getProtocol() + "://" + url.getHost(), url.getPath(), referrer, getProxy());
    }

    public Executor.HttpResponse getPage(String host, String uri, String referrer, Proxy proxy) throws Exception {
        return doGetPage(host, uri, referrer, proxy, new HttpGet(uri));
    }

    private HttpResponse doGetPage(String host, String uri, String referrer, Proxy proxy, HttpUriRequestBase httpUriRequestBase) throws Exception {

        try {
            //LOGGER.info((host + uri + " <- " + referrer));

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

            return httpResponse;
        } catch (Exception e) {

            //proxy.setStatus(Proxy.ProxyStatus.UNAVAILABLE); //TODO
            //proxy.setRetryAfterSec(60);
            //LOGGER.error("REST request exception. Proxy host: " + proxy.getHost(), e);
            throw e;
        }
    }

    public HttpResponse getFile(String fullUri, String referrer) throws Exception {
        URL url = new URL(fullUri);
        try {
            return getFile(url.getProtocol() + "://" + url.getHost(), url.getPath(), referrer, getProxy());
        } catch (Exception e) {
            throw new RuntimeException("Can't save file " + fullUri, e);
        }
    }

    public HttpResponse getFile(FileEntry file) throws Exception {
        return doGetFile(file.getHost(), file.getUri(), file.getReferrer(), getProxy(), new HttpGet(file.getUri()));
    }

    public HttpResponse getFile(String host, String uri, String referrer) throws Exception {
        return doGetFile(host, uri, referrer, getProxy(), new HttpGet(uri));
    }

    public HttpResponse getFile(String host, String uri, String referrer, Proxy proxy) throws Exception {
        return doGetFile(host, uri, referrer, proxy, new HttpGet(uri));
    }

    private HttpResponse doGetFile(String host, String uri, String referrer, Proxy proxy, HttpUriRequestBase httpUriRequestBase) throws Exception {

        try {
            //LOGGER.info(host + uri);

            HttpResponse httpResponse = getHttpResponse(host, referrer, proxy, httpUriRequestBase);

            //validate
            //validator disabled for not
            /*if (ImagesValidator.isBrokenImage(host + uri, httpResponse.getBytes())) {
                httpResponse = getHttpResponse(host, referrer, proxy, httpUriRequestBase);
                if (ImagesValidator.isBrokenImage(host + uri, httpResponse.getBytes())) {
                    YbomCrawler.brokenImages.add(host + uri);
                }
            }*/

            updateProxy(proxy, httpResponse);

            return httpResponse;

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

    public HttpResponse doHead(String uri, String referrer, Proxy proxy) throws Exception {

        try {
            //LOGGER.info(uri);
            HttpResponse httpResponse = getHttpResponse2(uri, referrer, proxy, new HttpHead(uri));

            updateProxy(proxy, httpResponse);
            return httpResponse;

        } catch (Exception e) {

            //TODO тут неверно. ошибка не всегда из-за прокси, надо разбираться
            e.printStackTrace();
            /*proxy.setStatus(Proxy.ProxyStatus.UNAVAILABLE); //TODO
            proxy.setRetryAfterSec(60);*/
            //LOGGER.error("REST request exception. Proxy host: " + proxy.getHost(), e);
            throw e;
        }
    }

    private HttpResponse getHttpResponse2(String host, String referrer, Proxy proxy, HttpUriRequestBase httpUriRequestBase) throws Exception {

        HttpHost targetHost = HttpHost.create(new URI(host).getHost()); // TODO optimize

        referrer = (referrer == null) ? "https://www.google.com/" : referrer;

        // httpUriRequestBase.addHeader(new BasicHeader("Host", host.replace("http://", "").replace("https://", "")));
        // httpUriRequestBase.addHeader(new BasicHeader("Accept", "image/avif,image/webp,*/*"));
        // addBrowserHeaders(httpUriRequestBase, referrer);

        //Prepare the CloseableHttpClient
        CloseableHttpClient closeableHttpClient;

        if (null != proxy && null != proxy.getHost()) {
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
        byte[] bytes = new byte[0];
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

    private void updateProxy(Proxy proxy, HttpResponse response) {

        // When a 429 is received, it's your obligation as an API to back off and not spam the API.
        //Repeatedly violating rate limits and/or failing to back off after receiving 429s will result in an automated IP ban (HTTP status 418).
        //IP bans are tracked and scale in duration for repeat offenders, from 2 minutes to 3 days.
        //A Retry-After header is sent with a 418 or 429 responses and will give the number of seconds required to wait,
        // in the case of a 429, to prevent a ban, or, in the case of a 418, until the ban is over.
        if (response.getCode() == 200) {//status = Proxy.ProxyStatus.NORMAL;
            //retryAfterSec = null;
        } else {
            //LOGGER.warn("Unexpected response code from proxy: " + response.getCode());
            //TODO proxy.setStatus(status);
            //TODO proxy.setRetryAfterSec(retryAfterSec);
        }
    }
}
