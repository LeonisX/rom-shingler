package md.leonis.shingler.gui.crawler.moby;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsStore;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
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

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

public class HttpExecutor {

    private static final Logger LOGGER = getLogger(HttpExecutor.class);

    private static final List<Proxy> DIRECT_LIST = Collections.singletonList(new Proxy(1, null, null, 0, null, null));

    private static final Random RANDOM = new Random();

    private final List<Proxy> proxies;

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


    // Http requests execution
    //TODO test
    public HttpResponse getResponse(String fullUri) throws Exception {
        URL url = new URL(fullUri);
        return getResponse(url.getProtocol() + "://" + url.getHost(), url.getPath(), getProxy());
    }

    public HttpResponse getResponse(String host, String uri) throws Exception {
        return getResponse(host, uri, getProxy());
    }

    public HttpResponse getResponse(String host, String uri, Proxy proxy) throws Exception {
        return doResponse(host, uri, null, proxy, new HttpGet(uri));
    }

    public HttpResponse getResponse(String host, String uri, String pubicKey) throws Exception {
        return doResponse(host, uri, pubicKey, getProxy(), new HttpGet(uri));
    }

    public HttpResponse postResponse(String host, String uri, String pubicKey) throws Exception {
        return doResponse(host, uri, pubicKey, getProxy(), new HttpPost(uri));
    }

    //TODO save raw html + date, next time read offline
    //TODO process exceptions -> retry list
    private HttpResponse doResponse(String host, String uri, String pubicKey, Proxy proxy, HttpUriRequestBase httpUriRequestBase) throws Exception {

        try {
            LOGGER.info(host + uri);

            String[] chunks = host.split("://");
            HttpHost targetHost = new HttpHost(chunks[0], chunks[1]);

            if (null != pubicKey) {
                httpUriRequestBase.addHeader(new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:94.0) Gecko/20100101 Firefox/94.0"));
                httpUriRequestBase.addHeader(new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"));
                httpUriRequestBase.addHeader(new BasicHeader("Accept-Language", "en-US;q=0.8,ru-RU,ru;q=0.5,en;q=0.3"));
                httpUriRequestBase.addHeader(new BasicHeader("Accept-Encoding", "gzip, deflate, br"));
                httpUriRequestBase.addHeader(new BasicHeader("Referer", "https://www.google.com/")); //TODO
                httpUriRequestBase.addHeader(new BasicHeader("Connection", "keep-alive"));
                httpUriRequestBase.addHeader(new BasicHeader("Cache-Control", "max-age=0"));
            }

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

            proxy.setStatus(Proxy.ProxyStatus.UNAVAILABLE);
            proxy.setRetryAfterSec(60);
            LOGGER.error("REST request exception. Proxy host: " + proxy.getHost(), e);
            throw e;
        }
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
        private int code;
        private Header[] headers;

        public HttpResponse(String body, int code, Header[] headers) {
            this.body = body;
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