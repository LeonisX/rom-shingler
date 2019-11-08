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
        String[] chunks = event.getRenderedMessage().split("\\|");
        if (chunks.length == 2) {
            InternalLogger.log.offer(new LogRecord(Level.valueOf(event.getLevel().toString()), event.getLoggerName(), chunks[0], Double.valueOf(chunks[1])));
        } else {
            InternalLogger.log.offer(new LogRecord(Level.valueOf(event.getLevel().toString()), event.getLoggerName(), event.getRenderedMessage(), null));
        }
    }

    @Override
    public void close() {
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }
}
