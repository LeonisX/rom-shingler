package md.leonis.shingler.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@AllArgsConstructor
public class Name implements Serializable {

    private static final long serialVersionUID = 292207904602980582L;

    private String name;
    private boolean done;
    private int index = 100;

    private double jakkardStatus = 0;

    public Name() {
        // For Jackson
    }

    public Name(String name, boolean done, double jakkardStatus) {
        this(name, done);
        this.jakkardStatus = jakkardStatus;
    }

    public Name(String name, boolean done) {
        this.name = name;
        this.done = done;

        if (name.contains("(U)")) {
            index += 100;
        }
        if (name.contains("(W)")) {
            index += 99;
        }
        if (name.contains("(E)")) {
            index += 80;
        }
        if (name.contains("(F)")) {
            index += 70;
        }
        if (name.contains("(G)")) {
            index += 70;
        }
        if (name.contains("(J)")) {
            index += 60;
        }
        if (name.contains("[b")) {
            index -= 50;
        }
        if (name.contains("[a")) {
            index -= 5;
        }
        if (name.contains("[h")) {
            index -= 20;
        }
        if (name.contains("[t")) {
            index -= 10;
        }
        if (name.contains("[p")) {
            index -= 10;
        }
        if (name.contains("[f")) {
            index -= 10;
        }
        if (name.contains("[T")) {
            index -= 10;
        }
        if (name.contains("[!]")) {
            index += 10;
        }
        if (name.contains("(PD)")) {
            index -= 45;
        }
        if (name.contains("(Hack") || name.contains("Hack)")) {
            index -= 45;
        }
        if (name.contains("+")) {
            index -= 2;
        }
    }

    @Override
    public String toString() {
        return name + ": " + jakkardStatus;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public String getCleanName() {
        String result = name;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Name name = (Name) o;
        return done == name.done &&
                index == name.index &&
                Double.compare(name.jakkardStatus, jakkardStatus) == 0 &&
                Objects.equals(name.name, this.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, done, index, jakkardStatus);
    }
}
