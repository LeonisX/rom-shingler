package md.leonis.crawler.moby.executor;

import md.leonis.crawler.moby.Proxy;
import md.leonis.crawler.moby.dto.FileEntry;
import org.apache.hc.core5.http.Header;

public class TestExecutor implements Executor {

    private final long sleep = 3500;

    @Override
    public HttpResponse getPage(String fullUri, String referrer) {
        sleep(sleep);
        return new HttpResponse("body", 200, new Header[0]);
    }

    @Override
    public HttpResponse getPage(String host, String uri, String referrer, Proxy proxy) {
        sleep(sleep + 1);
        return new HttpResponse("body", 200, new Header[0]);
    }

    @Override
    public HttpResponse getFile(FileEntry file) {
        sleep(sleep);
        boolean err = RANDOM.nextBoolean();
        boolean err2 = RANDOM.nextBoolean();
        if (err || err2) {
            throw new RuntimeException(file.getUri());
        }
        return null;
    }

    @Override
    public HttpResponse getFile(String host, String uri, String referrer) {
        sleep(sleep);
        return null;
    }

    @Override
    public HttpResponse getFile(String host, String uri, String referrer, Proxy proxy) {
        sleep(sleep);
        return null;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
