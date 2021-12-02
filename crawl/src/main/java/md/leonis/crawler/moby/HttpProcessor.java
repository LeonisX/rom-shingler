package md.leonis.crawler.moby;

import lombok.Data;
import md.leonis.crawler.moby.dto.FileEntry;

import java.util.Queue;

@Data
public class HttpProcessor implements Runnable {

    private boolean cancelled = false;
    private boolean canStop = false;
    private boolean inWork = false;
    private volatile Exception exception = null;
    private volatile boolean finished = false;

    Queue<FileEntry> httpQueue;
    HttpExecutor executor;

    public HttpProcessor(Queue<FileEntry> httpQueue, HttpExecutor executor) {
        this.httpQueue = httpQueue;
        this.executor = executor;
    }

    @Override
    public void run() {
        while (!cancelled) {
            try {
                if (httpQueue.isEmpty()) {
                    if (canStop && !inWork) {
                        stop();
                    }
                    sleep(50);
                } else {
                    // TODO process
                    inWork = true;
                    FileEntry file = httpQueue.poll();
                    if (file == null) {
                        System.out.println("===============File is null");
                        sleep(50);
                    } else {
                        try {
                            executor.saveFile(file);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    inWork = false;
                }

            } catch (Exception e) {
                exception = e;
                cancelled = true;
            }
        }
        finished = true;
    }

    public void stop() {
        cancelled = true;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
