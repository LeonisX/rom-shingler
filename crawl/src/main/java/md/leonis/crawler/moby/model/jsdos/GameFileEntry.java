package md.leonis.crawler.moby.model.jsdos;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

// JS-DOS now only
@Data
public class GameFileEntry {

    private String lang;    // ENG
    private String title;   // ''
    private String url;     // /custom/dos/4k-adventure.jsdos   -   need to prepend /

    private String host;

    //private String mobile;  // false
    //private String type;    // js
    //private String hardware;// true
    private Map<String, String> other = new HashMap<>();
}
