package md.leonis.shingler.gui.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import md.leonis.shingler.model.Family;
import md.leonis.shingler.model.FamilyType;
import md.leonis.shingler.model.Name;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public
class NameView {

    public static NameView EMPTY = new NameView(null, "", "", 0.0D, NodeStatus.FAMILY, new ArrayList<>(), null, 0);

    //TODO Need???
    private File file;
    private String name;
    private String familyName;
    private double jakkardStatus;

    private NodeStatus status;
    private List<NameView> items;

    private FamilyType type;

    private int level;

    // Name to NameView
    public NameView(Name name, String familyName, double jakkardStatus, int level) {
        this.file = name.getFile();
        this.name = this.file.getName();
        this.familyName = familyName;
        this.jakkardStatus = jakkardStatus;
        this.status = NodeStatus.MEMBER;
        this.items = new ArrayList<>();
        this.level = level;
    }

    // String (orphaned Name) to NameView
    public NameView(String title, int level) {
        this.file = new File(title);
        this.name = title;
        this.familyName = null;
        this.jakkardStatus = 0;
        this.status = NodeStatus.ORPHAN;
        this.items = new ArrayList<>();
        this.level = level;
    }

    // Family to NameView
    public NameView(Map.Entry<String, Family> entry, List<NameView> views, int level) {
        this.file = null;
        this.name = entry.getKey();
        this.familyName = entry.getKey();
        this.jakkardStatus = entry.getValue().getMembers().size();
        this.status = NodeStatus.FAMILY;
        this.items = views;
        this.type = entry.getValue().getType();
        this.level = level;
    }

    // Family + Jakkard to NameView
    public NameView(Family family, double jakkardStatus, int level) {
        this.file = null;
        this.name = family.getName();
        this.familyName = family.getName();
        this.jakkardStatus = jakkardStatus;
        this.status = NodeStatus.FAMILY_LIST;
        this.items = new ArrayList<>();
        this.type = family.getType();
        this.level = level;
    }

    public Name toName() {
        return new Name(file, false, jakkardStatus);
    }

    @Override
    public String toString() {
        if (status == NodeStatus.FAMILY) {
            String prefix = (type != null && type == FamilyType.GROUP) ? "* " : "";
            return String.format("%-48s   [%1.0f]", prefix + name, jakkardStatus);
        }
        if (status == NodeStatus.FAMILY_LIST) {
            return String.format("%-48s   (%2.3f%%)", name, jakkardStatus);
        }
        if (status == NodeStatus.ORPHAN) {
            if (items.size() > 0) {
                return String.format("%-48s   (%2.3f%%)", name, jakkardStatus);
            } else {
                return String.format("%-48s", name);
            }
        } else {
            return String.format("%-48s     (%2.3f%%)", name, jakkardStatus);
        }
    }
}
