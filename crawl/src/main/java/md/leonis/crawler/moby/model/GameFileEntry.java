package md.leonis.crawler.moby.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class GameFileEntry {

    private String lang;    // ENG
    private String title;   // ''
    private String url;     // custom/dos/4k-adventure.jsdos

    //private String mobile;  // false
    //private String type;    // js
    //private String hardware;// true
    private Map<String, String> other = new HashMap<>();
}
