package md.leonis.crawler.moby.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileEntry {

    private String platformId;
    private String host;
    private String uri;
    private String referrer;
    private boolean completed;
    private int errorsCount = 0;
    private List<Throwable> exceptions = new ArrayList<>();

    public FileEntry(String platformId, String host, String uri, String referrer) {
        this.platformId = platformId;
        this.host = host;
        this.uri = uri;
        this.referrer = referrer;
    }
}
