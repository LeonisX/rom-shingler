package md.leonis.crawler.moby.executor;

import md.leonis.crawler.moby.Proxy;
import md.leonis.crawler.moby.dto.FileEntry;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.util.Random;

public interface Executor {

    Random RANDOM = new Random();

    HttpResponse getPage(String fullUri, String referrer) throws Exception;

    HttpResponse getPage(String host, String uri, String referrer, Proxy proxy) throws Exception;

    HttpResponse getFile(FileEntry file) throws Exception;

    HttpResponse getFile(String host, String uri, String referrer) throws Exception;

    HttpResponse getFile(String host, String uri, String referrer, Proxy proxy) throws Exception;

    class HttpResponse {

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
