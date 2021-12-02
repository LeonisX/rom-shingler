package md.leonis.crawler.moby.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileEntry {

    private String platformId;
    private String host;
    private String uri;
    private String referrer;
}
