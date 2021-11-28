package md.leonis.crawler.moby.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserReview {

    private String rate;
    private String userId;
    private String summary;

    public UserReview(String rate, String userId, String summary) {
        this.rate = rate;
        this.userId = userId;
        this.summary = summary;
    }
}