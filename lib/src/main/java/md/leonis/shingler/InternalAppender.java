package md.leonis.shingler;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

//TODO percentage
public class InternalAppender extends AppenderSkeleton {

    //TODO different settings from log4j.properties

    public InternalAppender() {
        super();

        InternalLogger.log = new Log(1_000_000); //TODO MAX_LOG_ENTRIES
    }

    @Override
    protected void append(LoggingEvent event) {
        InternalLogger.log.offer(new LogRecord(Level.valueOf(event.getLevel().toString()), event.getLoggerName(), event.getRenderedMessage()));
    }

    @Override
    public void close() {
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }
}
