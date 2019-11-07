package md.leonis.shingler.gui.domain;

import lombok.Getter;

@Getter
public class ScriptWord {

    private String word;
    private long frequency;
    private LanguageLevel level;
    private String tag; // for WatchScriptController#webView

    public ScriptWord(String word, LanguageLevel level, String tag) {
        this(word, level, 1, tag);
    }

    public ScriptWord(String word, LanguageLevel level, long frequency, String tag) {
        this.word = word;
        this.frequency = frequency;
        this.level = level;
        this.tag = tag;
    }

    public void increment() {
        ++frequency;
    }
}
