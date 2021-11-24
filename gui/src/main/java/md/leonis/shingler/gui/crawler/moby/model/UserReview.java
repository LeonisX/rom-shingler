package md.leonis.shingler.gui.crawler.moby.model;

public class UserReview {

    private String rate;
    private String userId;
    private String summary;

    public UserReview(String rate, String userId, String summary) {
        this.rate = rate;
        this.userId = userId;
        this.summary = summary;
    }

    public String getRate() {
        return rate;
    }

    public void setRate(String rate) {
        this.rate = rate;
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