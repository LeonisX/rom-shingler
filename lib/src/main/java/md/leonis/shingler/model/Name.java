package md.leonis.shingler.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import md.leonis.shingler.ListFilesa;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@AllArgsConstructor
public class Name implements Serializable {

    private static final long serialVersionUID = 292207904602980582L;

    private String id = UUID.randomUUID().toString();

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
        this.index = ListFilesa.calculateIndex(name);
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonIgnore
    public String getCleanName() {
        return ListFilesa.getCleanName(name);
    }

    @JsonIgnore
    public String getPdCleanName() {
        return ListFilesa.getPdCleanName(name);
    }

    @JsonIgnore
    public String getHackCleanName() {
        return ListFilesa.getHackCleanName(name);
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
        return Objects.equals(id, name.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
