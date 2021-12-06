package md.leonis.crawler.moby.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import md.leonis.crawler.moby.dto.FileEntry;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Activity {

    private List<String> platforms;
    private Task task;
    private List<FileEntry> fileEntries;

    public Activity(List<String> platforms, Task task) {
        this.platforms = platforms;
        this.task = task;
        this.fileEntries = new ArrayList<>();
    }

    public enum Task {
        LOAD, RELOAD, VALIDATE
    }
}
