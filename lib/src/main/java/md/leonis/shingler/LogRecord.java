package md.leonis.shingler;

import java.util.Date;

public class LogRecord {

    private Date timestamp;
    private Level level;
    private String context;
    private String message;
    private Double progress;

    public LogRecord(Level level, String context, String message, Double progress) {
        this.timestamp = new Date();
        this.level = level;
        this.context = context;
        this.message = message;
        this.progress = progress;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public Level getLevel() {
        return level;
    }

    public String getContext() {
        return context;
    }

    public String getMessage() {
        return message;
    }

    public Double getProgress() {
        return progress;
    }
}
