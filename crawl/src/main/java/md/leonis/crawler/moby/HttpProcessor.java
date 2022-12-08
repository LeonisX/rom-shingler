package md.leonis.crawler.moby;

import lombok.Data;
import md.leonis.crawler.moby.dto.FileEntry;
import md.leonis.crawler.moby.executor.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.function.Consumer;

@Data
public class HttpProcessor implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpProcessor.class);

    private final int id;
    private boolean cancelled = false;
    private boolean canStop = false;
    private volatile Exception exception = null;
    private volatile boolean finished = false;

    private final int sleepInterval;
    private final int sleepMultiplier;
    private int sleep;

    private String file;

    Queue<FileEntry> httpQueue;
    Executor executor;
    private Consumer<FileEntry> refreshConsumer;
    private Consumer<FileEntry> errorConsumer;
    private Consumer<FileEntry> successConsumer;

    private Consumer<FileEntry> fileEntryConsumer; // to save/validate file

    public HttpProcessor(int id, Queue<FileEntry> httpQueue, Executor executor, Consumer<FileEntry> fileEntryConsumer) {
        this(id, httpQueue, executor, 15, 2, fileEntryConsumer);
    }

    public HttpProcessor(int id, Queue<FileEntry> httpQueue, Executor executor, int sleepInterval, int sleepMultiplier, Consumer<FileEntry> fileEntryConsumer) {
        this.id = id;
        this.httpQueue = httpQueue;
        this.executor = executor;
        this.sleepInterval = sleepInterval;
        this.sleepMultiplier = sleepMultiplier;
        this.sleep = sleepInterval;
        this.fileEntryConsumer = fileEntryConsumer;
    }

    @Override
    public void run() {
        while (!cancelled) {
            try {
                if (httpQueue.isEmpty()) {
                    if (canStop) {
                        stop();
                    }
                    sleep(50);
                } else {
                    FileEntry fileEntry = httpQueue.poll();
                    if (fileEntry == null) {
                        file = "";
                        refreshConsumer.accept(null);
                        System.out.println("DEBUG (not error) HttpQueue is empty."); //TODO use another queue
                        sleep(51);
                    } else {
                        file = fileEntry.getHost() + fileEntry.getUri();
                        refreshConsumer.accept(fileEntry);
                        try {
                            fileEntryConsumer.accept(fileEntry);
                            fileEntry.setCompleted(true);
                            successConsumer.accept(fileEntry);
                            sleep = sleepInterval;
                        } catch (Exception e) {
                            if (fileEntry.getErrorsCount() < 5) {
                                fileEntry.setErrorsCount(fileEntry.getErrorsCount() + 1);
                                fileEntry.getExceptions().add(e);
                                file = String.format("Sleep %s second(s) after %s (%s: %s)", sleep, file, e.getClass().getSimpleName(), e.getMessage());
                                httpQueue.add(fileEntry);
                                errorConsumer.accept(fileEntry);
                                if (!cancelled) {
                                    sleepSeconds(sleep);
                                }
                                sleep = sleep * sleepMultiplier;
                            }
                        }
                    }
                }

            } catch (Exception e) {
                exception = e;
                cancelled = true;
            }
        }
        finished = true;
        file = "";
        refreshConsumer.accept(null);
    }

    public void stop() {
        cancelled = true;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void reset() {
        cancelled = false;
        canStop = false;
        exception = null;
        finished = false;
    }

    private void sleepSeconds(int sec) {
        try {
            Thread.sleep(sec * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setConsumers(Consumer<FileEntry> refreshConsumer, Consumer<FileEntry> successConsumer, Consumer<FileEntry> errorConsumer) {
        this.refreshConsumer = refreshConsumer;
        this.successConsumer = successConsumer;
        this.errorConsumer = errorConsumer;
    }
}
