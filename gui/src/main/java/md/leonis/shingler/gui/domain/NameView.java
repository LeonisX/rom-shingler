package md.leonis.shingler.gui.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import md.leonis.shingler.model.Family;
import md.leonis.shingler.model.Name;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public
class NameView {

    public static NameView EMPTY = new NameView((File) null, "", "", 0.0D, true, new ArrayList<NameView>());

    //TODO Need???
    private File file;
    private String name;
    private String familyName;
    private double jakkardStatus;

    private boolean isFamily;
    private List<NameView> items;

    // Name to NameView
    public NameView(Name name, String familyName, double jakkardStatus) {
        this.file = name.getFile();
        this.name = this.file.getName();
        this.familyName = familyName;
        this.jakkardStatus = jakkardStatus;
        this.isFamily = false;
        this.items = new ArrayList<>();
    }

    // String (orphaned Name) to NameView
    public NameView(String title) {
        this.file = new File(title);
        this.name = title;
        this.familyName = null;
        this.jakkardStatus = 0;
        this.isFamily = false;
        this.items = new ArrayList<>();
    }

    // Family to NameView
    public NameView(Map.Entry<String, Family> entry, List<NameView> views) {
        this.file = null;
        this.name = entry.getKey();
        this.familyName = entry.getKey();
        this.jakkardStatus = entry.getValue().getMembers().size();
        this.isFamily = true;
        this.items = views;
    }

    public Name toName() {
        return new Name(file, false, jakkardStatus);
    }

    @Override
    public String toString() {
        if (isFamily) {
            return String.format("%-48s   [%1.0f]", name, jakkardStatus);
        } else {
            return String.format("%-48s     (%2.3f%%)", name, jakkardStatus);
        }
    }
}
