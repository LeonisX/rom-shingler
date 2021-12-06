package md.leonis.crawler.moby;

import lombok.Data;
import md.leonis.crawler.moby.dto.FileEntry;
import md.leonis.crawler.moby.executor.Executor;
import md.leonis.crawler.moby.executor.HttpExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Data
public class FilesProcessor {

    private final ExecutorService service;
    private final List<HttpProcessor> processors;
    private Queue<FileEntry> httpQueue = new ConcurrentLinkedQueue<>();
    private final Executor executor;

    private AtomicInteger index = new AtomicInteger(0);

    private Consumer<FileEntry> addFileConsumer = (s) -> {
    };

    public FilesProcessor(int processorsCount) {

        this.service = Executors.newCachedThreadPool();

        this.processors = new ArrayList<>();
        for (int i = 1; i <= processorsCount; i++) {
            processors.add(new HttpProcessor(i, httpQueue, new HttpExecutor()));
        }
        processors.forEach(service::execute);
        this.executor = processors.get(0).getExecutor();
    }

    public FilesProcessor(List<HttpProcessor> processors) {

        this.service = Executors.newCachedThreadPool();
        this.processors = processors;
        this.executor = processors.get(0).getExecutor();
        this.httpQueue = processors.get(0).getHttpQueue();
        processors.forEach(service::execute);
    }

    public void stopProcessors() {

        processors.forEach(p -> {
            p.setCancelled(true);
            p.setCanStop(true);
        });

        while (processors.stream().filter(HttpProcessor::isFinished).count() < processors.size()) {
            sleep(200);
        }
        service.shutdown();
    }

    public void finalizeProcessors() throws Exception {

        processors.forEach(p -> p.setCanStop(true));

        while (processors.stream().filter(HttpProcessor::isFinished).count() < processors.size()) {
            stopAllProcessorsIfError();
            sleep(201);
        }
        service.shutdown();
    }

    public void stopAllProcessorsIfError() throws InterruptedException {

        Exception exception = processors.stream().filter(p -> p.getException() != null).findFirst().map(HttpProcessor::getException).orElse(null);
        if (exception != null) {
            processors.forEach(p -> p.setCancelled(true));
            service.awaitTermination(2, TimeUnit.SECONDS);
            service.shutdown();
            throw new RuntimeException(exception);
        }
    }

    public void add(FileEntry fileEntry) {
        addFileConsumer.accept(fileEntry);
        httpQueue.add(fileEntry);
    }

    public void setAddFileConsumer(Consumer<FileEntry> consumer) {
        this.addFileConsumer = consumer;
    }

    public void resetProcessors() {
        processors.forEach(HttpProcessor::reset);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
