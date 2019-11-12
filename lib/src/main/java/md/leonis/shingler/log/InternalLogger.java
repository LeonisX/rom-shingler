package md.leonis.shingler.log;

import static md.leonis.shingler.log.Level.*;

public class InternalLogger {

    public static Log log;
    private final String context;

    public InternalLogger(String context) {
        this.context = context;
        log = new Log();
    }

    public InternalLogger(Class<?> clazz) {
        this.context = clazz.getName();
        log = new Log();
    }

    public void log(LogRecord record) {
        if (null != log) {
            log.offer(record);
        }
    }

    void trace(String msg) {
        log(new LogRecord(TRACE, context, msg, null));
    }

    void debug(String msg) {
        log(new LogRecord(DEBUG, context, msg, null));
    }

    public void info(String msg) {
        log(new LogRecord(INFO, context, msg, null));
    }

    public void warn(String msg) {
        log(new LogRecord(WARN, context, msg, null));
    }

    void error(String msg) {
        log(new LogRecord(ERROR, context, msg, null));
    }

    public Log getLog() {
        return log;
    }
}
