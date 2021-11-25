package md.leonis.shingler.gui.crawler.moby.model;

public class Credits {

    private String id;
    private String name;
    private String origName;
    private String note;
    private String yup;

    // 	Tetsuji Tanaka (Tanaka [タナカ]/Yuk)
    public Credits(String id, String name, String origName, String group, String yup) {
        this.id = id;
        this.name = name;
        this.origName = origName;
        this.note = group;
        this.yup = yup;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrigName() {
        return origName;
    }

    public void setOrigName(String origName) {
        this.origName = origName;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getYup() {
        return yup;
    }

    public void setYup(String yup) {
        this.yup = yup;
    }

    @Override
    public String toString() {
        String result = "{" + id + "=" + name;
        if (origName != null) {
            result +=", origName=" + origName;
        }
        if (note != null) {
            result += ", group=" + note;
        }
        if (yup != null) {
            result += ", yup=" + yup;
        }
        return result + '}';
    }
}
