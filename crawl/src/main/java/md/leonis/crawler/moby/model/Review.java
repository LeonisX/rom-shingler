package md.leonis.crawler.moby.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Review {

    private Integer score;
    private String sourceId;
    private String date;
    private String review;
    private String sourceUrl;

    public Review(Integer score, String sourceId, String date, String review) {
        this.score = score;
        this.sourceId = sourceId;
        this.date = date;
        this.review = review;
    }
}
