package md.leonis.shingler.gui.crawler.moby;

public class UserReview {

    private String note;
    private String userId;
    private String summary;

    public UserReview(String note, String userId, String summary) {
        this.note = note;
        this.userId = userId;
        this.summary = summary;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}