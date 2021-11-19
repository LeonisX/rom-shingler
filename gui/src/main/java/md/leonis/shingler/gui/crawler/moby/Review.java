package md.leonis.shingler.gui.crawler.moby;

public class Review {

    private int score;
    private String sourceId;
    private String date;
    private String review;
    private String sourceUrl;

    public Review(int score, String sourceId, String date, String review) {
        this.score = score;
        this.sourceId = sourceId;
        this.date = date;
        this.review = review;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
