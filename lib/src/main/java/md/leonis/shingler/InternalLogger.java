package md.leonis.shingler;

import static md.leonis.shingler.Level.*;

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
        log(new LogRecord(TRACE, context, msg));
    }

    void debug(String msg) {
        log(new LogRecord(DEBUG, context, msg));
    }

    public void info(String msg) {
        log(new LogRecord(INFO, context, msg));
    }

    public void warn(String msg) {
        log(new LogRecord(WARN, context, msg));
    }

    void error(String msg) {
        log(new LogRecord(ERROR, context, msg));
    }

    public Log getLog() {
        return log;
    }
}
