package md.leonis.shingler.model;

import java.io.File;
import java.io.Serializable;

public class Name implements Serializable {

    private static final long serialVersionUID = 292207904602980582L;

    private File file;
    private boolean done;
    private int index = 100;

    private double jakkardStatus = 0;

    public Name(File file, boolean done) {
        this.file = file;
        this.done = done;

        if (file.getName().contains("(U)")) {
            index += 100;
        }
        if (file.getName().contains("(W)")) {
            index += 99;
        }
        if (file.getName().contains("(E)")) {
            index += 80;
        }
        if (file.getName().contains("(F)")) {
            index += 70;
        }
        if (file.getName().contains("(G)")) {
            index += 70;
        }
        if (file.getName().contains("(J)")) {
            index += 60;
        }
        if (file.getName().contains("[b")) {
            index -= 50;
        }
        if (file.getName().contains("[a")) {
            index -= 5;
        }
        if (file.getName().contains("[h")) {
            index -= 20;
        }
        if (file.getName().contains("[t")) {
            index -= 10;
        }
        if (file.getName().contains("[p")) {
            index -= 10;
        }
        if (file.getName().contains("[f")) {
            index -= 10;
        }
        if (file.getName().contains("[T")) {
            index -= 10;
        }
        if (file.getName().contains("[!]")) {
            index += 10;
        }
        if (file.getName().contains("(PD)")) {
            index -= 45;
        }
        if (file.getName().contains("(Hack") || file.getName().contains("Hack)")) {
            index -= 45;
        }
        if (file.getName().contains("+")) {
            index -= 2;
        }
    }

    @Override
    public String toString() {
        return file.getName() + ": " + jakkardStatus;
    }

    public int getIndex() {
        return index;
    }

    void setIndex(int index) {
        this.index = index;
    }

    File getFile() {
        return file;
    }

    void setFile(File file) {
        this.file = file;
    }

    public String getName() {
        return file.getName();
    }

    public String getCleanName() {
        String result = file.getName();
        int braceIndex = result.indexOf("(");
        if (braceIndex > 0) {
            result = result.substring(0, braceIndex);
        }
        braceIndex = result.indexOf("[");
        if (braceIndex > 0) {
            result = result.substring(0, braceIndex);
        }
        return result.trim();
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public double getJakkardStatus() {
        return jakkardStatus;
    }

    public void setJakkardStatus(double jakkardStatus) {
        this.jakkardStatus = jakkardStatus;
    }

    public void addJakkardStatus(double status) {
        jakkardStatus += status;
    }
}
