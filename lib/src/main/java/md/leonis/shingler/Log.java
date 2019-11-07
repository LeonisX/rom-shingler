package md.leonis.shingler;

import java.util.Collection;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class Log {

    private final BlockingDeque<LogRecord> log;

    public Log() {
        log = new LinkedBlockingDeque<>(1);
    }

    public Log(int maxLogEntries) {
        log = new LinkedBlockingDeque<>(maxLogEntries);
    }

    public void drainTo(Collection<? super LogRecord> collection) {
        log.drainTo(collection);
    }

    void offer(LogRecord record) {
        log.offer(record);
    }
}
